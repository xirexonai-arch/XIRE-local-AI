package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.XireBronze
import com.example.ui.theme.XireBronzeDeep
import com.example.ui.theme.XireBronzeLight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val TWO_PI = (2.0 * PI).toFloat()

private data class ProjectedDot(
    val x: Float,
    val y: Float,
    val z: Float,
    val perspectiveScale: Float
)

/**
 * Small 3-dot triangle thinking indicator moving in 3D perspective (pitch/yaw tumble,
 * not a flat 2D circle), rendered purely as 3 light-brown dots with no lines and no logo.
 */
@Composable
fun ThinkingTriangleDots(
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
    statusLabel: String = "Thinking"
) {
    val transition = rememberInfiniteTransition(label = "triangle_3d_thinking")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "triangle_phase"
    )

    val projectedDots = remember {
        Array(3) { ProjectedDot(0f, 0f, 0f, 1f) }
    }
    val drawOrder = remember { intArrayOf(0, 1, 2) }

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = statusLabel }
            .testTag("thinking_dots_animation")
    ) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val radius = this.size.minDimension * 0.34f
        val focalLength = radius * 3.1f
        val baseDotRadius = (this.size.minDimension * 0.115f).coerceIn(2.dp.toPx(), 3.6.dp.toPx())

        // 3D tumbling orientation: steep pitch + nutation + yaw precession so dots
        // move across 3D depth rather than tracing a flat 2D circle.
        val spinAngle = TWO_PI * phase
        val pitch = 0.92f + 0.34f * sin(TWO_PI * phase)       // ~33° to ~72° tilt into Z depth
        val yaw = 0.45f * cos(TWO_PI * phase)                 // gentle left/right 3D precession
        val roll = 0.28f * sin(TWO_PI * 2f * phase)           // harmonic Z tilt

        val cosPitch = cos(pitch)
        val sinPitch = sin(pitch)
        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val cosRoll = cos(roll)
        val sinRoll = sin(roll)

        for (i in 0 until 3) {
            val vertexAngle = spinAngle + (i * TWO_PI / 3f)
            // Subtle breathing separation along local triangle plane
            val localR = radius * (0.94f + 0.08f * sin(TWO_PI * 2f * phase + i * 2.094f))
            val lx = localR * cos(vertexAngle)
            val ly = localR * sin(vertexAngle)

            // Rotate around X (pitch into 3D depth)
            val y1 = ly * cosPitch
            val z1 = ly * sinPitch

            // Rotate around Y (3D yaw precession)
            val x2 = lx * cosYaw + z1 * sinYaw
            val z2 = -lx * sinYaw + z1 * cosYaw

            // Rotate around Z (gentle roll)
            val x3 = x2 * cosRoll - y1 * sinRoll
            val y3 = x2 * sinRoll + y1 * cosRoll

            val perspective = focalLength / (focalLength - z2).coerceAtLeast(focalLength * 0.45f)
            projectedDots[i] = ProjectedDot(
                x = cx + x3 * perspective,
                y = cy + y3 * perspective,
                z = z2,
                perspectiveScale = perspective
            )
        }

        // Sort back-to-front by Z so nearer dots cleanly overlap farther dots
        drawOrder[0] = 0
        drawOrder[1] = 1
        drawOrder[2] = 2
        for (a in 0 until 2) {
            for (b in (a + 1) until 3) {
                if (projectedDots[drawOrder[a]].z > projectedDots[drawOrder[b]].z) {
                    val tmp = drawOrder[a]
                    drawOrder[a] = drawOrder[b]
                    drawOrder[b] = tmp
                }
            }
        }

        val maxZ = radius.coerceAtLeast(1f)
        for (idx in 0 until 3) {
            val dot = projectedDots[drawOrder[idx]]
            val depthNorm = ((dot.z / maxZ) * 0.5f + 0.5f).coerceIn(0f, 1f)
            val dotColor = when {
                depthNorm < 0.5f -> lerp(XireBronzeDeep, XireBronze, depthNorm * 2f)
                else -> lerp(XireBronze, XireBronzeLight, (depthNorm - 0.5f) * 2f)
            }
            val dotAlpha = (0.62f + 0.38f * depthNorm).coerceIn(0.6f, 1f)
            val dotRadius = baseDotRadius * dot.perspectiveScale.coerceIn(0.68f, 1.32f)

            drawCircle(
                color = dotColor.copy(alpha = dotAlpha),
                radius = dotRadius,
                center = Offset(dot.x, dot.y)
            )
        }
    }
}
