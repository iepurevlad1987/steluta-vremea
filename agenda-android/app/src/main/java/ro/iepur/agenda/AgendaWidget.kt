package ro.iepur.agenda

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

const val ACTION_REFRESH = "ro.iepur.agenda.REFRESH"

/**
 * Widget-ul de pe ecranul de start: data de azi, butonul +, si dedesubt evenimentele
 * urmatoarelor zile, grupate pe zile, intr-o lista care se deruleaza.
 *
 * Se redeseneaza:
 * - cand se schimba ceva in calendar ([CalendarWatchJob], in cateva secunde);
 * - cand se termina un eveniment de azi, si la miezul noptii (o alarma neexacta);
 * - la schimbarea orei, a fusului sau a limbii;
 * - la orice schimbare din aplicatie;
 * - si, de rezerva, o data la 30 de minute.
 */
class AgendaWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        redraw(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle,
    ) = redraw(context)

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> redraw(context)
        }
    }

    override fun onDisabled(context: Context) {
        CalendarWatchJob.cancel(context)
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    }

    companion object {

        fun redraw(c: Context) {
            val manager = AppWidgetManager.getInstance(c)
            val ids = manager.getAppWidgetIds(ComponentName(c, AgendaWidget::class.java))
            if (ids.isEmpty()) return
            val events = CalendarRepo.load(c, Prefs.days(c))
            ids.forEach { manager.updateAppWidget(it, build(c, events)) }
            CalendarWatchJob.schedule(c)
            scheduleNext(c, events)
        }

        private fun refreshIntent(c: Context): PendingIntent = PendingIntent.getBroadcast(
            c, 0,
            Intent(c, AgendaWidget::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        /**
         * Urmatoarea redesenare: cand incepe sau se termina primul eveniment de azi (ca sa
         * apara „Acum" si sa dispara ce s-a terminat), cel tarziu la miezul noptii.
         * Neexacta dinadins - nu cere permisiunea de alarme exacte, iar un minut-doua nu conteaza.
         */
        private fun scheduleNext(c: Context, events: List<Ev>?) {
            val zone = ZoneId.systemDefault()
            val now = System.currentTimeMillis()
            var next = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 30_000
            events?.filter { !it.allDay }?.forEach { e ->
                if (e.begin > now) next = minOf(next, e.begin)
                if (e.end > now) next = minOf(next, e.end)
            }
            next = maxOf(next, now + 60_000)
            c.getSystemService(AlarmManager::class.java)
                .setAndAllowWhileIdle(AlarmManager.RTC, next, refreshIntent(c))
        }

        private fun build(c: Context, events: List<Ev>?): RemoteViews {
            val light = Prefs.light(c)
            val opacity = Prefs.opacity(c)
            val primary = if (light) 0xFF1C1B21.toInt() else 0xFFFFFFFF.toInt()
            val secondary = if (light) 0xB31C1B21.toInt() else 0xD9FFFFFF.toInt()
            val locale = Locale.getDefault()
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)

            val rv = RemoteViews(c.packageName, R.layout.widget)

            // Fundalul: o forma rotunjita alba, colorata si facuta transparenta din cod.
            rv.setInt(R.id.bg, "setColorFilter", if (light) 0xFFF6F4F9.toInt() else 0xFF14131A.toInt())
            rv.setInt(R.id.bg, "setImageAlpha", opacity * 255 / 100)
            // Conturul fin: alb pe fundal inchis, umbra fina pe fundal deschis.
            if (Prefs.edge(c)) {
                rv.setViewVisibility(R.id.edge, View.VISIBLE)
                rv.setInt(R.id.edge, "setColorFilter", if (light) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                rv.setInt(R.id.edge, "setImageAlpha", if (light) 28 else 64)
            } else {
                rv.setViewVisibility(R.id.edge, View.GONE)
            }

            // Antetul: „Luni, 16 noiembrie" si cate sunt azi.
            val date = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", locale))
            rv.setTextViewText(R.id.date, date.replaceFirstChar { it.titlecase(locale) })
            rv.setTextColor(R.id.date, primary)
            rv.setTextColor(R.id.subtitle, secondary)
            rv.setOnClickPendingIntent(R.id.header, OpenActivity.dayPending(c, System.currentTimeMillis()))
            rv.setOnClickPendingIntent(R.id.add, OpenActivity.addPending(c))
            rv.setTextColor(R.id.empty, secondary)

            if (events == null) {
                rv.setTextViewText(R.id.subtitle, c.getString(R.string.no_access_short))
                rv.setTextViewText(R.id.empty, c.getString(R.string.need_permission))
                rv.setViewVisibility(R.id.list, View.GONE)
                rv.setViewVisibility(R.id.empty, View.VISIBLE)
                rv.setOnClickPendingIntent(R.id.empty, OpenActivity.appPending(c))
                return rv
            }

            val todayCount = events.count { it.day == today }
            rv.setTextViewText(
                R.id.subtitle,
                if (todayCount == 0) c.getString(R.string.nothing_today)
                else c.resources.getQuantityString(R.plurals.events_today, todayCount, todayCount),
            )
            rv.setTextViewText(R.id.empty, c.getString(R.string.no_events))
            rv.setViewVisibility(R.id.list, View.VISIBLE)
            rv.setOnClickPendingIntent(R.id.empty, OpenActivity.dayPending(c, System.currentTimeMillis()))

            val time = DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(c)) "HH:mm" else "h:mm a", locale)
            val dayShort = DateTimeFormatter.ofPattern("EEE", locale)
            val dayLong = DateTimeFormatter.ofPattern("EEEE, d MMM", locale)
            val now = System.currentTimeMillis()

            val items = RemoteViews.RemoteCollectionItems.Builder()
                .setHasStableIds(true)
                .setViewTypeCount(2)

            events.groupBy { it.day }.forEach { (day, list) ->
                val h = RemoteViews(c.packageName, R.layout.item_day)
                val label = when (day) {
                    today -> c.getString(R.string.today) + " · " + day.format(dayLong)
                    today.plusDays(1) -> c.getString(R.string.tomorrow) + " · " + day.format(dayLong)
                    else -> day.format(dayLong).replaceFirstChar { it.titlecase(locale) }
                }
                h.setTextViewText(R.id.day, label)
                h.setTextColor(R.id.day, secondary)
                h.setOnClickFillInIntent(
                    R.id.day,
                    OpenActivity.dayFill(day.atStartOfDay(zone).plusHours(9).toInstant().toEpochMilli()),
                )
                items.addItem(-(day.toEpochDay() + 1), h)

                list.forEach { e ->
                    val r = RemoteViews(c.packageName, R.layout.item_event)
                    r.setInt(R.id.card, "setBackgroundResource", if (light) R.drawable.card_light else R.drawable.card_dark)
                    r.setInt(R.id.accent, "setColorFilter", e.color)
                    r.setTextViewText(R.id.title, e.title)
                    r.setTextColor(R.id.title, primary)
                    r.setTextColor(R.id.time, secondary)

                    val start = Instant.ofEpochMilli(e.begin).atZone(zone)
                    val end = Instant.ofEpochMilli(e.end).atZone(zone)
                    var t = when {
                        e.allDay -> c.getString(R.string.all_day)
                        start.toLocalDate() == end.toLocalDate() || e.end - e.begin <= 0 ->
                            start.format(time) + " – " + end.format(time)
                        start.toLocalDate().isBefore(e.day) ->
                            c.getString(R.string.until, end.format(dayShort) + " " + end.format(time))
                        else -> start.format(time) + " → " + end.format(dayShort) + " " + end.format(time)
                    }
                    if (!e.allDay && e.begin <= now && now < e.end) t = c.getString(R.string.now) + " · " + t
                    e.location?.let { t += "  ·  $it" }
                    r.setTextViewText(R.id.time, t)

                    r.setOnClickFillInIntent(R.id.card, OpenActivity.eventFill(e))
                    items.addItem(e.id * 1_000_003L + e.begin / 60_000, r)
                }
            }

            rv.setRemoteAdapter(R.id.list, items.build())
            rv.setPendingIntentTemplate(R.id.list, OpenActivity.template(c))
            rv.setEmptyView(R.id.list, R.id.empty)
            return rv
        }
    }
}
