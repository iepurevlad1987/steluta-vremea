package ro.iepur.agenda

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Toast

/**
 * Trambulina pentru apasarile din widget. Exista pentru ca lista din widget cere un
 * PendingIntent *mutabil* (fiecare rand isi completeaza datele), iar de la Android 14 unul
 * mutabil trebuie sa tinteasca explicit o componenta a aplicatiei. De aici se deschide
 * aplicatia de calendar si trambulina se inchide pe loc, fara sa se vada.
 */
class OpenActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = when (intent.getStringExtra(MODE)) {
            MODE_ADD -> Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
            MODE_EVENT -> Intent(
                Intent.ACTION_VIEW,
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, intent.getLongExtra(ID, 0)),
            )
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, intent.getLongExtra(BEGIN, 0))
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, intent.getLongExtra(END, 0))
            MODE_APP -> Intent(this, MainActivity::class.java)
            else -> Intent(
                Intent.ACTION_VIEW,
                CalendarContract.CONTENT_URI.buildUpon()
                    .appendPath("time")
                    .appendPath(intent.getLongExtra(TIME, System.currentTimeMillis()).toString())
                    .build(),
            )
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(target)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_calendar_app, Toast.LENGTH_SHORT).show()
        }
        finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val MODE = "mode"
        private const val MODE_ADD = "add"
        private const val MODE_EVENT = "event"
        private const val MODE_DAY = "day"
        private const val MODE_APP = "app"
        private const val ID = "id"
        private const val BEGIN = "begin"
        private const val END = "end"
        private const val TIME = "time"

        private fun base(c: Context) = Intent(c, OpenActivity::class.java)

        private fun immutable(c: Context, code: Int, i: Intent) = PendingIntent.getActivity(
            c, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        fun addPending(c: Context) = immutable(c, 10, base(c).putExtra(MODE, MODE_ADD))
        fun appPending(c: Context) = immutable(c, 11, base(c).putExtra(MODE, MODE_APP))
        fun dayPending(c: Context, time: Long) =
            immutable(c, 12, base(c).putExtra(MODE, MODE_DAY).putExtra(TIME, time))

        /** Sablonul pentru randurile listei: mutabil, ca fiecare rand sa-si puna datele. */
        fun template(c: Context): PendingIntent = PendingIntent.getActivity(
            c, 20, base(c), PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        fun eventFill(e: Ev) = Intent()
            .putExtra(MODE, MODE_EVENT)
            .putExtra(ID, e.id)
            .putExtra(BEGIN, e.begin)
            .putExtra(END, e.end)

        fun dayFill(time: Long) = Intent().putExtra(MODE, MODE_DAY).putExtra(TIME, time)
    }
}
