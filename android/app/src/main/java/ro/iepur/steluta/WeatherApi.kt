package ro.iepur.steluta

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.roundToInt

private const val TAG = "StelutaApi"

/**
 * Vremea, de la Open-Meteo.
 *
 * **Fara nicio biblioteca.** `HttpURLConnection` si `org.json` sunt in Android de la
 * inceput; Retrofit si Moshi ar fi adus doua dependinte si un procesor de adnotari
 * pentru un singur apel GET care intoarce un obiect.
 *
 * Open-Meteo nu cere cheie de API, deci aplicatia n-are niciun secret de pastrat.
 */
object WeatherApi {

    private const val URL_BASE = "https://api.open-meteo.com/v1/forecast"

    /**
     * Opt secunde.
     *
     * Nu e o cifra rotunda la intamplare: widget-ul cheama functia asta din `goAsync()`,
     * al carui buget e in jur de zece secunde. Un timeout mai lung ar insemna ca Android
     * taie procesul exact cand raspunsul era pe drum.
     */
    private const val TIMEOUT_MS = 8_000

    /**
     * Aduce vremea pentru [spot]. **Blocheaza** - se cheama doar de pe un fir de fundal.
     *
     * Intoarce `null` la orice esec, in loc sa arunce: cine cheama functia asta nu poate
     * face nimic diferit pentru „retea cazuta" fata de „JSON stricat", si in amandoua
     * cazurile arata ce stia dinainte.
     */
    fun fetch(spot: Spot): Weather? {
        val url = URL(
            "$URL_BASE?latitude=${spot.lat}&longitude=${spot.lon}" +
                "&current=temperature_2m,apparent_temperature,is_day,weather_code,wind_speed_10m" +
                "&hourly=temperature_2m,weather_code,is_day" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                // `forecast_hours` taie orele la cate ne trebuie, incepand cu ora curenta;
                // fara el ar veni sase zile intregi, adica 144 de ore din care folosim cinci.
                "&timezone=auto&forecast_days=6&forecast_hours=30"
        )

        var conn: HttpURLConnection? = null
        return try {
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }

            if (conn.responseCode !in 200..299) {
                Log.w(TAG, "Open-Meteo a răspuns ${conn.responseCode}")
                return null
            }

            parse(conn.inputStream.bufferedReader().use { it.readText() }, spot.name)
        } catch (e: Exception) {
            Log.w(TAG, "Apelul a eșuat", e)
            null
        } finally {
            conn?.disconnect()
        }
    }

    /** Despartita de [fetch] ca sa se poata testa pe JVM, fara retea. */
    fun parse(body: String, placeName: String): Weather? = try {
        val root = JSONObject(body)
        val cur = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")

        val times = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val maxs = daily.getJSONArray("temperature_2m_max")
        val mins = daily.getJSONArray("temperature_2m_min")

        val days = (0 until times.length()).map { i ->
            Day(
                iso = times.getString(i),
                code = codes.getInt(i),
                min = mins.getDouble(i).roundToInt(),
                max = maxs.getDouble(i).roundToInt(),
            )
        }

        Weather(
            place = placeName,
            hours = parseHours(root),
            temp = cur.getDouble("temperature_2m").roundToInt(),
            feels = cur.getDouble("apparent_temperature").roundToInt(),
            code = cur.getInt("weather_code"),
            wind = cur.getDouble("wind_speed_10m").roundToInt(),
            isDay = cur.getInt("is_day") == 1,
            min = days.firstOrNull()?.min ?: 0,
            max = days.firstOrNull()?.max ?: 0,
            days = days,
            at = System.currentTimeMillis(),
        )
    } catch (e: Exception) {
        Log.w(TAG, "Răspuns pe care nu-l pot citi", e)
        null
    }

    /**
     * Orele, din `hourly`.
     *
     * Open-Meteo le da ca ora locului (`2026-09-19T14:00`, fara fus), iar `utc_offset_seconds`
     * spune cat e de departe de UTC. Din amandoua iese momentul exact, care nu depinde de
     * fusul telefonului. Daca lipsesc - raspuns vechi, cache - lista e goala si randul de
     * jos ramane gol pana la urmatoarea reimprospatare, nu crapa.
     */
    private fun parseHours(root: JSONObject): List<Hour> = try {
        val hourly = root.getJSONObject("hourly")
        val offset = root.optLong("utc_offset_seconds", 0L)
        val times = hourly.getJSONArray("time")
        val temps = hourly.getJSONArray("temperature_2m")
        val codes = hourly.getJSONArray("weather_code")
        val days = hourly.getJSONArray("is_day")

        (0 until times.length()).map { i ->
            val iso = times.getString(i)
            Hour(
                epoch = (LocalDateTime.parse(iso).toEpochSecond(ZoneOffset.UTC) - offset) * 1000L,
                label = iso.substringAfter('T').take(5),
                code = codes.getInt(i),
                temp = temps.getDouble(i).roundToInt(),
                isDay = days.getInt(i) == 1,
            )
        }
    } catch (e: Exception) {
        Log.w(TAG, "Fără ore în răspuns", e)
        emptyList()
    }
}
