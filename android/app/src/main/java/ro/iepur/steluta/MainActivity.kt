package ro.iepur.steluta

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Ecranul principal.
 *
 * Treaba de zi cu zi o face widget-ul; ecranul asta e pentru cand vrei sa te uiti mai
 * atent - ceas, prognoza pe zile, si comentariul intreg, nu taiat la doua randuri.
 *
 * `Activity` simplu, nu `AppCompatActivity`: nu tragem o biblioteca de compatibilitate
 * pentru un ecran fara bara de titlu si fara meniuri, pe o aplicatie care cere oricum
 * Android 8.
 */
private const val REQ_LOCATION = 1

class MainActivity : Activity() {

    companion object {
        /**
         * Cu actiunea asta o deschide butonul ↻ din widget cand aplicatia n-are inca voie
         * la locatie: ecranul cere permisiunea si cauta locul, ca si cum ai fi apasat
         * „Locația mea".
         */
        const val ACTION_LOCATE = "ro.iepur.steluta.LOCATE"
    }

    /**
     * Limba telefonului, citita la fiecare folosire. Daca omul schimba limba, Android
     * recreeaza ecranul, iar textele scrise din cod trebuie sa vina si ele in cea noua.
     */
    private val locale: Locale get() = Locale.getDefault()

    private val main = Handler(Looper.getMainLooper())

    /**
     * Ceasul.
     *
     * Se opreste cand ecranul nu se vede (vezi [onStop]) - un `Handler` care bate din
     * secunda in secunda pe o activitate ascunsa e exact felul de lucru care goleste
     * bateria fara ca nimeni sa afle de ce.
     */
    private val tick = object : Runnable {
        override fun run() {
            showClock()
            main.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Butonul de aici schimba si comentariul, ca ↻ din widget. Reimprospatarea de la
        // deschidere, nu: altfel fiecare deschidere ar sari la alt text.
        findViewById<Button>(R.id.m_refresh).setOnClickListener { load(shuffle = true) }
        findViewById<Button>(R.id.m_locate).setOnClickListener { locate() }
        findViewById<Button>(R.id.m_theme).setOnClickListener { pickTheme() }
        showThemeName()

        // Ce stiam deja, imediat - la fel ca widget-ul. Reteaua vine peste el.
        WeatherStore.load(this)?.let { show(it, fromCache = true) }
        load()
        handle(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_LOCATE) locate()
    }

    override fun onStart() {
        super.onStart()
        // Temele „wallpaper", „semi-transparent" si „transparent" se potrivesc dupa
        // wallpaper-ul de acum, pe care widget-ul nu-l poate urmari singur. Deschiderea
        // aplicatiei e un moment bun sa se uite din nou.
        StelutaWidget.redrawAll(this)
        showClock()
        main.postDelayed(tick, 1000)
    }

    override fun onStop() {
        super.onStop()
        main.removeCallbacks(tick)
    }

    private fun showClock() {
        val now = Date()
        findViewById<TextView>(R.id.m_clock).text = SimpleDateFormat("HH:mm", locale).format(now)
        findViewById<TextView>(R.id.m_seconds).text = SimpleDateFormat("ss", locale).format(now)
        findViewById<TextView>(R.id.m_date).text =
            SimpleDateFormat("EEEE, d MMMM", locale).format(now)
                .replaceFirstChar { it.uppercase(locale) }
    }

    private fun load(shuffle: Boolean = false) {
        val btn = findViewById<Button>(R.id.m_refresh)
        btn.isEnabled = false

        Thread {
            val fresh = WeatherApi.fetch(PlaceStore.load(this))
            if (fresh != null) WeatherStore.save(this, fresh)
            if (shuffle) {
                (fresh ?: WeatherStore.load(this))?.let { w ->
                    Quips.shuffle(this, w)
                    // Widget-ul citeste acelasi comentariu; fara asta ar ramane pe cel vechi
                    // pana la urmatoarea lui reimprospatare.
                    StelutaWidget.redrawAll(this)
                    if (fresh == null) main.post { show(w, fromCache = true) }
                }
            }
            main.post {
                btn.isEnabled = true
                if (fresh != null) {
                    show(fresh, fromCache = false)
                } else if (WeatherStore.load(this) == null) {
                    findViewById<TextView>(R.id.m_desc).text = getString(R.string.no_data)
                }
            }
        }.start()
    }

    /**
     * Afla unde suntem si mutam vremea acolo.
     *
     * Permisiunea se cere abia la apasarea butonului, nu la prima pornire: un ecran de
     * permisiune care apare inainte sa fi vazut omul ce face aplicatia se refuza din
     * reflex.
     */
    private fun locate() {
        if (!Locator.hasPermission(this)) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                REQ_LOCATION,
            )
            return
        }

        val btn = findViewById<Button>(R.id.m_locate)
        btn.isEnabled = false
        btn.text = getString(R.string.locating)

