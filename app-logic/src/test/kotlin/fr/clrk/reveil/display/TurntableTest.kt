package fr.clrk.reveil.display

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TurntableTest {
    @Test
    fun `the label turns smoothly from still to full speed`() {
        assertEquals(0f, Turntable.angle(2_000))
        val endOfSpeedUp = Turntable.angle(2_200 + 1_200)
        assertEquals(120f, endOfSpeedUp, 0.01f) // 1.2 s at half speed on average = 0.6 s at 200 °/s
        assertEquals((endOfSpeedUp + 200f) % 360f, Turntable.angle(2_200 + 1_200 + 1_000), 0.01f)
    }
}
