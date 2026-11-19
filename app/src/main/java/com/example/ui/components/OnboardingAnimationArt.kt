package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.XireBronze
import com.example.ui.theme.XireBronzeDeep
import com.example.ui.theme.XireBronzeHighlight
import com.example.ui.theme.XireTerracotta
import com.example.ui.theme.XireTerracottaLight
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val TWO_PI = (2.0 * PI).toFloat()

private data class Neuron3DSpec(
    val baseX: Float,
    val baseY: Float,
    val baseZ: Float,
    val orbitRadius: Float,
    val phase: Float,
    val sizeRatio: Float,
    val speedMult: Int
)

private data class SynapseEdgeSpec(
    val fromIdx: Int,
    val toIdx: Int,
    val curveLift: Float,
    val pulseSpeed: Int,
    val pulseOffset: Float
)

private data class ProjectedNeuron(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var scale: Float = 1f,
    var flare: Float = 0f
)

private val ThoughtNeuronSpecs = listOf(
    Neuron3DSpec(0.53f, 0.24f, 0.05f, 0.017f, 0.0f, 0.032f, 1), // 0: Central mind hub
    Neuron3DSpec(0.36f, 0.17f, -0.04f, 0.019f, 1.0f, 0.025f, 1), // 1: Above brow
    Neuron3DSpec(0.70f, 0.16f, 0.06f, 0.021f, 2.1f, 0.029f, 1), // 2: Upper right
    Neuron3DSpec(0.84f, 0.30f, -0.05f, 0.018f, 3.2f, 0.027f, 1), // 3: Far right
    Neuron3DSpec(0.69f, 0.38f, 0.07f, 0.019f, 4.3f, 0.026f, 1), // 4: Forward gaze
    Neuron3DSpec(0.24f, 0.23f, 0.03f, 0.016f, 5.1f, 0.022f, 1), // 5: Above crown
    Neuron3DSpec(0.53f, 0.09f, -0.06f, 0.018f, 0.6f, 0.022f, 2)  // 6: Apex thought
)

private val ThoughtSynapseEdges = listOf(
    SynapseEdgeSpec(0, 1, -0.04f, 2, 0.00f),
    SynapseEdgeSpec(0, 2, -0.05f, 2, 0.32f),
    SynapseEdgeSpec(0, 4, 0.04f, 3, 0.14f),
    SynapseEdgeSpec(0, 6, -0.03f, 2, 0.64f),
    SynapseEdgeSpec(1, 5, -0.03f, 2, 0.48f),
    SynapseEdgeSpec(1, 6, -0.04f, 3, 0.78f),
    SynapseEdgeSpec(2, 3, 0.04f, 2, 0.24f),
    SynapseEdgeSpec(2, 6, -0.03f, 2, 0.88f),
    SynapseEdgeSpec(3, 4, 0.04f, 3, 0.52f),
    SynapseEdgeSpec(4, 2, -0.04f, 2, 0.70f)
)

/**
 * Original XIRE AI onboarding illustration:
 * A person in calm contemplation with a raised, actively moving hand near their chin,
 * emitting thinking waves and 3D orbiting neurons with live firing synapses.
 */
