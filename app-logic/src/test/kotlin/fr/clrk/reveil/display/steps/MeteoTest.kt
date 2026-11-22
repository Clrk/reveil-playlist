package fr.clrk.reveil.display.steps

import fr.clrk.reveil.display.DayWeather
import fr.clrk.reveil.display.HourWeather
import fr.clrk.reveil.display.Sky
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.TestFactory

class MeteoWorld {
    val hours = mutableListOf<HourWeather>()
    val day get() = DayWeather.of(hours)

    fun add(from: String, to: String, code: Int, temperature: String) {
        (from.toInt()..to.toInt()).forEach { hours += HourWeather(it, code, temperature.toDouble()) }
    }
}

class MeteoTest {
    private val skies = mapOf("soleil" to Sky.CLEAR, "nuages" to Sky.CLOUDY, "pluie" to Sky.RAIN, "neige" to Sky.SNOW, "gel" to Sky.FROST)
    private val range = "de \"(\\d+)\" h à \"(\\d+)\" h"
    private val temp = "à \"(-?\\d+)\" °C"
    private val shows = "affiche \"(\\w+)\" et \"(-?\\d+)\" °C"

    @TestFactory
    fun scenarios() = FeatureRunner.run("features/meteo.feature", ::MeteoWorld) {
        step("$range il pleut $temp") { (a, b, t) -> add(a, b, RAIN_CODE, t) }
        step("$range il neige $temp") { (a, b, t) -> add(a, b, SNOW_CODE, t) }
        step("$range c'est couvert $temp") { (a, b, t) -> add(a, b, OVERCAST_CODE, t) }
        step("$range le ciel est dégagé $temp") { (a, b, t) -> add(a, b, CLEAR_CODE, t) }
        step("aucune prévision") { hours.clear() }
        step("le matin $shows") { (sky, t) -> assertEquals(skies[sky] to t.toInt(), day!!.morning.let { it.sky to it.temperature }) }
        step("l'après-midi $shows") { (sky, t) -> assertEquals(skies[sky] to t.toInt(), day!!.afternoon.let { it.sky to it.temperature }) }
        step("la scène du réveil est \"(\\w+)\"") { (sky) -> assertEquals(skies[sky], DayWeather.sceneOf(day)) }
        step("aucune météo n'est affichée") { assertNull(day) }
    }

    private companion object {
        const val RAIN_CODE = 61
        const val SNOW_CODE = 71
        const val OVERCAST_CODE = 3
        const val CLEAR_CODE = 0
    }
}
