package ro.iepur.steluta

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "StelutaStore"

/**
 * Ultima vreme stiuta, pastrata intre porniri.
 *
 * **De ce exista.** Un widget se redeseneaza si cand telefonul reporneste, si cand
 * launcher-ul e omorat de sistem, si la fiecare rotire a ecranului - momente in care nu
 * se poate astepta dupa retea. Fara memoria asta, widget-ul ar clipi in „se încarcă…"
 * de cateva ori pe zi, degeaba.
 *
 * Se scrie ca JSON in `SharedPreferences`, nu intr-o baza de date: e un singur obiect,
 * citit si scris intreg de fiecare data.
 */
object WeatherStore {

    private const val PREFS = "steluta"
    private const val KEY = "last_weather"

    fun save(context: Context, w: Weather) {
        val days = JSONArray()
        w.days.forEach { d ->
            days.put(
                JSONObject()
                    .put("iso", d.iso)
                    .put("code", d.code)
                    .put("min", d.min)
                    .put("max", d.max)
            )
        }

        val json = JSONObject()
            .put("place", w.place)
            .put("temp", w.temp)
            .put("feels", w.feels)
            .put("code", w.code)
            .put("wind", w.wind)
            .put("isDay", w.isDay)
            .put("min", w.min)
            .put("max", w.max)
            .put("at", w.at)
            .put("days", days)

        prefs(context).edit().putString(KEY, json.toString()).apply()
    }

    fun load(context: Context): Weather? {
        val raw = prefs(context).getString(KEY, null) ?: return null
        return try {
            val o = JSONObject(raw)
            val arr = o.getJSONArray("days")
            val days = (0 until arr.length()).map { i ->
                val d = arr.getJSONObject(i)
                Day(d.getString("iso"), d.getInt("code"), d.getInt("min"), d.getInt("max"))
            }
            Weather(
                place = o.getString("place"),
                temp = o.getInt("temp"),
                feels = o.getInt("feels"),
                code = o.getInt("code"),
                wind = o.getInt("wind"),
                isDay = o.getBoolean("isDay"),
                min = o.getInt("min"),
                max = o.getInt("max"),
                days = days,
                at = o.getLong("at"),
            )
        } catch (e: Exception) {
            // Un rand stricat nu are voie sa impiedice pornirea: se uita si se ia de la capat.
            Log.w(TAG, "Ce era salvat nu se poate citi; se ignoră", e)
            null
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
