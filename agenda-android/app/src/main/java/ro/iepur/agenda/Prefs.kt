package ro.iepur.agenda

import android.content.Context

/** Setarile din aplicatie, citite de widget la fiecare redesenare. */
object Prefs {
    private const val NAME = "agenda"
    const val OPACITY = "opacity"   // 0..100, cat de opac e fundalul
    const val LIGHT = "light"       // false = fundal inchis, true = fundal deschis
    const val EDGE = "edge"         // conturul fin de pe margine
    const val DAYS = "days"         // cate zile inainte

    fun of(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun opacity(c: Context) = of(c).getInt(OPACITY, 40)
    fun light(c: Context) = of(c).getBoolean(LIGHT, false)
    fun edge(c: Context) = of(c).getBoolean(EDGE, true)
    fun days(c: Context) = of(c).getInt(DAYS, 14)
}
