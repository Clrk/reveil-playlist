package fr.clrk.reveil.core.steps

import fr.clrk.reveil.core.Alarm
import fr.clrk.reveil.core.VolumeRamp
import fr.clrk.reveil.core.bdd.FeatureRunner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.TestFactory
import java.time.Duration
import java.time.LocalTime
import kotlin.math.roundToInt

class VolumeWorld {
    var alarm = Alarm.at(LocalTime.of(7, 0))

    fun percentAfter(seconds: Long) = (VolumeRamp(alarm.rampSeconds).at(Duration.ofSeconds(seconds)) * 100).roundToInt()
}

class VolumeTest {
    @TestFactory
    fun scenarios() = FeatureRunner.run("features/volume.feature", ::VolumeWorld) {
        step("une alarme aux réglages par défaut") { }
        step("une montée du volume sur \"(\\d+)\" secondes") { (seconds) -> alarm = alarm.copy(rampSeconds = seconds.toInt()) }
        step("le volume monte sur \"(\\d+)\" secondes") { (seconds) -> assertEquals(seconds.toInt(), alarm.rampSeconds) }
        step("au début le volume est à \"(\\d+)\" %") { (percent) -> assertEquals(percent.toInt(), percentAfter(0)) }
        step("après \"(\\d+)\" secondes le volume est à \"(\\d+)\" %") { (seconds, percent) ->
            assertEquals(percent.toInt(), percentAfter(seconds.toLong()))
        }
    }
}
