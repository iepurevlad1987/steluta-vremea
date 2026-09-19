package ro.iepur.steluta

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

private const val TAG = "StelutaRefresh"
private const val CHANNEL = "refresh"
private const val NOTIFICATION_ID = 1

/**
 * Ce se intampla cand apesi ↻ pe widget: locatia de acum, vremea de acolo, alt comentariu.
 *
 * **De ce un serviciu in prim-plan si nu receiver-ul widget-ului.** Doua motive:
 *
 * - **Locatia.** Aplicatia are doar permisiunea „cat folosesc aplicatia", nu „tot timpul".
 *   Un receiver ruleaza „in fundal" si n-ar primi nicio pozitie. Un serviciu in prim-plan
 *   de tip `location`, pornit dintr-o apasare pe widget, **e** „in folosire": Android il
 *   lasa, fiindca apasarea a venit de la om, prin launcher-ul pe care il vede.
 * - **Timpul.** Receiver-ul are vreo zece secunde. Locatia (pana la 15) plus vremea
 *   (pana la 8) nu incap acolo.
 *
 * Notificarea ceruta de Android pentru un asemenea serviciu nici nu apuca de obicei sa
 * apara: de la Android 12, sistemul o arata abia dupa zece secunde, iar treaba se termina
 * de regula mai repede.
 */
class RefreshService : Service() {

    private val main = Handler(Looper.getMainLooper())

    /** O apasare in plus cat lucreaza deja nu porneste a doua cautare. */
    private var busy = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // **Primul lucru, inainte de orice.** Un serviciu pornit cu
        // `startForegroundService` care nu cheama `startForeground` in cateva secunde
        // omoara toata aplicatia - si asta de fiecare data cand e pornit, deci si la a doua
        // apasare, cand nu mai face nimic altceva.
        val canLocate = goForeground()
        if (busy) return START_NOT_STICKY
        busy = true

        StelutaWidget.showBusy(this)
        Thread {
            try {
                refresh(canLocate)
            } catch (e: Exception) {
                Log.w(TAG, "Reîmprospătarea a eșuat", e)
            } finally {
                // Oricum s-a terminat, butonul revine la ↻ - altfel ar ramane „⋯" pana la
                // urmatoarea reimprospatare de rutina, adica pana la jumatate de ora.
                StelutaWidget.redrawAll(this)
                main.post {
                    busy = false
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }.start()
        return START_NOT_STICKY
    }

    /** **Blocheaza** - ruleaza pe firul pornit mai sus. */
    private fun refresh(canLocate: Boolean) {
        if (canLocate) {
            // Fara pozitie - GPS oprit, pivnita - ramane locul de dinainte. Vremea tot
            // se reimprospateaza; butonul n-are voie sa nu faca nimic.
            Locator.find(this)?.let { PlaceStore.save(this, it) }
        }

        val fresh = WeatherApi.fetch(PlaceStore.load(this))
        if (fresh != null) WeatherStore.save(this, fresh)

        // Comentariul se schimba si fara internet, pe vremea stiuta: butonul promite
        // si asta, iar un mesaj nou e singurul semn ca a fost apasat cand reteaua lipseste.
        (fresh ?: WeatherStore.load(this))?.let { Quips.shuffle(this, it) }
    }

    /**
     * Intra in prim-plan si spune daca are voie sa ceara locatia.
     *
     * Pe Android 14+, tipul `location` e refuzat cu `SecurityException` daca permisiunea a
     * fost retrasa intre timp sau daca sistemul nu socoteste apasarea ca venind de la om.
     * Atunci intra ca `shortService` - un tip care nu cere nimic - si reimprospateaza doar
     * vremea, pentru locul salvat.
     */
    private fun goForeground(): Boolean {
        val n = notification()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, n)
            return true
        }
        return try {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "Locația nu e permisă acum; doar vremea", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE)
            } else {
                startForeground(NOTIFICATION_ID, n)
            }
            false
        }
    }

    /**
     * `shortService` are voie cel mult trei minute. Treaba ia sub jumatate de minut, dar
     * daca s-ar agata ceva, oprirea aici e ce impiedica Android sa inchida aplicatia fortat.
     */
    override fun onTimeout(startId: Int) {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        // Importanta minima: fara sunet, fara iconita in bara de sus. Canalul se creeaza o
        // singura data; apelul repetat nu schimba nimic.
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.notif_channel), NotificationManager.IMPORTANCE_MIN)
        )
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_refresh)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notif_refreshing))
            .setOngoing(true)
            .build()
    }
}
