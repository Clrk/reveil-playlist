package fr.clrk.reveil.weather

import android.content.Context
import androidx.core.content.edit
import fr.clrk.reveil.alarm.FileTextStore
import fr.clrk.reveil.core.DayWeather
import fr.clrk.reveil.core.Geocoding
import fr.clrk.reveil.core.OpenMeteo
import fr.clrk.reveil.core.Place
import fr.clrk.reveil.core.PlaceStore
import fr.clrk.reveil.music.NewPipeDownloader
import fr.clrk.reveil.text
import okhttp3.OkHttpClient
import java.io.File
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Keeps the last Open-Meteo forecast of the chosen city, fetched the evening before and refreshed when the alarm rings. */
class WeatherRepository(context: Context) {
    private val prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE)
    val places = PlaceStore(FileTextStore(File(context.noBackupFilesDir, "place.json")))

    /** Blocking: call from a background thread. A failure keeps the previous forecast; no city, no forecast. */
    fun refresh() {
        val url = places.forecastUrl() ?: return prefs.edit { remove(KEY_BODY) }
        get(url)?.let { prefs.edit { putString(KEY_BODY, it) } }
    }

    fun day(date: LocalDate = LocalDate.now()): DayWeather? =
        prefs.getString(KEY_BODY, null)?.let { DayWeather.of(OpenMeteo.hours(it, date)) }

    /** Blocking: cities matching what the user typed, empty when offline. */
    fun search(query: String): List<Place> =
        if (query.isBlank()) emptyList() else get(Geocoding.url(query))?.let(Geocoding::places).orEmpty()

    /** Tomorrow morning's sky, for the alarm list. */
    fun tomorrow() = day(LocalDate.now().plusDays(1))?.morning

    /** Blocking: remembers the city and fetches its forecast. */
    fun choose(place: Place) {
        places.choose(place)
        prefs.edit { remove(KEY_BODY) }
        refresh()
    }

    private fun get(url: String): String? = http.text(url)

    private companion object {
        const val KEY_BODY = "openMeteo"
        const val TIMEOUT_S = 8L
        val http: OkHttpClient = NewPipeDownloader.instance.client.newBuilder().callTimeout(TIMEOUT_S, TimeUnit.SECONDS).build()
    }
}