@Composable
fun OnboardingAnimationArt(
    modifier: Modifier = Modifier,
    size: Dp = 216.dp
) {
    val lineColor = MaterialTheme.colorScheme.onBackground
    val bgColor = MaterialTheme.colorScheme.background

    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "person_thinking_neurons")
    val cycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinking_cycle"
    )

    val projectedNeurons = remember { Array(ThoughtNeuronSpecs.size) { ProjectedNeuron() } }
    val drawOrder = remember { IntArray(ThoughtNeuronSpecs.size) { it } }
    val axonPath = remember { Path() }
    val axonMeasure = remember { PathMeasure() }
    val headProfilePath = remember { Path() }
    val shoulderPath = remember { Path() }
    val eyePath = remember { Path() }
    val handOutlinePath = remember { Path() }
    val handFillPath = remember { Path() }
    val handMeasure = remember { PathMeasure() }
    val revealedHandPath = remember { Path() }
    val fingerTips = remember { Array(5) { Offset.Zero } }

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = "Animated person thinking with moving hand and active neurons"
            }
            .testTag("onboarding_animation_art")
    ) {
        val w = this.size.width
        val h = this.size.height
        val minDim = this.size.minDimension
        val theta = TWO_PI * cycle
        val intro = entrance.value

        // Gentle contemplative breathing & slight head tilt
        val headBreathY = sin(theta) * (minDim * 0.008f)
        val headPivot = Offset(w * 0.30f, h * 0.62f)
        val headTilt = 0.032f * sin(theta + 0.4f)
        val cosH = cos(headTilt)
        val sinH = sin(headTilt)

        fun headPoint(nx: Float, ny: Float): Offset {
            val dx = w * nx - headPivot.x
            val dy = h * ny - headPivot.y
            val rx = dx * cosH - dy * sinH
            val ry = dx * sinH + dy * cosH
            return Offset(headPivot.x + rx, headPivot.y + ry + headBreathY)
        }

        // 1. Build Person Thinking Silhouette (facing right toward the neural constellation)
        val napeEnd = headPoint(0.20f, 0.74f)
        val backNeck = headPoint(0.18f, 0.66f)
        val backHead = headPoint(0.16f, 0.53f)
        val crownTop = headPoint(0.29f, 0.34f)
        val forehead = headPoint(0.415f, 0.385f)
        val browBridge = headPoint(0.425f, 0.445f)
        val noseTip = headPoint(0.472f, 0.502f)
        val underNose = headPoint(0.442f, 0.524f)
        val upperLip = headPoint(0.452f, 0.546f)
        val lowerLip = headPoint(0.444f, 0.566f)
        val chinPoint = headPoint(0.442f, 0.602f)
        val jawCorner = headPoint(0.325f, 0.635f)
        val frontNeck = headPoint(0.345f, 0.74f)

        headProfilePath.reset()
        headProfilePath.moveTo(napeEnd.x, napeEnd.y)
        headProfilePath.cubicTo(
            backNeck.x, backNeck.y,
            backHead.x, backHead.y,
            headPoint(0.19f, 0.41f).x, headPoint(0.19f, 0.41f).y
        )
        headProfilePath.cubicTo(
            headPoint(0.22f, 0.35f).x, headPoint(0.22f, 0.35f).y,
            crownTop.x, crownTop.y,
            forehead.x, forehead.y
        )
        headProfilePath.quadraticTo(
            headPoint(0.432f, 0.415f).x, headPoint(0.432f, 0.415f).y,
            browBridge.x, browBridge.y
        )
        headProfilePath.quadraticTo(
            headPoint(0.460f, 0.475f).x, headPoint(0.460f, 0.475f).y,
            noseTip.x, noseTip.y
        )
        headProfilePath.quadraticTo(
            headPoint(0.470f, 0.518f).x, headPoint(0.470f, 0.518f).y,
            underNose.x, underNose.y
        )
        headProfilePath.quadraticTo(
            upperLip.x + minDim * 0.004f, upperLip.y,
            headPoint(0.438f, 0.556f).x, headPoint(0.438f, 0.556f).y
        )
        headProfilePath.quadraticTo(
            lowerLip.x + minDim * 0.002f, lowerLip.y,
            chinPoint.x, chinPoint.y
        )
        headProfilePath.cubicTo(
            headPoint(0.435f, 0.628f).x, headPoint(0.435f, 0.628f).y,
            headPoint(0.370f, 0.638f).x, headPoint(0.370f, 0.638f).y,
            jawCorner.x, jawCorner.y
        )

        // Shoulders & collar line
        shoulderPath.reset()
        shoulderPath.moveTo(w * 0.07f, h * 0.90f + headBreathY * 0.4f)
        shoulderPath.cubicTo(
            w * 0.11f, h * 0.80f + headBreathY * 0.5f,
            w * 0.16f, h * 0.76f + headBreathY * 0.7f,
            napeEnd.x, napeEnd.y
        )
        shoulderPath.moveTo(jawCorner.x, jawCorner.y)
        shoulderPath.quadraticTo(
            headPoint(0.335f, 0.69f).x, headPoint(0.335f, 0.69f).y,
            frontNeck.x, frontNeck.y
        )
        shoulderPath.cubicTo(
            w * 0.37f, h * 0.79f + headBreathY * 0.5f,
            w * 0.44f, h * 0.83f + headBreathY * 0.4f,
            w * 0.50f, h * 0.90f + headBreathY * 0.3f
        )

        // Calm thoughtful eye & brow mark
        val eyeL = headPoint(0.372f, 0.465f)
        val eyeMid = headPoint(0.394f, 0.472f)
        val eyeR = headPoint(0.415f, 0.464f)
        val browL = headPoint(0.370f, 0.445f)
        val browMid = headPoint(0.394f, 0.438f)
        val browR = headPoint(0.417f, 0.443f)
        eyePath.reset()
        eyePath.moveTo(eyeL.x, eyeL.y)
        eyePath.quadraticTo(eyeMid.x, eyeMid.y, eyeR.x, eyeR.y)
        eyePath.moveTo(browL.x, browL.y)
        eyePath.quadraticTo(browMid.x, browMid.y, browR.x, browR.y)

        // 2. Thinking Waves & Thought Bubbles rising from the crown/brow into the mind space
        val thoughtSource = headPoint(0.355f, 0.355f)
        for (ringIdx in 0 until 3) {
            val wavePhase = (cycle * 2f + ringIdx / 3f) % 1f
            val waveAlpha = sin(PI.toFloat() * wavePhase).coerceIn(0f, 1f) * 0.42f * intro
            val arcRadius = minDim * (0.045f + 0.115f * wavePhase)
            drawArc(
                color = XireTerracotta.copy(alpha = waveAlpha),
                startAngle = -115f,
                sweepAngle = 85f,
                useCenter = false,
                topLeft = Offset(
                    thoughtSource.x - arcRadius * 0.65f,
                    thoughtSource.y - arcRadius * 1.15f
                ),
                size = Size(arcRadius * 2f, arcRadius * 2f),
                style = Stroke(width = minDim * 0.0085f, cap = StrokeCap.Round)
            )
        }

        // Small rising thought bubbles drifting from forehead/temple toward the neurons
        for (b in 0 until 3) {
            val bt = (cycle * 2f + b * 0.33f) % 1f
            val bx = thoughtSource.x + minDim * (0.015f + 0.06f * bt + 0.01f * sin(theta * 2f + b))
            val by = thoughtSource.y - minDim * (0.02f + 0.10f * bt)
            val br = minDim * (0.008f + 0.007f * sin(PI.toFloat() * bt))
            val bAlpha = sin(PI.toFloat() * bt).coerceIn(0f, 1f) * 0.72f * intro
            drawCircle(
                color = XireBronze.copy(alpha = bAlpha),
                radius = br,
                center = Offset(bx, by)
            )
        }

        // 3. 3D Orbiting Neurons & Firing Synapses above/in front of the thinker
        val focal = minDim * 2.2f
        val constellationCenter = Offset(w * 0.56f, h * 0.24f)
        val globalRot = 0.22f * sin(theta)

        for (i in ThoughtNeuronSpecs.indices) {
            val spec = ThoughtNeuronSpecs[i]
            val orbAngle = theta * spec.speedMult + spec.phase
            val ox = cos(orbAngle) * spec.orbitRadius * minDim
            val oy = sin(orbAngle) * spec.orbitRadius * 0.75f * minDim
            val oz = sin(orbAngle + spec.phase) * spec.orbitRadius * 1.35f * minDim

            val rawX = w * spec.baseX + ox - constellationCenter.x
            val rawZ = spec.baseZ * minDim + oz
            val rotX = rawX * cos(globalRot) - rawZ * sin(globalRot)
            val rotZ = rawX * sin(globalRot) + rawZ * cos(globalRot)

            val worldX = constellationCenter.x + rotX
            val worldY = h * spec.baseY + oy
            val scale = (focal / (focal - rotZ).coerceAtLeast(focal * 0.45f)).coerceIn(0.75f, 1.30f)

            projectedNeurons[i].apply {
                x = constellationCenter.x + (worldX - constellationCenter.x) * intro
                y = constellationCenter.y + (worldY - constellationCenter.y) * intro
                z = rotZ
                this.scale = scale
                flare = 0f
            }
        }

        // Synaptic impulses ascending from the person's mind/crown into the neural network
        val mindToNeuronTargets = listOf(5, 1, 0, 4)
        for ((idx, targetNeuronIdx) in mindToNeuronTargets.withIndex()) {
            val target = projectedNeurons[targetNeuronIdx]
            val startPt = if (idx < 2) headPoint(0.32f, 0.36f) else headPoint(0.40f, 0.39f)
            val ctrl = Offset(
                (startPt.x + target.x) * 0.5f - minDim * 0.025f,
                (startPt.y + target.y) * 0.5f - minDim * 0.03f
            )
            axonPath.reset()
            axonPath.moveTo(startPt.x, startPt.y)
            axonPath.quadraticTo(ctrl.x, ctrl.y, target.x, target.y)

            drawPath(
                path = axonPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.06f * intro),
                        XireBronze.copy(alpha = 0.38f * intro),
                        XireTerracotta.copy(alpha = 0.55f * intro)
                    ),
                    start = startPt,
                    end = Offset(target.x, target.y)
                ),
                style = Stroke(width = minDim * 0.0075f, cap = StrokeCap.Round)
            )

            axonMeasure.setPath(axonPath, false)
            val pathLen = axonMeasure.length
            val t = (cycle * 2f + idx * 0.25f) % 1f
            val sparkPos = axonMeasure.getPosition(pathLen * t)
            if (sparkPos != Offset.Unspecified) {
                val sparkAlpha = sin(PI.toFloat() * t).coerceIn(0f, 1f) * intro
                drawCircle(
                    color = XireTerracottaLight.copy(alpha = 0.36f * sparkAlpha),
                    radius = minDim * 0.020f,
                    center = sparkPos
                )
                drawCircle(
                    color = XireBronzeHighlight.copy(alpha = 0.92f * sparkAlpha),
                    radius = minDim * 0.008f,
                    center = sparkPos
                )
                if (t > 0.85f) {
                    target.flare = (target.flare + (t - 0.85f) / 0.15f).coerceAtMost(1f)
                }
            }
        }

        // Curved axons & traveling sparks between neurons
        for (edge in ThoughtSynapseEdges) {
            val a = projectedNeurons[edge.fromIdx]
            val b = projectedNeurons[edge.toIdx]
            val ctrl = Offset(
                (a.x + b.x) * 0.5f,
                (a.y + b.y) * 0.5f + edge.curveLift * minDim
            )
            axonPath.reset()
            axonPath.moveTo(a.x, a.y)
            axonPath.quadraticTo(ctrl.x, ctrl.y, b.x, b.y)

            val avgDepth = ((a.z + b.z) * 0.5f / (minDim * 0.12f)).coerceIn(-1f, 1f)
            val axonAlpha = (0.34f + 0.28f * (avgDepth * 0.5f + 0.5f)) * intro
            drawPath(
                path = axonPath,
                color = XireTerracotta.copy(alpha = axonAlpha),
                style = Stroke(width = minDim * 0.010f, cap = StrokeCap.Round)
            )

            axonMeasure.setPath(axonPath, false)
            val len = axonMeasure.length
            val pulseT = (cycle * edge.pulseSpeed + edge.pulseOffset) % 1f
            val pulsePos = axonMeasure.getPosition(len * pulseT)
            if (pulsePos != Offset.Unspecified) {
                val pulseAlpha = sin(PI.toFloat() * pulseT).coerceIn(0f, 1f) * intro
                drawCircle(
                    color = XireTerracottaLight.copy(alpha = 0.40f * pulseAlpha),
                    radius = minDim * 0.022f,
                    center = pulsePos
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.90f * pulseAlpha),
                    radius = minDim * 0.008f,
                    center = pulsePos
                )
                if (pulseT > 0.85f) {
                    b.flare = (b.flare + (pulseT - 0.85f) / 0.15f).coerceAtMost(1f)
                }
            }
        }

        // Depth-sort and draw 3D neurons with reactive halo glow
        for (i in drawOrder.indices) drawOrder[i] = i
        for (i in 0 until drawOrder.size - 1) {
            for (j in i + 1 until drawOrder.size) {
                if (projectedNeurons[drawOrder[i]].z > projectedNeurons[drawOrder[j]].z) {
                    val tmp = drawOrder[i]
                    drawOrder[i] = drawOrder[j]
                    drawOrder[j] = tmp
                }
            }
        }

        for (idx in drawOrder) {
            val spec = ThoughtNeuronSpecs[idx]
            val n = projectedNeurons[idx]
            val depthNorm = ((n.z / (minDim * 0.08f)) * 0.5f + 0.5f).coerceIn(0f, 1f)
            val baseR = minDim * spec.sizeRatio * n.scale * intro
            val r = baseR * (1f + 0.16f * n.flare)

            if (n.flare > 0.02f) {
                drawCircle(
                    color = XireTerracottaLight.copy(alpha = 0.30f * n.flare * intro),
                    radius = r * (1.75f + 0.45f * n.flare),
                    center = Offset(n.x, n.y)
                )
            }

            val coreColor = if (depthNorm < 0.5f) {
                lerp(XireBronzeDeep, XireTerracotta, depthNorm * 2f)
            } else {
                lerp(XireTerracotta, XireTerracottaLight, (depthNorm - 0.5f) * 2f)
            }

            drawCircle(
                color = coreColor.copy(alpha = (0.80f + 0.20f * depthNorm) * intro),
                radius = r,
                center = Offset(n.x, n.y)
            )
            drawCircle(
                color = XireBronzeHighlight.copy(alpha = (0.45f + 0.35f * n.flare) * intro),
                radius = r * 0.40f,
                center = Offset(n.x - r * 0.20f, n.y - r * 0.20f)
            )
        }

        // 4. Draw Person Silhouette Strokes (behind raised hand)
        val personStrokeAlpha = intro.coerceIn(0f, 1f)
        drawPath(
            path = shoulderPath,
            color = lineColor.copy(alpha = personStrokeAlpha),
            style = Stroke(
                width = minDim * 0.015f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = headProfilePath,
            color = lineColor.copy(alpha = personStrokeAlpha),
            style = Stroke(
                width = minDim * 0.015f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = eyePath,
            color = lineColor.copy(alpha = 0.85f * personStrokeAlpha),
            style = Stroke(
                width = minDim * 0.010f,
                cap = StrokeCap.Round
            )
        )

        // 5. Moving Articulated Thinking Hand raised near chin/jaw
        val wristBob = sin(theta * 2f) * (minDim * 0.014f)
        val wristSway = cos(theta) * (minDim * 0.010f)
        val wristTilt = 0.32f + 0.065f * sin(theta) // tilted up-and-left beneath the chin

        val wristCenter = Offset(w * 0.555f + wristSway, h * 0.925f + wristBob)
        val cosW = cos(wristTilt)
        val sinW = sin(wristTilt)

        fun handLocal(lx: Float, ly: Float): Offset {
            val rx = lx * cosW - ly * sinW
            val ry = lx * sinW + ly * cosW
            return Offset(wristCenter.x + rx * minDim, wristCenter.y - ry * minDim)
        }

        fun computeFinger(
            knuckleLocal: Offset,
            baseAngleFromHandUp: Float,
            proxLen: Float,
            distLen: Float,
            halfWidth: Float,
            flex1: Float,
            flex2: Float
        ): Pair<List<Offset>, Offset> {
            val a1 = baseAngleFromHandUp + flex1
            val a2 = a1 + flex2
            val k = handLocal(knuckleLocal.x, knuckleLocal.y)
            val midLocal = Offset(
                knuckleLocal.x + sin(a1) * proxLen,
                knuckleLocal.y + cos(a1) * proxLen
            )
            val tipLocal = Offset(
                midLocal.x + sin(a2) * distLen,
                midLocal.y + cos(a2) * distLen
            )
            val mid = handLocal(midLocal.x, midLocal.y)
            val tip = handLocal(tipLocal.x, tipLocal.y)

            val dir1 = Offset(mid.x - k.x, mid.y - k.y)
            val len1 = hypot(dir1.x, dir1.y).coerceAtLeast(1f)
            val n1 = Offset(dir1.y / len1, -dir1.x / len1)

            val dir2 = Offset(tip.x - mid.x, tip.y - mid.y)
            val len2 = hypot(dir2.x, dir2.y).coerceAtLeast(1f)
            val n2 = Offset(dir2.y / len2, -dir2.x / len2)

            val rBase = halfWidth * minDim
            val rMid = halfWidth * 0.84f * minDim
            val rTip = halfWidth * 0.65f * minDim

            val leftBase = Offset(k.x + n1.x * rBase, k.y + n1.y * rBase)
            val leftMid = Offset(mid.x + n1.x * rMid, mid.y + n1.y * rMid)
            val leftTip = Offset(tip.x + n2.x * rTip, tip.y + n2.y * rTip)
            val tipCap = Offset(tip.x + (dir2.x / len2) * rTip * 1.12f, tip.y + (dir2.y / len2) * rTip * 1.12f)
            val rightTip = Offset(tip.x - n2.x * rTip, tip.y - n2.y * rTip)
            val rightMid = Offset(mid.x - n1.x * rMid, mid.y - n1.y * rMid)
            val rightBase = Offset(k.x - n1.x * rBase, k.y - n1.y * rBase)

            return listOf(leftBase, leftMid, leftTip, tipCap, rightTip, rightMid, rightBase) to tipCap
        }

        // Real animated thinking finger gestures:
        // Index gently taps/curls under the chin, thumb rests along jaw, other fingers wave thoughtfully
        val thumbFlex1 = -0.10f + 0.08f * sin(theta)
        val thumbFlex2 = 0.14f + 0.09f * sin(theta + 0.5f)

        val indexFlex1 = -0.04f + 0.10f * sin(theta * 2f + 0.2f)
        val indexFlex2 = 0.18f + 0.16f * sin(theta * 2f + 0.7f)

        val middleFlex1 = 0.22f + 0.10f * sin(theta + 1.0f)
        val middleFlex2 = 0.38f + 0.16f * sin(theta + 1.5f)

        val ringFlex1 = 0.34f + 0.11f * sin(theta + 1.7f)
        val ringFlex2 = 0.48f + 0.16f * sin(theta + 2.2f)

        val pinkyFlex1 = 0.42f + 0.12f * sin(theta + 2.4f)
        val pinkyFlex2 = 0.52f + 0.16f * sin(theta + 2.9f)

        val (thumbPts, thumbTip) = computeFinger(
            knuckleLocal = Offset(-0.068f, 0.125f),
            baseAngleFromHandUp = -0.72f,
            proxLen = 0.072f,
            distLen = 0.062f,
            halfWidth = 0.018f,
            flex1 = thumbFlex1,
            flex2 = thumbFlex2
        )
        val (indexPts, indexTip) = computeFinger(
            knuckleLocal = Offset(-0.040f, 0.205f),
            baseAngleFromHandUp = -0.12f,
            proxLen = 0.088f,
            distLen = 0.072f,
            halfWidth = 0.015f,
            flex1 = indexFlex1,
            flex2 = indexFlex2
        )
        val (middlePts, middleTip) = computeFinger(
            knuckleLocal = Offset(0.002f, 0.214f),
            baseAngleFromHandUp = 0.08f,
            proxLen = 0.082f,
            distLen = 0.068f,
            halfWidth = 0.015f,
            flex1 = middleFlex1,
            flex2 = middleFlex2
        )
        val (ringPts, ringTip) = computeFinger(
            knuckleLocal = Offset(0.042f, 0.202f),
            baseAngleFromHandUp = 0.26f,
            proxLen = 0.074f,
            distLen = 0.060f,
            halfWidth = 0.014f,
            flex1 = ringFlex1,
            flex2 = ringFlex2
        )
        val (pinkyPts, pinkyTip) = computeFinger(
            knuckleLocal = Offset(0.078f, 0.180f),
            baseAngleFromHandUp = 0.42f,
            proxLen = 0.058f,
            distLen = 0.048f,
            halfWidth = 0.013f,
            flex1 = pinkyFlex1,
            flex2 = pinkyFlex2
        )

        fingerTips[0] = thumbTip
        fingerTips[1] = indexTip
        fingerTips[2] = middleTip
        fingerTips[3] = ringTip
        fingerTips[4] = pinkyTip

        val wristLeft = handLocal(-0.052f, -0.02f)
        val wristMidLeft = handLocal(-0.068f, 0.055f)
        val thumbHeel = handLocal(-0.082f, 0.095f)
        val palmOuter = handLocal(0.095f, 0.095f)
        val wristRight = handLocal(0.052f, -0.02f)

        handOutlinePath.reset()
        handOutlinePath.moveTo(wristLeft.x, wristLeft.y)
        handOutlinePath.cubicTo(
            wristMidLeft.x, wristMidLeft.y,
            thumbHeel.x, thumbHeel.y,
            thumbPts[0].x, thumbPts[0].y
        )

        fun appendFinger(pts: List<Offset>) {
            handOutlinePath.quadraticTo(pts[1].x, pts[1].y, pts[2].x, pts[2].y)
            handOutlinePath.quadraticTo(pts[3].x, pts[3].y, pts[4].x, pts[4].y)
            handOutlinePath.quadraticTo(pts[5].x, pts[5].y, pts[6].x, pts[6].y)
        }

        appendFinger(thumbPts)
        val webThumbIndex = handLocal(-0.058f, 0.175f)
        handOutlinePath.quadraticTo(webThumbIndex.x, webThumbIndex.y, indexPts[0].x, indexPts[0].y)

        appendFinger(indexPts)
        val web12 = handLocal(-0.018f, 0.194f)
        handOutlinePath.quadraticTo(web12.x, web12.y, middlePts[0].x, middlePts[0].y)

        appendFinger(middlePts)
        val web23 = handLocal(0.022f, 0.192f)
        handOutlinePath.quadraticTo(web23.x, web23.y, ringPts[0].x, ringPts[0].y)

        appendFinger(ringPts)
        val web34 = handLocal(0.060f, 0.174f)
        handOutlinePath.quadraticTo(web34.x, web34.y, pinkyPts[0].x, pinkyPts[0].y)

        appendFinger(pinkyPts)
        handOutlinePath.cubicTo(
            palmOuter.x, palmOuter.y,
            wristRight.x + minDim * 0.015f, wristRight.y - minDim * 0.03f,
            wristRight.x, wristRight.y
        )

        // Opaque-tinted hand fill so the raised hand cleanly sits in front of the collar/jaw
        handFillPath.reset()
        handFillPath.addPath(handOutlinePath)
        handFillPath.close()
        drawPath(
            path = handFillPath,
            color = bgColor.copy(alpha = 0.94f * intro)
        )

        handMeasure.setPath(handOutlinePath, false)
        val totalHandLen = handMeasure.length
        revealedHandPath.reset()
        handMeasure.getSegment(
            startDistance = 0f,
            stopDistance = totalHandLen * intro.coerceIn(0f, 1f),
            destination = revealedHandPath,
            startWithMoveTo = true
        )

        drawPath(
            path = revealedHandPath,
            color = lineColor,
            style = Stroke(
                width = minDim * 0.015f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Subtle warm spark at index/thumb tip during thinking gesture
        if (intro > 0.7f) {
            val tipAlpha = ((intro - 0.7f) / 0.3f).coerceIn(0f, 1f)
            val pulse = 0.65f + 0.35f * sin(theta * 2f)
            drawCircle(
                color = XireTerracotta.copy(alpha = 0.65f * tipAlpha * pulse),
                radius = minDim * 0.010f,
                center = indexTip
            )
        }
    }
}
