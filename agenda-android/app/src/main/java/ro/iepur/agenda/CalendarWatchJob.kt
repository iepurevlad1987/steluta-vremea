package ro.iepur.agenda

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.provider.CalendarContract

/**
 * Pandeste calendarul: cand Google Calendar sincronizeaza ceva nou (eveniment adaugat,
 * mutat, sters - de pe telefon sau de pe calculator), Android porneste job-ul asta si
 * widget-ul se redeseneaza in cateva secunde.
 *
 * Un job cu „content trigger" se consuma dupa o pornire, de aceea [AgendaWidget.redraw]
 * il programeaza din nou de fiecare data.
 */
class CalendarWatchJob : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        AgendaWidget.redraw(this)
        return false
    }

    override fun onStopJob(params: JobParameters) = false

    companion object {
        private const val ID = 4207

        fun schedule(c: Context) {
            val info = JobInfo.Builder(ID, ComponentName(c, CalendarWatchJob::class.java))
                .addTriggerContentUri(
                    JobInfo.TriggerContentUri(
                        CalendarContract.CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
                    ),
                )
                .setTriggerContentUpdateDelay(2_000)
                .setTriggerContentMaxDelay(10_000)
                .build()
            c.getSystemService(JobScheduler::class.java).schedule(info)
        }

        fun cancel(c: Context) = c.getSystemService(JobScheduler::class.java).cancel(ID)
    }
}
