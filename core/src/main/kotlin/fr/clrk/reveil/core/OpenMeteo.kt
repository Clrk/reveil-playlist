package fr.clrk.reveil.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime

/** Open-Meteo hourly forecast (free, no key): URL to call and how to read it. */
object OpenMeteo {
    private val json = Json { ignoreUnknownKeys = true }

    fun url(place: Place) = "https://api.open-meteo.com/v1/forecast?latitude=${place.latitude}&longitude=${place.longitude}" +
        "&hourly=weather_code,temperature_2m&timezone=auto&forecast_days=3"

    /** Hours of [date] found in an Open-Meteo answer; empty when the answer is unreadable. */
    fun hours(body: String, date: LocalDate): List<HourWeather> {
        val hourly = runCatching { json.decodeFromString<Answer>(body).hourly }.getOrNull() ?: return emptyList()
        return hourly.time.indices.mapNotNull { i -> hour(hourly, i, date) }
    }

    private fun hour(hourly: Hourly, i: Int, date: LocalDate): HourWeather? {
        val time = LocalDateTime.parse(hourly.time[i])
        val code = hourly.code.getOrNull(i)
        val temperature = hourly.temperature.getOrNull(i)
        val usable = time.toLocalDate() == date && code != null && temperature != null
        return if (usable) HourWeather(time.hour, code!!, temperature!!) else null
    }

    @Serializable
    private data class Answer(val hourly: Hourly)

    @Serializable
    private data class Hourly(
        val time: List<String>,
        @SerialName("weather_code") val code: List<Int?>,
        @SerialName("temperature_2m") val temperature: List<Double?>,
    )
}
