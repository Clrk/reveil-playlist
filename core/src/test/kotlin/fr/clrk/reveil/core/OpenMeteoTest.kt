package fr.clrk.reveil.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpenMeteoTest {
    private val body = """
        {"latitude":45.12,"hourly":{
          "time":["2026-11-20T23:00","2026-11-21T07:00","2026-11-21T08:00","2026-11-21T09:00"],
          "weather_code":[0,61,null,3],
          "temperature_2m":[1.5,8.2,8.0,null]}}
    """.trimIndent()

    @Test
    fun `keeps only complete hours of the wake-up day`() {
        assertEquals(listOf(HourWeather(7, 61, 8.2)), OpenMeteo.hours(body, LocalDate.of(2026, 11, 21)))
    }

    @Test
    fun `an unreadable answer gives no hours`() {
        assertTrue(OpenMeteo.hours("<html>", LocalDate.of(2026, 11, 21)).isEmpty())
    }

    @Test
    fun `asks the hourly code and temperature of the place`() {
        val url = OpenMeteo.url(Place("Claix", 45.1206, 5.6728))
        assertTrue("latitude=45.1206&longitude=5.6728" in url && "weather_code,temperature_2m" in url, url)
    }
}
