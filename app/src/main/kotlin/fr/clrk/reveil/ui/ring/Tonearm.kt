@file:Suppress("MagicNumber") // geometry measured on the plate pictures

package fr.clrk.reveil.ui.ring

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import fr.clrk.reveil.core.ArmPose
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tonearm drawn in the platter plane (x right, y toward the viewer, squashed by [DEPTH]; h = height),
 * so lifting and swinging stay in perspective with the photographed turntable.
 */
internal fun DrawScope.tonearm(pose: ArmPose, light: Float) {
    val angle = Math.toRadians((PLAY + (REST - PLAY) * pose.swing).toDouble())
    val dir = Offset(cos(angle).toFloat(), sin(angle).toFloat())
    val lift = LIFT * pose.lift + pose.bob
    // The arm is a lever hinged on its post: the head rises, the counterweight dips, the post stays put.
    val pivotTop = project(PIVOT, POST)
    val head = project(PIVOT + dir * (LENGTH - HEAD), HEAD_H + lift + 2f)
    val weight = project(PIVOT - dir * WEIGHT, POST + 1f - lift * WEIGHT / LENGTH)
    val dark = lerp(Color(0xFF0A0C0F), Color(0xFF1E1B19), light)
    val shine = lerp(Color(0xFF2C323A), Color(0xFFFAECD6), light)
    shadow(dir, lift, light)
    drawLine(dark, project(PIVOT, 0f), pivotTop, strokeWidth = 7f, cap = StrokeCap.Round)
    drawCircle(dark, 8f, weight)
    drawLine(dark, weight, pivotTop, strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(dark, pivotTop, head, strokeWidth = 5f, cap = StrokeCap.Round)
    drawLine(shine.copy(alpha = 0.75f), pivotTop - Offset(0f, 1.8f), head - Offset(0f, 1.8f), strokeWidth = 1.2f, cap = StrokeCap.Round)
    headshell(angle, dir, lift, dark, shine)
    drawOval(dark, topLeft = pivotTop - Offset(4.5f, 3f), size = androidx.compose.ui.geometry.Size(9f, 6f))
}

private fun DrawScope.headshell(angle: Double, dir: Offset, lift: Float, dark: Color, shine: Color) {
    val offsetAngle = angle + HEAD_OFFSET
    val headDir = Offset(cos(offsetAngle).toFloat(), sin(offsetAngle).toFloat())
    val base = PIVOT + dir * (LENGTH - HEAD)
    val from = project(base, HEAD_H + lift + 2f)
    val to = project(base + headDir * HEAD, HEAD_H + lift)
    drawLine(dark, from, to, strokeWidth = 8f)
    drawLine(shine.copy(alpha = 0.45f), from - Offset(0f, 2.5f), to - Offset(0f, 2.5f), strokeWidth = 1f)
}

/** Soft shadow on the deck, pushed away from the window (upper left) more as the arm rises. */
private fun DrawScope.shadow(dir: Offset, lift: Float, light: Float) {
    val drift = Offset(5f + lift * 0.5f, 3f + lift * 0.35f)
    val from = project(PIVOT, 0f) + drift * 0.3f
    val to = project(PIVOT + dir * LENGTH, 0f) + drift
    drawLine(Color.Black.copy(alpha = 0.15f + 0.3f * light), from, to, strokeWidth = 6f, cap = StrokeCap.Round)
}

private fun project(p: Offset, h: Float) = Offset(CENTER.x + p.x, CENTER.y + p.y * DEPTH - h)

private val CENTER = Offset(305f, 668f)
private val PIVOT = Offset(147f, 37f)
private const val DEPTH = 0.59f
private const val LENGTH = 140f
private const val HEAD = 18f
private const val WEIGHT = 26f
private const val POST = 11f
private const val HEAD_H = 6f
private const val LIFT = 12f
private const val PLAY = 170.8f
private const val REST = 112f
private const val HEAD_OFFSET = 0.35
