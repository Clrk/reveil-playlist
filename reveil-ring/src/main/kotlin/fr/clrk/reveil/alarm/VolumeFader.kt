package fr.clrk.reveil.alarm

import androidx.media3.common.Player
import fr.clrk.reveil.core.VolumeRamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration

private const val STEP_MS = 250L

/** Raises the player volume step by step; the ringtone fallback is played by the same player, so it ramps too. */
fun CoroutineScope.rampVolume(player: Player, ramp: VolumeRamp) = launch {
    val start = System.currentTimeMillis()
    do {
        player.volume = ramp.at(Duration.ofMillis(System.currentTimeMillis() - start))
        delay(STEP_MS)
    } while (player.volume < 1f)
}
