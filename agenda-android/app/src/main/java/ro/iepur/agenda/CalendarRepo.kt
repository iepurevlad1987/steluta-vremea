package ro.iepur.agenda

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Instances
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

data class Ev(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val color: Int,
    val location: String?,
    /** Ziua sub care apare: ziua inceperii, sau azi pentru ce a inceput deja. */
    val day: LocalDate,
)

/**
 * Citeste evenimentele direct din calendarul telefonului (CalendarContract) - adica tot ce
 * sincronizeaza Google Calendar, fara cont, cheie de API sau internet. Doar calendarele
 * bifate ca vizibile in aplicatia de calendar; invitatiile refuzate nu apar.
 */
object CalendarRepo {

    private val PROJECTION = arrayOf(
        Instances.EVENT_ID,
        Instances.TITLE,
        Instances.BEGIN,
        Instances.END,
        Instances.ALL_DAY,
        Instances.DISPLAY_COLOR,
        Instances.EVENT_LOCATION,
        Instances.SELF_ATTENDEE_STATUS,
    )

    fun hasPermission(c: Context) =
        c.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** `null` = fara permisiune. */
    fun load(c: Context, days: Int): List<Ev>? {
        if (!hasPermission(c)) return null
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val now = System.currentTimeMillis()
        // Evenimentele de o zi intreaga sunt tinute in UTC: o zi de calendar = miezul noptii
        // UTC -> miezul noptii UTC. De aici cele doua repere separate.
        val todayUtc = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val from = minOf(today.atStartOfDay(zone).toInstant().toEpochMilli(), todayUtc)
        val to = today.plusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()

        val uri = Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from)
            ContentUris.appendId(it, to)
        }.build()

        val out = ArrayList<Ev>()
        try {
            c.contentResolver.query(uri, PROJECTION, "${Instances.VISIBLE} = 1", null, null)?.use { cur ->
                while (cur.moveToNext()) {
                    if (cur.getInt(7) == Attendees.ATTENDEE_STATUS_DECLINED) continue
                    val begin = cur.getLong(2)
                    val end = cur.getLong(3)
                    val allDay = cur.getInt(4) == 1
                    val day: LocalDate
                    if (allDay) {
                        if (end <= todayUtc) continue
                        day = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
                    } else {
                        if (end <= now && end != begin) continue
                        if (end == begin && begin < now) continue
                        day = Instant.ofEpochMilli(begin).atZone(zone).toLocalDate()
                    }
                    val shown = if (day.isBefore(today)) today else day
                    if (!shown.isBefore(today.plusDays(days.toLong()))) continue
                    out += Ev(
                        id = cur.getLong(0),
                        title = cur.getString(1)?.takeIf { it.isNotBlank() } ?: c.getString(R.string.untitled),
                        begin = begin,
                        end = end,
                        allDay = allDay,
                        color = cur.getInt(5) or 0xFF000000.toInt(),
                        location = cur.getString(6)?.takeIf { it.isNotBlank() },
                        day = shown,
                    )
                }
            }
        } catch (_: SecurityException) {
            return null
        }
        // Pe zile; in fiecare zi, intai cele de o zi intreaga, apoi dupa ora.
        return out.sortedWith(compareBy<Ev>({ it.day }, { !it.allDay }, { it.begin }, { it.title }))
    }
}
