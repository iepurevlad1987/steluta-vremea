package ro.iepur.steluta

import android.app.WallpaperManager
import android.app.WallpaperColors
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

private const val TAG = "StelutaTheme"

/**
 * Cum arata widget-ul: fundalul si culoarea textului.
 *
 * Ordinea de aici e ordinea din lista din aplicatie (`theme_names` din strings.xml) - un
 * element nou se adauga **la sfarsit**, in ambele locuri, altfel cine avea o tema aleasa
 * s-ar trezi cu alta.
 */
enum class ThemeId { DARK, BLACK, LIGHT, WALLPAPER, GLASS, CLEAR }

/**
 * Culorile gata de pus pe widget.
 *
 * [onLight] = fundalul pe care se vede textul e deschis. De el depind culoarea textului si
 * varianta iconitelor: luna si zapada, desenate aproape albe, ar disparea pe un fundal
 * deschis, deci acolo se deseneaza mai inchise.
 */
data class Palette(
    /** Culoarea fundalului, opaca; transparenta e separat, in [bgAlpha]. */
    val bg: Int,
    /** 0 = fundal invizibil, 255 = opac. */
    val bgAlpha: Int,
    val ink: Int,
    val ink2: Int,
    val ink3: Int,
    val accent: Int,
    val line: Int,
    val onLight: Boolean,
)

object WidgetTheme {

    private const val PREFS = "steluta"
    private const val KEY = "widget_theme"

    // Textul pentru fundal inchis: aceleasi culori ca in colors.xml.
    private val LIGHT_INK = Ink(0xFFF4EFEA.toInt(), 0xFFB6A9A0.toInt(), 0xFF8C7F77.toInt(), 0xFFDA7756.toInt())

    // Textul pentru fundal deschis. Portocaliul e putin mai inchis: cel original, pe crem,
    // are prea putin contrast pentru un text de 12sp.
    private val DARK_INK = Ink(0xFF1F1A17.toInt(), 0xFF52473F.toInt(), 0xFF766960.toInt(), 0xFFB85A3A.toInt())

    private data class Ink(val ink: Int, val ink2: Int, val ink3: Int, val accent: Int)

    fun load(context: Context): ThemeId {
        val name = prefs(context).getString(KEY, null) ?: return ThemeId.DARK
        return runCatching { ThemeId.valueOf(name) }.getOrDefault(ThemeId.DARK)
    }

    fun save(context: Context, id: ThemeId) {
        prefs(context).edit().putString(KEY, id.name).apply()
    }

    /**
     * Culorile temei alese, cu wallpaper-ul de **acum**.
     *
     * Se calculeaza la fiecare desenare, nu se salveaza: daca schimbi wallpaper-ul, widget-ul
     * se potriveste la urmatoarea redesenare (cel mult jumatate de ora, sau imediat la ↻ ori
     * la deschiderea aplicatiei).
     */
    fun palette(context: Context): Palette = when (load(context)) {
        ThemeId.DARK -> make(0xFF191411.toInt(), 0xE6, onLight = false)
        ThemeId.BLACK -> make(Color.BLACK, 0xFF, onLight = false)
        ThemeId.LIGHT -> make(0xFFF7F1EA.toInt(), 0xF2, onLight = true)
        ThemeId.WALLPAPER -> {
            // Culoarea dominanta a wallpaper-ului, iar textul dupa cat de deschisa e ea.
            val c = wallpaperPrimary(context) ?: 0xFF191411.toInt()
            make(c or 0xFF000000.toInt(), 0xEB, onLight = Color.luminance(c) > 0.5f)
        }
        ThemeId.GLASS -> {
            // Un strat subtire peste wallpaper, alb pe fundal deschis si negru pe fundal
            // inchis: incadreaza widget-ul fara sa-l desprinda de ecran.
            val light = wallpaperIsLight(context)
            if (light) make(Color.WHITE, 0x73, onLight = true) else make(Color.BLACK, 0x66, onLight = false)
        }
        ThemeId.CLEAR -> make(Color.BLACK, 0x00, onLight = wallpaperIsLight(context))
    }

    private fun make(bg: Int, alpha: Int, onLight: Boolean): Palette {
        val i = if (onLight) DARK_INK else LIGHT_INK
        // Linia despartitoare: din culoarea textului, foarte stearsa - se vede pe orice fundal.
        val line = (i.ink and 0x00FFFFFF) or (0x33 shl 24)
        return Palette(bg, alpha, i.ink, i.ink2, i.ink3, i.accent, line, onLight)
    }

    /**
     * Daca wallpaper-ul e deschis la culoare, deci textul trebuie sa fie inchis.
     *
     * Pe Android 12+ intreaba direct sistemul (`HINT_SUPPORTS_DARK_TEXT`, acelasi semnal
     * dupa care isi coloreaza launcher-ul ceasul si iconitele). Mai vechi, judeca dupa cat
     * de deschisa e culoarea dominanta.
     */
    private fun wallpaperIsLight(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val colors = wallpaper(context) ?: return false
            return colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT != 0
        }
        val c = wallpaperPrimary(context) ?: return false
        return Color.luminance(c) > 0.5f
    }

    /** Culoarea dominanta a wallpaper-ului, opaca; `null` cand nu se poate afla. */
    private fun wallpaperPrimary(context: Context): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return null
        return wallpaper(context)?.primaryColor?.toArgb()
    }

    /**
     * Culorile wallpaper-ului, fara nicio permisiune.
     *
     * `null` pe Android 8.0 (API-ul a aparut in 8.1) si la unele wallpaper-uri animate, care
     * nu-si declara culorile - atunci temele care depind de el cad pe fundal inchis.
     */
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    private fun wallpaper(context: Context): WallpaperColors? {
        return try {
            WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        } catch (e: Exception) {
            Log.w(TAG, "Culorile wallpaper-ului nu se pot citi", e)
            null
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
