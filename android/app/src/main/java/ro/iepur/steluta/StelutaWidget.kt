package ro.iepur.steluta

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Actiunea din versiunea 1.0, cand tot widget-ul era buton de reimprospatare. Ramane
 * declarata ca un widget asezat inainte de actualizare sa nu devina mort pana la prima
 * lui redesenare.
 */
private const val ACTION_REFRESH = "ro.iepur.steluta.REFRESH"

/** Butonul „ore ⇄ / zile ⇄". Declarata si in manifest. */
private const val ACTION_TOGGLE = "ro.iepur.steluta.TOGGLE_MODE"

private const val PREFS = "steluta"

/** `true` = randul de jos arata ore; lipsa sau `false` = zile, ca inainte. */
private const val SHOW_HOURS = "widget_show_hours"

/** Din cate in cate ore se ia o coloana: cinci coloane acopera astfel 13 ore. */
private const val HOUR_STEP = 3

/**
 * Widget-ul de pe ecranul de start.
 *
 * **Se deseneaza de doua ori la fiecare reimprospatare, si asta e intentionat.** Intai
 * cu ce se stia deja, imediat, ca sa nu existe nicio clipa de „se încarcă…" pe un ecran
 * de start; apoi, cand raspunde reteaua, cu cifrele noi. Un widget care se goleste cat
 * asteapta e mai rau decat unul care arata vremea de acum zece minute.
 *
 * Trei locuri de apasat:
 * - **butonul mic ↻** de langa comentariu: locatia de acum, vremea de acolo si alt
 *   comentariu - toate trei, prin [RefreshService];
 * - **„ore ⇄ / zile ⇄"**, deasupra randului de jos: il comuta intre urmatoarele cinci zile
 *   si urmatoarele ore, din trei in trei. Orele vin in acelasi apel cu restul vremii,
 *   deci comutarea nu asteapta dupa retea;
 * - **restul widget-ului** deschide aplicatia.
 *
 * In rest, Android il reimprospateaza singur o data la 30 de minute (minimul pe care il
 * accepta), cu locul salvat si fara sa schimbe comentariul inainte de ora lui.
 */
class StelutaWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // Ce stim deja, pe ecran acum. Si fara nimic stiut, butoanele trebuie legate:
        // altfel un widget pus inainte de primul raspuns n-ar raspunde la nicio apasare.
        val known = WeatherStore.load(context)
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, build(context, known)) }
        refreshInBackground(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> refreshInBackground(context)
            ACTION_TOGGLE -> {
                val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                // `commit`, nu `apply`: redesenarea de pe randul urmator citeste valoarea.
                p.edit().putBoolean(SHOW_HOURS, !p.getBoolean(SHOW_HOURS, false)).commit()
                redrawAll(context)
            }
        }
    }

    /**
     * Reimprospatarea de rutina: vremea pentru locul salvat, pe un fir de fundal.
     *
     * `goAsync()` e felul in care un receiver spune Android-ului „nu ma opri inca".
     * Bugetul e in jur de zece secunde, de aia apelul are timeout mai scurt de atat -
     * altfel procesul ar fi taiat exact cand raspunsul era pe drum. Locatia nu se cauta
     * aici: n-ar incapea in buget si, din fundal, n-ar avea voie.
     */
    private fun refreshInBackground(context: Context) {
        val pending = goAsync()
        Thread {
            try {
                WeatherApi.fetch(PlaceStore.load(context))?.let { fresh ->
                    WeatherStore.save(context, fresh)
                    redrawAll(context)
                }
            } finally {
                pending.finish()
            }
        }.start()
    }

    companion object {

        /** Redeseneaza toate widget-urile cu ce e salvat acum. */
        fun redrawAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, StelutaWidget::class.java))
            if (ids.isEmpty()) return
            val known = WeatherStore.load(context)
            ids.forEach { id -> manager.updateAppWidget(id, build(context, known)) }
        }

        /**
         * Semnul ca butonul a fost auzit: iconita devine „⋯" si locul scrie „Se caută…".
         *
         * `partiallyUpdateAppWidget` schimba doar cele doua lucruri si lasa restul cum e.
         * Revenirea o face [redrawAll], care deseneaza totul de la zero.
         */
        fun showBusy(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, StelutaWidget::class.java))
            if (ids.isEmpty()) return
            val v = RemoteViews(context.packageName, R.layout.widget)
            v.setImageViewResource(R.id.refresh, R.drawable.ic_refresh_busy)
            v.setTextViewText(R.id.place, context.getString(R.string.locating))
            manager.partiallyUpdateAppWidget(ids, v)
        }

        /**
         * Umple layout-ul. Nicio decizie aici - doar asezarea a ce s-a hotarat deja.
         *
         * Fara vreme stiuta ([w] `null`) se leaga doar apasarile, iar textele raman cele
         * din layout („se încarcă…").
         */
        private fun build(context: Context, w: Weather?): RemoteViews {
            val v = RemoteViews(context.packageName, R.layout.widget)
            v.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            v.setOnClickPendingIntent(R.id.refresh, refreshIntent(context))
            v.setOnClickPendingIntent(R.id.mode_toggle, toggleIntent(context))
            v.setImageViewResource(R.id.refresh, R.drawable.ic_refresh)

            val hoursMode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(SHOW_HOURS, false)
            // Butonul scrie unde duce, titlul scrie unde esti.
            v.setTextViewText(
                R.id.bottom_title,
                context.getString(if (hoursMode) R.string.next_hours else R.string.next_days),
            )
            v.setTextViewText(
                R.id.mode_toggle,
                context.getString(if (hoursMode) R.string.show_days else R.string.show_hours),
            )
            if (w == null) return v

            // Limba telefonului, citita acum si nu tinuta intr-o constanta: daca omul
            // schimba limba, urmatoarea redesenare trebuie sa vina deja in cea noua.
            val locale = Locale.getDefault()

            v.setTextViewText(R.id.place, w.place)
            v.setTextViewText(
                R.id.desc,
                Wmo.labelOf(context, w.code).replaceFirstChar { it.uppercase(locale) },
            )
            v.setTextViewText(R.id.date, SimpleDateFormat("EEE d MMM", locale).format(Date(w.at)))
            v.setTextViewText(R.id.temp, "${w.temp}°")
            v.setTextViewText(R.id.minmax, "${w.max}°\n${w.min}°")
            v.setTextViewText(R.id.quip, Quips.forWeather(context, w))
            v.setImageViewResource(R.id.icon, Wmo.iconOf(w.code, w.isDay))

            val nameIds = intArrayOf(R.id.day0_name, R.id.day1_name, R.id.day2_name, R.id.day3_name, R.id.day4_name)
            val iconIds = intArrayOf(R.id.day0_icon, R.id.day1_icon, R.id.day2_icon, R.id.day3_icon, R.id.day4_icon)
            val tempIds = intArrayOf(R.id.day0_temps, R.id.day1_temps, R.id.day2_temps, R.id.day3_temps, R.id.day4_temps)

            // Aceleasi cinci coloane pentru ambele moduri: doar ce se scrie in ele difera.
            val cells: List<Triple<String, Int, String>> = if (hoursMode) {
                nextHours(w.hours).map { h ->
                    // Orele au iconita lor de zi sau de noapte: la 23:00 senin e luna.
                    Triple(h.label, Wmo.iconOf(h.code, h.isDay), "${h.temp}°")
                }
            } else {
                // Randul de jos incepe de maine: ziua de azi e deja sus, cu cifra ei mare.
                // Zilele intregi se deseneaza mereu cu iconita de zi.
                w.days.drop(1).take(5).map { d ->
                    Triple(shortDayName(d.iso, locale), Wmo.iconOf(d.code, isDay = true), "${d.max}°/${d.min}°")
                }
            }

            for (i in 0 until 5) {
                val c = cells.getOrNull(i)
                if (c == null) {
                    // Mai putine date decat locuri: se golesc, nu se lasa ce era inainte.
                    v.setTextViewText(nameIds[i], "")
                    v.setTextViewText(tempIds[i], "")
                    v.setViewVisibility(iconIds[i], View.INVISIBLE)
                    continue
                }
                v.setViewVisibility(iconIds[i], View.VISIBLE)
                v.setTextViewText(nameIds[i], c.first)
                v.setImageViewResource(iconIds[i], c.second)
                v.setTextViewText(tempIds[i], c.third)
            }

            return v
        }

        /**
         * Cinci ore, din [HOUR_STEP] in [HOUR_STEP], incepand cu **prima ora plina de dupa
         * acum** - ora curenta e deja sus, cu cifra ei mare.
         *
         * Se alege la desenare, nu la descarcare: widget-ul se redeseneaza si cand n-a venit
         * nimic nou, iar la 16:10 prima coloana trebuie sa fie 17:00, nu ora la care
         * raspunsese reteaua.
         */
        private fun nextHours(hours: List<Hour>, now: Long = System.currentTimeMillis()): List<Hour> =
            hours.filter { it.epoch > now }
                .filterIndexed { i, _ -> i % HOUR_STEP == 0 }
                .take(5)

        private fun toggleIntent(context: Context): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                3,
                Intent(context, StelutaWidget::class.java).setAction(ACTION_TOGGLE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        /**
         * Butonul ↻.
         *
         * **Cu permisiunea de locatie:** porneste [RefreshService] direct, fara sa deschida
         * nimic. **Fara ea:** deschide aplicatia, care o cere - un serviciu pornit din
         * widget n-are cum sa arate fereastra de permisiune. Dupa ce e data, aplicatia
         * redeseneaza widget-ul, si de-atunci butonul merge pe prima cale.
         *
         * `FLAG_IMMUTABLE` e obligatoriu de la Android 12 incolo; fara el aplicatia crapa la
         * prima asezare a widget-ului, nu la instalare - adica exact acolo unde nu te uiti.
         */
        private fun refreshIntent(context: Context): PendingIntent {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            if (!Locator.hasPermission(context)) {
                val intent = Intent(context, MainActivity::class.java)
                    .setAction(MainActivity.ACTION_LOCATE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                return PendingIntent.getActivity(context, 1, intent, flags)
            }
            return PendingIntent.getForegroundService(
                context,
                2,
                Intent(context, RefreshService::class.java),
                flags,
            )
        }

        private fun openAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        /** „Sâm", „Sat" - ziua din data ISO, fara sa depinda de fusul serverului. */
        private fun shortDayName(iso: String, locale: Locale): String = try {
            val parts = iso.split("-")
            val cal = Calendar.getInstance().apply {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 12, 0, 0)
            }
            SimpleDateFormat("EEE", locale).format(cal.time).replaceFirstChar { it.uppercase(locale) }
        } catch (e: Exception) {
            ""
        }
    }
}