        Thread {
            val spot = Locator.find(this)
            main.post {
                btn.isEnabled = true
                btn.text = getString(R.string.use_my_location)

                if (spot == null) {
                    Toast.makeText(this, R.string.location_failed, Toast.LENGTH_LONG).show()
                    return@post
                }

                PlaceStore.save(this, spot)
                Toast.makeText(
                    this,
                    getString(R.string.location_found, spot.name),
                    Toast.LENGTH_SHORT,
                ).show()

                load()
                // Widget-ul foloseste acelasi loc salvat, deci trebuie sa afle acum -
                // altfel ar arata orasul vechi pana la urmatoarea lui reimprospatare.
                nudgeWidget()
            }
        }.start()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_LOCATION) return

        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            // Butonul ↻ din widget se leaga altfel cu permisiunea data (vezi
            // StelutaWidget.refreshIntent), deci widget-ul trebuie redesenat acum -
            // chiar daca locatia de mai jos nu se gaseste.
            StelutaWidget.redrawAll(this)
            locate()
        } else {
            Toast.makeText(this, R.string.location_denied, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Lista de teme. Alegerea se vede imediat pe ecranul de start: widget-ul se redeseneaza
     * din ce e salvat, fara retea.
     */
    private fun pickTheme() {
        val names = resources.getStringArray(R.array.theme_names)
        val current = WidgetTheme.load(this).ordinal
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(R.string.widget_theme_title)
            .setSingleChoiceItems(names, current) { dialog, which ->
                ThemeId.entries.getOrNull(which)?.let { WidgetTheme.save(this, it) }
                showThemeName()
                StelutaWidget.redrawAll(this)
                dialog.dismiss()
            }
            .show()
    }

    private fun showThemeName() {
        val names = resources.getStringArray(R.array.theme_names)
        val name = names.getOrElse(WidgetTheme.load(this).ordinal) { "" }
        findViewById<Button>(R.id.m_theme).text = getString(R.string.widget_theme, name)
    }

    /** Ii spune widget-ului sa se redeseneze acum, cu locul nou. */
    private fun nudgeWidget() {
        val ids = AppWidgetManager.getInstance(this)
            .getAppWidgetIds(ComponentName(this, StelutaWidget::class.java))
        if (ids.isEmpty()) return

        sendBroadcast(
            Intent(this, StelutaWidget::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        )
    }

    private fun show(w: Weather, fromCache: Boolean) {
        findViewById<TextView>(R.id.m_place).text = w.place
        findViewById<ImageView>(R.id.m_icon).setImageResource(Wmo.iconOf(w.code, w.isDay))
        findViewById<TextView>(R.id.m_temp).text = w.temp.toString()
        findViewById<TextView>(R.id.m_desc).text =
            Wmo.labelOf(this, w.code).replaceFirstChar { it.uppercase(locale) }
        findViewById<TextView>(R.id.m_feels).text =
            if (w.feels != w.temp) getString(R.string.feels_like, w.feels) else ""
        findViewById<TextView>(R.id.m_quip).text = Quips.forWeather(this, w)

        findViewById<TextView>(R.id.m_min).text = "${w.min}°"
        findViewById<TextView>(R.id.m_max).text = "${w.max}°"
        findViewById<TextView>(R.id.m_wind).text = "${w.wind}"

        showDays(w)

        val t = SimpleDateFormat("HH:mm", locale).format(Date(w.at))
        findViewById<TextView>(R.id.m_updated).text =
            getString(if (fromCache) R.string.updated_cached else R.string.updated_fresh, t)
    }

    /**
     * Randurile de prognoza.
     *
     * Se sterg si se refac de fiecare data. Ar fi si o cale mai destepta - sa se
     * refoloseasca randurile existente - dar pentru sase randuri desenate de doua ori pe
     * deschidere, aia ar fi cod in plus pentru un castig pe care nu-l vede nimeni.
     */
    private fun showDays(w: Weather) {
        val holder = findViewById<LinearLayout>(R.id.m_days)
        holder.removeAllViews()
        val inflater = LayoutInflater.from(this)

        w.days.forEach { d ->
            val row: View = inflater.inflate(R.layout.row_day, holder, false)
            row.findViewById<TextView>(R.id.r_name).text = dayName(d.iso)
            // Zilele intregi se deseneaza mereu cu iconita de zi: o zi n-are cum sa fie
            // „noapte".
            row.findViewById<ImageView>(R.id.r_icon)
                .setImageResource(Wmo.iconOf(d.code, isDay = true))
            row.findViewById<TextView>(R.id.r_min).text = "${d.min}°"
            row.findViewById<TextView>(R.id.r_max).text = "${d.max}°"
            holder.addView(row)
        }
    }

    private fun dayName(iso: String): String = try {
        val p = iso.split("-")
        val cal = Calendar.getInstance().apply {
            set(p[0].toInt(), p[1].toInt() - 1, p[2].toInt(), 12, 0, 0)
        }
        val today = Calendar.getInstance()
        val sameDay = cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

        if (sameDay) {
            getString(R.string.today)
        } else {
            SimpleDateFormat("EEE", locale).format(cal.time).replaceFirstChar { it.uppercase(locale) }
        }
    } catch (e: Exception) {
        ""
    }
}
