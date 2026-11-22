package fr.clrk.reveil.display

import kotlin.math.roundToInt

/** Sky shown on the ring screen, ordered from mildest to most striking (ties go to the most striking). */
enum class Sky { CLEAR, CLOUDY, FROST, RAIN, SNOW }

/** One forecast hour: local [hour] (0–23), WMO weather [code], [temperature] in °C. */
data class HourWeather(val hour: Int, val code: Int, val temperature: Double)

/** A half day as displayed: one sky and one temperature. */
data class HalfDay(val sky: Sky, val temperature: Int)

/** Morning (6–11 h, lowest temperature) and afternoon (12–17 h, highest temperature) of the wake-up day. */
data class DayWeather(val morning: HalfDay, val afternoon: HalfDay) {
    companion object {
        private val MORNING = 6..11
        private val AFTERNOON = 12..17

        fun of(hours: List<HourWeather>): DayWeather? {
            val morning = hours.filter { it.hour in MORNING }
            val afternoon = hours.filter { it.hour in AFTERNOON }
            if (morning.isEmpty() || afternoon.isEmpty()) return null
            return DayWeather(halfDay(morning, morning.minOf { it.temperature }), halfDay(afternoon, afternoon.maxOf { it.temperature }))
        }

        /** The ring scene follows the morning; without forecast it stays sunny. */
        fun sceneOf(day: DayWeather?): Sky = if (day == null) Sky.CLEAR else day.morning.sky

        private fun halfDay(hours: List<HourWeather>, temperature: Double): HalfDay {
            val counts = hours.groupingBy { skyOf(it) }.eachCount()
            val sky = counts.entries.maxWith(compareBy<Map.Entry<Sky, Int>> { it.value }.thenBy { it.key.ordinal }).key
            return HalfDay(sky, temperature.roundToInt())
        }

        private fun skyOf(hour: HourWeather): Sky {
            val sky = WmoCode.sky(hour.code)
            val freezing = hour.temperature <= 0.0 && sky == Sky.CLEAR
            return if (freezing) Sky.FROST else sky
        }
    }
}

/** WMO weather interpretation codes, as returned by Open-Meteo. */
object WmoCode {
    private val SNOW = setOf(71, 73, 75, 77, 85, 86)
    private val RAIN = (51..67).toSet() + (80..82) + (95..99)
    private val CLEAR = setOf(0, 1)

    fun sky(code: Int): Sky = when (code) {
        in SNOW -> Sky.SNOW
        in RAIN -> Sky.RAIN
        in CLEAR -> Sky.CLEAR
        else -> Sky.CLOUDY
    }
}
