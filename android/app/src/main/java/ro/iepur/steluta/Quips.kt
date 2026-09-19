package ro.iepur.steluta

import android.content.Context
import androidx.annotation.ArrayRes
import kotlin.random.Random

/**
 * Comentariile, pe fel de vreme.
 *
 * **Textele nu stau aici, ci in `res/values/quips.xml` si `res/values-ro/quips.xml`**,
 * cate un fisier pe limba. Asa
 * Android alege singur lista dupa limba telefonului: romana din `values-ro`, iar orice
 * alta limba cade pe engleza din `values`. O limba noua inseamna un fisier nou, fara
 * nicio linie de cod.
 *
 * Varianta web are inca doar comentariile romanesti de la inceput; de aici incolo cele
 * doua nu mai spun neaparat acelasi lucru despre aceeasi zi.
 */
object Quips {

    private const val PREFS = "steluta"
    private const val SEED = "quip_seed"

    /**
     * Ce fel de comentariu se potriveste, in ordinea in care conteaza.
     *
     * **Vremea grea bate temperatura**: cand ploua cu galeata, pe nimeni nu-l intereseaza
     * ca sunt 18 grade placute.
     */
    @ArrayRes
    private fun category(w: Weather): Int {
        val kind = Wmo.kindOf(w.code)
        return when {
            kind == Kind.STORM -> R.array.quips_storm
            kind == Kind.SNOW -> R.array.quips_snow
            kind == Kind.RAIN || kind == Kind.DRIZZLE -> R.array.quips_rain
            kind == Kind.FOG -> R.array.quips_fog
            w.wind >= 35 -> R.array.quips_wind
            w.temp <= -5 -> R.array.quips_frost
            w.temp < 5 -> R.array.quips_cold
            w.temp < 15 -> R.array.quips_cool
            w.temp < 24 -> when {
                !w.isDay -> R.array.quips_night
                kind == Kind.CLEAR -> R.array.quips_clear
                else -> R.array.quips_pleasant
            }
            w.temp < 31 -> R.array.quips_warm
            else -> R.array.quips_heat
        }
    }

    /**
     * Alegerea nu e la intamplare de tot: se schimba singura din ora in ora, nu la fiecare
     * reimprospatare. Un text care sare la fiecare zece minute distrage; unul care nu se
     * schimba deloc devine invizibil in doua zile.
     *
     * Peste ora se aduna [SEED], pe care il muta doar butonul ↻ (vezi [shuffle]). Widget-ul
     * si ecranul aplicatiei citesc aceeasi valoare, deci arata acelasi comentariu.
     */
    fun forWeather(
        context: Context,
        w: Weather,
        nowMillis: Long = System.currentTimeMillis(),
    ): String {
        val list = context.resources.getStringArray(category(w))
        if (list.isEmpty()) return ""
        val hours = nowMillis / 3_600_000L
        return list[Math.floorMod(hours * 7 + seed(context), list.size.toLong()).toInt()]
    }

    /**
     * Alt comentariu, la cerere.
     *
     * Pasul e intre 1 si `size - 1`, deci niciodata un multiplu al lungimii listei: in
     * aceeasi categorie, apasarea butonului **garanteaza** alt text, nu doar il face probabil.
     */
    fun shuffle(context: Context, w: Weather) {
        val size = context.resources.getStringArray(category(w)).size
        if (size < 2) return
        val next = seed(context) + 1 + Random.nextInt(size - 1)
        prefs(context).edit().putLong(SEED, next).apply()
    }

    private fun seed(context: Context): Long = prefs(context).getLong(SEED, 0L)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
