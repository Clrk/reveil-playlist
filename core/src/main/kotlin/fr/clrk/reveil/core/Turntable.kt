package fr.clrk.reveil.core

/** The record starts once the arm has landed, speeds up to 33⅓ rpm, then keeps turning. */
object Turntable {
    private const val RPM = 100f / 3
    private const val SPIN_FROM_MS = Tonearm.START_MS + 200
    private const val SPEED_UP_MS = 1200f
    private const val MS_PER_MINUTE = 60_000f
    private const val TURN = 360f

    /** Share of full speed, from 0 (still) to 1. */
    fun speed(elapsedMs: Long): Float = ((elapsedMs - SPIN_FROM_MS) / SPEED_UP_MS).coerceIn(0f, 1f)

    fun rpm(elapsedMs: Long): Float = RPM * speed(elapsedMs)

    /** Record rotation in degrees: integral of the speed, so the label never jumps. */
    fun angle(elapsedMs: Long): Float {
        val running = (elapsedMs - SPIN_FROM_MS).coerceAtLeast(0).toFloat()
        val fullSpeedMs = if (running < SPEED_UP_MS) running * running / (2 * SPEED_UP_MS) else running - SPEED_UP_MS / 2
        return fullSpeedMs * RPM * TURN / MS_PER_MINUTE % TURN
    }
}
