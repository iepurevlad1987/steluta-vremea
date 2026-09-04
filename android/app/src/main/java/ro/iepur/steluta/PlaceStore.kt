package ro.iepur.steluta

import android.content.Context

/** Un loc pentru care se cere vremea. */
data class Spot(val lat: Double, val lon: Double, val name: String)

/**
 * Locul curent, pastrat intre porniri.
 *
 * **De ce e salvat si nu cerut de fiecare data.** Widget-ul se actualizeaza in fundal, iar
 * locatia in fundal cere `ACCESS_BACKGROUND_LOCATION` - permisiunea „Permite tot timpul",
 * pe care Android o ingroapa in doua ecrane de setari si o poate revoca singura daca nu e
 * folosita des. Asa, permisiunea ceruta e cea obisnuita, „doar cat folosesc aplicatia":
 * o iei o data cand deschizi aplicatia, iar widget-ul foloseste mai departe ce s-a salvat.
 *
 * In practica: pleci intr-un alt oras, deschizi aplicatia o data cand ajungi, si de-atunci
 * si ecranul si widget-ul arata vremea de acolo. Widget-ul singur nu te urmareste, si e
 * chiar ce ne dorim - altfel ar cere permisiunea aia.
 */
object PlaceStore {

    /** Pana cand alege omul altceva. */
    val DEFAULT = Spot(47.1911, 23.0574, "Zalău")

    private const val PREFS = "steluta"
    private const val LAT = "place_lat"
    private const val LON = "place_lon"
    private const val NAME = "place_name"

    fun load(context: Context): Spot {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = p.getString(NAME, null) ?: return DEFAULT
        // `getFloat` ar pierde din precizie la a patra zecimala, adica vreo zece metri -
        // nesemnificativ pentru vreme, dar `Double` nu costa nimic in plus asa.
        val lat = java.lang.Double.longBitsToDouble(p.getLong(LAT, 0L))
        val lon = java.lang.Double.longBitsToDouble(p.getLong(LON, 0L))
        return Spot(lat, lon, name)
    }

    fun save(context: Context, spot: Spot) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(LAT, java.lang.Double.doubleToRawLongBits(spot.lat))
            .putLong(LON, java.lang.Double.doubleToRawLongBits(spot.lon))
            .putString(NAME, spot.name)
            .apply()
    }

    /** Sterge alegerea si revine la [DEFAULT]. */
    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(LAT).remove(LON).remove(NAME)
            .apply()
    }
}
