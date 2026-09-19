package ro.iepur.steluta

import android.content.Context

/** Ce fel de iconita se deseneaza. Nu e acelasi lucru cu textul afisat. */
enum class Kind { CLEAR, PARTLY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM }

/** O zi din prognoza, asa cum apare in randul de jos al widget-ului. */
data class Day(
    /** Data in format ISO, `2026-09-05`. Se formateaza la afisare, nu aici. */
    val iso: String,
    val code: Int,
    val min: Int,
    val max: Int,
)

/**
 * Vremea, gata de pus pe ecran.
 *
 * Cifrele sunt deja rotunjite: widget-ul si ecranul principal arata acelasi lucru, si
 * niciunul nu mai are voie sa rotunjeasca a doua oara.
 */
data class Weather(
    val place: String,
    val temp: Int,
    val feels: Int,
    val code: Int,
    val wind: Int,
    val isDay: Boolean,
    val min: Int,
    val max: Int,
    val days: List<Day>,
    /** Cand a fost adus raspunsul. Ecranul spune „de la HH:mm" pe baza lui. */
    val at: Long,
)

/**
 * Codurile WMO, asa cum le da Open-Meteo.
 *
 * Ce nu e in tabel cade pe „innorat", care nu minte niciodata prea tare. Textele sunt
 * resurse (`wmo_*` din strings.xml), ca sa vina in limba telefonului.
 */
object Wmo {

    private val TABLE: Map<Int, Pair<Kind, Int>> = mapOf(
        0 to (Kind.CLEAR to R.string.wmo_clear),
        1 to (Kind.CLEAR to R.string.wmo_mostly_clear),
        2 to (Kind.PARTLY to R.string.wmo_partly),
        3 to (Kind.CLOUDY to R.string.wmo_cloudy),
        45 to (Kind.FOG to R.string.wmo_fog),
        48 to (Kind.FOG to R.string.wmo_rime_fog),
        51 to (Kind.DRIZZLE to R.string.wmo_drizzle_light),
        53 to (Kind.DRIZZLE to R.string.wmo_drizzle),
        55 to (Kind.DRIZZLE to R.string.wmo_drizzle_dense),
        56 to (Kind.DRIZZLE to R.string.wmo_drizzle_freezing),
        57 to (Kind.DRIZZLE to R.string.wmo_drizzle_freezing),
        61 to (Kind.RAIN to R.string.wmo_rain_light),
        63 to (Kind.RAIN to R.string.wmo_rain),
        65 to (Kind.RAIN to R.string.wmo_rain_heavy),
        66 to (Kind.RAIN to R.string.wmo_rain_freezing),
        67 to (Kind.RAIN to R.string.wmo_rain_freezing),
        71 to (Kind.SNOW to R.string.wmo_snow_light),
        73 to (Kind.SNOW to R.string.wmo_snow),
        75 to (Kind.SNOW to R.string.wmo_snow_heavy),
        77 to (Kind.SNOW to R.string.wmo_snow_grains),
        80 to (Kind.RAIN to R.string.wmo_showers_light),
        81 to (Kind.RAIN to R.string.wmo_showers),
        82 to (Kind.RAIN to R.string.wmo_showers_heavy),
        85 to (Kind.SNOW to R.string.wmo_snow_showers),
        86 to (Kind.SNOW to R.string.wmo_snow_showers),
        95 to (Kind.STORM to R.string.wmo_storm),
        96 to (Kind.STORM to R.string.wmo_storm_hail),
        99 to (Kind.STORM to R.string.wmo_storm_hail),
    )

    fun kindOf(code: Int): Kind = TABLE[code]?.first ?: Kind.CLOUDY

    fun labelOf(context: Context, code: Int): String =
        context.getString(TABLE[code]?.second ?: R.string.wmo_cloudy)

    /**
     * Ce desen se pune pentru codul dat.
     *
     * **Noaptea, „senin" nu se deseneaza cu un soare** - un sunburst portocaliu la ora
     * doua dimineata ar fi prima minciuna a widget-ului. Doar cele doua feluri de vreme
     * senina au varianta de noapte; ploaia arata la fel la orice ora.
     *
     * Randul de jos, care rezuma zile intregi, cere mereu varianta de zi: o zi n-are cum
     * sa fie „noapte".
     */
    fun iconOf(code: Int, isDay: Boolean): Int = when (kindOf(code)) {
        Kind.CLEAR -> if (isDay) R.drawable.ic_clear else R.drawable.ic_clear_night
        Kind.PARTLY -> if (isDay) R.drawable.ic_partly else R.drawable.ic_partly_night
        Kind.CLOUDY -> R.drawable.ic_cloudy
        Kind.FOG -> R.drawable.ic_fog
        Kind.DRIZZLE -> R.drawable.ic_drizzle
        Kind.RAIN -> R.drawable.ic_rain
        Kind.SNOW -> R.drawable.ic_snow
        Kind.STORM -> R.drawable.ic_storm
    }
}
