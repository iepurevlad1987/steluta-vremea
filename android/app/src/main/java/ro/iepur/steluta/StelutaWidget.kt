package ro.iepur.steluta

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Actiunea trimisa cand se apasa pe widget. Declarata si in manifest. */
private const val ACTION_REFRESH = "ro.iepur.steluta.REFRESH"

private val RO = Locale.forLanguageTag("ro-RO")

/**
 * Widget-ul de pe ecranul de start.
 *
 * **Se deseneaza de doua ori la fiecare reimprospatare, si asta e intentionat.** Intai
 * cu ce se stia deja, imediat, ca sa nu existe nicio clipa de „se încarcă…" pe un ecran
 * de start; apoi, cand raspunde reteaua, cu cifrele noi. Un widget care se goleste cat
 * asteapta e mai rau decat unul care arata vremea de acum zece minute.
 *
 * Android reimprospateaza singur o data la 30 de minute (minimul pe care il accepta),
 * iar un tap pe widget cere date noi imediat.
 */
class StelutaWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // Ce stim deja, pe ecran acum.
        WeatherStore.load(context)?.let { known ->
            appWidgetIds.forEach { id -> manager.updateAppWidget(id, build(context, known)) }
        }
        refreshInBackground(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refreshInBackground(context)
    }

    /**
     * Cere vremea pe un fir de fundal si redeseneaza cand vine.
     *
     * `goAsync()` e felul in care un receiver spune Android-ului „nu ma opri inca".
     * Bugetul e in jur de zece secunde, de aia apelul are timeout mai scurt de atat -
     * altfel procesul ar fi taiat exact cand raspunsul era pe drum.
     */
    private fun refreshInBackground(context: Context) {
        val pending = goAsync()
        Thread {
            try {
                WeatherApi.fetch(PlaceStore.load(context))?.let { fresh ->
                    WeatherStore.save(context, fresh)
                    redrawAll(context, fresh)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun redrawAll(context: Context, w: Weather) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, StelutaWidget::class.java))
        ids.forEach { id -> manager.updateAppWidget(id, build(context, w)) }
    }

    /** Umple layout-ul. Nicio decizie aici - doar asezarea a ce s-a hotarat deja. */
    private fun build(context: Context, w: Weather): RemoteViews {
        val v = RemoteViews(context.packageName, R.layout.widget)

        v.setTextViewText(R.id.place, w.place)
        v.setTextViewText(R.id.desc, Wmo.labelOf(w.code).replaceFirstChar { it.uppercase(RO) })
        v.setTextViewText(R.id.date, SimpleDateFormat("EEE d MMM", RO).format(Date(w.at)))
        v.setTextViewText(R.id.temp, "${w.temp}°")
        v.setTextViewText(R.id.minmax, "${w.max}°\n${w.min}°")
        v.setTextViewText(R.id.quip, Quips.forWeather(w))
        v.setImageViewResource(R.id.icon, Wmo.iconOf(w.code, w.isDay))

        // Randul de jos incepe de maine: ziua de azi e deja sus, cu cifra ei mare.
        val next = w.days.drop(1).take(5)
        val nameIds = intArrayOf(R.id.day0_name, R.id.day1_name, R.id.day2_name, R.id.day3_name, R.id.day4_name)
        val iconIds = intArrayOf(R.id.day0_icon, R.id.day1_icon, R.id.day2_icon, R.id.day3_icon, R.id.day4_icon)
        val tempIds = intArrayOf(R.id.day0_temps, R.id.day1_temps, R.id.day2_temps, R.id.day3_temps, R.id.day4_temps)

        for (i in 0 until 5) {
            val d = next.getOrNull(i)
            if (d == null) {
                // Mai putine zile decat locuri: se golesc, nu se lasa ce era inainte.
                v.setTextViewText(nameIds[i], "")
                v.setTextViewText(tempIds[i], "")
                v.setViewVisibility(iconIds[i], android.view.View.INVISIBLE)
                continue
            }
            v.setViewVisibility(iconIds[i], android.view.View.VISIBLE)
            v.setTextViewText(nameIds[i], shortDayName(d.iso))
            v.setTextViewText(tempIds[i], "${d.max}°/${d.min}°")
            // Zilele intregi se deseneaza mereu cu iconita de zi.
            v.setImageViewResource(iconIds[i], Wmo.iconOf(d.code, isDay = true))
        }

        v.setOnClickPendingIntent(R.id.widget_root, refreshIntent(context))
        return v
    }

    /**
     * Tap pe widget = date noi.
     *
     * `FLAG_IMMUTABLE` e obligatoriu de la Android 12 incolo; fara el aplicatia crapa la
     * prima asezare a widget-ului, nu la instalare - adica exact acolo unde nu te uiti.
     */
    private fun refreshIntent(context: Context): PendingIntent {
        val intent = Intent(context, StelutaWidget::class.java).setAction(ACTION_REFRESH)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** „Sâm", „Dum" — ziua din data ISO, fara sa depinda de fusul serverului. */
    private fun shortDayName(iso: String): String = try {
        val parts = iso.split("-")
        val cal = Calendar.getInstance().apply {
            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 12, 0, 0)
        }
        SimpleDateFormat("EEE", RO).format(cal.time).replaceFirstChar { it.uppercase(RO) }
    } catch (e: Exception) {
        ""
    }
}
