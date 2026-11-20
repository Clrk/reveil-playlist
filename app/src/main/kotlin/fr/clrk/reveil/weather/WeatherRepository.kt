package fr.clrk.reveil.weather

import android.content.Context
import androidx.core.content.edit
import fr.clrk.reveil.core.DayWeather
import fr.clrk.reveil.core.OpenMeteo
import fr.clrk.reveil.core.Place
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Keeps the last Open-Meteo forecast, fetched the evening before and refreshed when the alarm rings. */
class WeatherRepository(context: Context) {
    private val prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE)

    /** Blocking: call from a background thread. A failure keeps the previous forecast. */
    fun refresh(place: Place = Place.CLAIX) {
        val request = Request.Builder().url(OpenMeteo.url(place)).build()
        val body = runCatching { http.newCall(request).execute().use { if (it.isSuccessful) it.body.string() else null } }
        body.getOrNull()?.let { prefs.edit { putString(KEY_BODY, it) } }
    }

    fun day(date: LocalDate = LocalDate.now()): DayWeather? =
        prefs.getString(KEY_BODY, null)?.let { DayWeather.of(OpenMeteo.hours(it, date)) }

    private companion object {
        const val KEY_BODY = "openMeteo"
        const val TIMEOUT_S = 8L
        val http: OkHttpClient = OkHttpClient.Builder().callTimeout(TIMEOUT_S, TimeUnit.SECONDS).build()
    }
}
