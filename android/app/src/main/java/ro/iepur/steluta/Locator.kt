package ro.iepur.steluta

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import java.util.Locale

private const val TAG = "StelutaLocator"

/**
 * Unde suntem.
 *
 * **Fara Google Play Services.** `LocationManager` e in Android de la inceput si ajunge
 * pentru vreme: aici nu ne trebuie precizie de metri, ne trebuie orasul. In schimb,
 * aplicatia merge si pe un telefon fara servicii Google.
 */
object Locator {

    /** Cat asteptam o pozitie noua cand nu exista niciuna salvata de sistem. */
    private const val WAIT_MS = 15_000L

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Cauta locul curent. **Blocheaza** - se cheama de pe un fir de fundal.
     *
     * Intai incearca ultima pozitie stiuta de sistem, care e instantanee si de obicei
     * destul de proaspata. Doar daca nu exista niciuna cere una noua, si atunci asteapta
     * cel mult [WAIT_MS] - un buton care se invarte la nesfarsit fiindca telefonul e
     * intr-o pivnita fara semnal e mai rau decat unul care spune ca n-a reusit.
     */
    fun find(context: Context): Spot? {
        if (!hasPermission(context)) return null

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val known = lastKnown(lm)
        val location = known ?: requestOne(lm) ?: return null

        return Spot(
            lat = round4(location.latitude),
            lon = round4(location.longitude),
            name = nameFor(context, location.latitude, location.longitude),
        )
    }

    /**
     * Cea mai recenta pozitie pe care o are deja sistemul, din orice sursa.
     *
     * Se iau toate sursele active, nu doar GPS-ul: intr-o cladire, GPS-ul n-are nimic, dar
     * reteaua are de obicei o pozitie de acum cateva minute - suficient de buna cand
     * intrebarea e „in ce oras sunt".
     */
    private fun lastKnown(lm: LocationManager): Location? = try {
        lm.getProviders(true)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
    } catch (e: SecurityException) {
        Log.w(TAG, "Permisiunea a fost retrasă între timp", e)
        null
    }

    /** O singura pozitie noua, cu asteptare marginita. */
    private fun requestOne(lm: LocationManager): Location? {
        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }

        val lock = Object()
        var result: Location? = null

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                synchronized(lock) {
                    result = location
                    lock.notifyAll()
                }
            }

            // Metodele astea sunt abstracte pe Android 8-10; pe versiunile noi au
            // implementari implicite. Scrise aici, merg pe toate.
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
            @Deprecated("Cerută de interfață pe API vechi")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit
        }

        return try {
            lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            synchronized(lock) {
                if (result == null) lock.wait(WAIT_MS)
                result
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nu s-a putut cere poziția", e)
            null
        } finally {
            runCatching { lm.removeUpdates(listener) }
        }
    }

    /**
     * Numele locului, din coordonate.
     *
     * `Geocoder` e in Android, deci fara cheie si fara alt serviciu. Daca nu raspunde -
     * se intampla, e un serviciu al telefonului si poate lipsi - se scriu coordonatele.
     * Un nume gresit ar fi mai rau decat doua cifre.
     */
    private fun nameFor(context: Context, lat: Double, lon: Double): String {
        val fallback = "%.2f, %.2f".format(Locale.US, lat, lon)
        if (!Geocoder.isPresent()) return fallback

        return try {
            @Suppress("DEPRECATION")
            val hit = Geocoder(context, Locale.forLanguageTag("ro-RO"))
                .getFromLocation(lat, lon, 1)
                ?.firstOrNull()
                ?: return fallback

            hit.locality ?: hit.subAdminArea ?: hit.adminArea ?: fallback
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder n-a răspuns", e)
            fallback
        }
    }

    /** Patru zecimale = vreo zece metri. Mai mult n-are ce face un buletin meteo. */
    private fun round4(v: Double): Double = Math.round(v * 10_000.0) / 10_000.0
}
