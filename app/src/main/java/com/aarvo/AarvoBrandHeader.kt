package com.aarvo

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Crash-safe subtle haptic trigger wrapper.
 * Wraps LocalHapticFeedback so accessibility disables or missing vibrators never crash.
 */
fun triggerHaptic(haptics: HapticFeedback, strong: Boolean = false) {
    runCatching {
        haptics.performHapticFeedback(
            if (strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
        )
    }
}

/* ========================================================================= */
/* Crisp Custom Canvas Vector Icons (no material-icons-extended dependency)  */
/* ========================================================================= */

@Composable
fun MicVectorIcon(tint: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.10f
        drawRoundRect(
            color = tint,
            topLeft = Offset(s * 0.36f, s * 0.14f),
            size = Size(s * 0.28f, s * 0.44f),
            cornerRadius = CornerRadius(s * 0.14f, s * 0.14f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(s * 0.22f, s * 0.30f),
            size = Size(s * 0.56f, s * 0.42f),
            style = Stroke(width = sw, cap = StrokeCap.Round)
        )
        drawLine(tint, Offset(s * 0.5f, s * 0.72f), Offset(s * 0.5f, s * 0.88f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.34f, s * 0.88f), Offset(s * 0.66f, s * 0.88f), sw, StrokeCap.Round)
    }
}

@Composable
fun MoonVectorIcon(tint: Color, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val path = Path().apply {
            moveTo(s * 0.72f, s * 0.68f)
            cubicTo(s * 0.48f, s * 0.68f, s * 0.32f, s * 0.52f, s * 0.32f, s * 0.28f)
            cubicTo(s * 0.32f, s * 0.22f, s * 0.34f, s * 0.17f, s * 0.37f, s * 0.13f)
            cubicTo(s * 0.21f, s * 0.19f, s * 0.14f, s * 0.36f, s * 0.16f, s * 0.54f)
            cubicTo(s * 0.19f, s * 0.74f, s * 0.37f, s * 0.88f, s * 0.58f, s * 0.86f)
            cubicTo(s * 0.72f, s * 0.85f, s * 0.83f, s * 0.76f, s * 0.88f, s * 0.64f)
            cubicTo(s * 0.83f, s * 0.66f, s * 0.78f, s * 0.68f, s * 0.72f, s * 0.68f)
            close()
        }
        drawPath(path, color = tint, style = Stroke(width = s * 0.095f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SunVectorIcon(tint: Color, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.095f
        val c = Offset(s * 0.5f, s * 0.5f)
        drawCircle(color = tint, radius = s * 0.20f, center = c, style = Stroke(width = sw))
        val rays = listOf(
            Offset(0.5f, 0.10f) to Offset(0.5f, 0.20f),
            Offset(0.5f, 0.80f) to Offset(0.5f, 0.90f),
            Offset(0.10f, 0.5f) to Offset(0.20f, 0.5f),
            Offset(0.80f, 0.5f) to Offset(0.90f, 0.5f),
            Offset(0.22f, 0.22f) to Offset(0.29f, 0.29f),
            Offset(0.71f, 0.71f) to Offset(0.78f, 0.78f),
            Offset(0.78f, 0.22f) to Offset(0.71f, 0.29f),
            Offset(0.29f, 0.71f) to Offset(0.22f, 0.78f)
        )
        rays.forEach { (start, end) ->
            drawLine(tint, Offset(s * start.x, s * start.y), Offset(s * end.x, s * end.y), sw, StrokeCap.Round)
        }
    }
}

@Composable
fun GridVectorIcon(tint: Color, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.095f
        val box = s * 0.30f
        val cr = CornerRadius(s * 0.07f, s * 0.07f)
        drawRoundRect(tint, Offset(s * 0.14f, s * 0.14f), Size(box, box), cr, style = Stroke(sw))
        drawRoundRect(tint, Offset(s * 0.56f, s * 0.14f), Size(box, box), cr, style = Stroke(sw))
        drawRoundRect(tint, Offset(s * 0.14f, s * 0.56f), Size(box, box), cr, style = Stroke(sw))
        drawRoundRect(tint, Offset(s * 0.56f, s * 0.56f), Size(box, box), cr, style = Stroke(sw))
    }
}

@Composable
fun PackageVectorIcon(tint: Color, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.09f
        val hex = Path().apply {
            moveTo(s * 0.5f, s * 0.12f)
            lineTo(s * 0.85f, s * 0.31f)
            lineTo(s * 0.85f, s * 0.69f)
            lineTo(s * 0.5f, s * 0.88f)
            lineTo(s * 0.15f, s * 0.69f)
            lineTo(s * 0.15f, s * 0.31f)
            close()
        }
        drawPath(hex, tint, style = Stroke(sw, join = StrokeJoin.Round, cap = StrokeCap.Round))
        drawLine(tint, Offset(s * 0.15f, s * 0.31f), Offset(s * 0.5f, s * 0.50f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.85f, s * 0.31f), Offset(s * 0.5f, s * 0.50f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.5f, s * 0.50f), Offset(s * 0.5f, s * 0.88f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.32f, s * 0.215f), Offset(s * 0.68f, s * 0.405f), sw * 0.85f, StrokeCap.Round)
    }
}

@Composable
fun TruckVectorIcon(tint: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.09f
        drawRoundRect(
            color = tint,
            topLeft = Offset(s * 0.08f, s * 0.24f),
            size = Size(s * 0.52f, s * 0.42f),
            cornerRadius = CornerRadius(s * 0.06f, s * 0.06f),
            style = Stroke(sw)
        )
        val cab = Path().apply {
            moveTo(s * 0.60f, s * 0.34f)
            lineTo(s * 0.78f, s * 0.34f)
            lineTo(s * 0.90f, s * 0.50f)
            lineTo(s * 0.90f, s * 0.66f)
            lineTo(s * 0.60f, s * 0.66f)
            close()
        }
        drawPath(cab, tint, style = Stroke(sw, join = StrokeJoin.Round))
        drawCircle(tint, radius = s * 0.10f, center = Offset(s * 0.28f, s * 0.74f), style = Stroke(sw))
        drawCircle(tint, radius = s * 0.10f, center = Offset(s * 0.74f, s * 0.74f), style = Stroke(sw))
    }
}

@Composable
fun ShieldVectorIcon(tint: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.09f
        val shield = Path().apply {
            moveTo(s * 0.50f, s * 0.10f)
            lineTo(s * 0.84f, s * 0.24f)
            lineTo(s * 0.84f, s * 0.52f)
            cubicTo(s * 0.84f, s * 0.72f, s * 0.66f, s * 0.84f, s * 0.50f, s * 0.90f)
            cubicTo(s * 0.34f, s * 0.84f, s * 0.16f, s * 0.72f, s * 0.16f, s * 0.52f)
            lineTo(s * 0.16f, s * 0.24f)
            close()
        }
        drawPath(shield, tint, style = Stroke(sw, join = StrokeJoin.Round))
        drawLine(tint, Offset(s * 0.36f, s * 0.52f), Offset(s * 0.46f, s * 0.62f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.46f, s * 0.62f), Offset(s * 0.66f, s * 0.40f), sw, StrokeCap.Round)
    }
}

@Composable
fun TagVectorIcon(tint: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.09f
        val path = Path().apply {
            moveTo(s * 0.14f, s * 0.14f)
            lineTo(s * 0.48f, s * 0.14f)
            lineTo(s * 0.86f, s * 0.52f)
            lineTo(s * 0.52f, s * 0.86f)
            lineTo(s * 0.14f, s * 0.48f)
            close()
        }
        drawPath(path, tint, style = Stroke(sw, join = StrokeJoin.Round))
        drawCircle(tint, radius = s * 0.06f, center = Offset(s * 0.32f, s * 0.32f))
    }
}

@Composable
fun StoreVectorIcon(tint: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.09f
        val awning = Path().apply {
            moveTo(s * 0.14f, s * 0.38f)
            lineTo(s * 0.22f, s * 0.18f)
            lineTo(s * 0.78f, s * 0.18f)
            lineTo(s * 0.86f, s * 0.38f)
            close()
        }
        drawPath(awning, tint, style = Stroke(sw, join = StrokeJoin.Round))
        drawRect(tint, Offset(s * 0.20f, s * 0.38f), Size(s * 0.60f, s * 0.46f), style = Stroke(sw))
        drawRect(tint, Offset(s * 0.40f, s * 0.56f), Size(s * 0.20f, s * 0.28f), style = Stroke(sw * 0.85f))
    }
}

@Composable
fun MapPinVectorIcon(tint: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.09f
        val pin = Path().apply {
            moveTo(s * 0.50f, s * 0.88f)
            cubicTo(s * 0.50f, s * 0.88f, s * 0.22f, s * 0.56f, s * 0.22f, s * 0.38f)
            cubicTo(s * 0.22f, s * 0.22f, s * 0.34f, s * 0.12f, s * 0.50f, s * 0.12f)
            cubicTo(s * 0.66f, s * 0.12f, s * 0.78f, s * 0.22f, s * 0.78f, s * 0.38f)
            cubicTo(s * 0.78f, s * 0.56f, s * 0.50f, s * 0.88f, s * 0.50f, s * 0.88f)
            close()
        }
        drawPath(pin, tint, style = Stroke(sw, join = StrokeJoin.Round))
        drawCircle(tint, radius = s * 0.09f, center = Offset(s * 0.50f, s * 0.38f), style = Stroke(sw))
    }
}

@Composable
fun ZoomVectorIcon(tint: Color, modifier: Modifier = Modifier.size(16.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.10f
        drawCircle(tint, radius = s * 0.28f, center = Offset(s * 0.42f, s * 0.42f), style = Stroke(sw))
        drawLine(tint, Offset(s * 0.62f, s * 0.62f), Offset(s * 0.86f, s * 0.86f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.32f, s * 0.42f), Offset(s * 0.52f, s * 0.42f), sw * 0.85f, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.42f, s * 0.32f), Offset(s * 0.42f, s * 0.52f), sw * 0.85f, StrokeCap.Round)
    }
}

@Composable
fun HelpVectorIcon(tint: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.09f
        drawCircle(tint, radius = s * 0.38f, center = Offset(s * 0.50f, s * 0.50f), style = Stroke(sw))
        drawArc(
            color = tint,
            startAngle = 190f,
            sweepAngle = 190f,
            useCenter = false,
            topLeft = Offset(s * 0.38f, s * 0.28f),
            size = Size(s * 0.24f, s * 0.24f),
            style = Stroke(sw, cap = StrokeCap.Round)
        )
        drawLine(tint, Offset(s * 0.50f, s * 0.52f), Offset(s * 0.50f, s * 0.60f), sw, StrokeCap.Round)
        drawCircle(tint, radius = s * 0.045f, center = Offset(s * 0.50f, s * 0.72f))
    }
}

@Composable
fun SortFilterVectorIcon(tint: Color, modifier: Modifier = Modifier.size(16.dp)) {
    Canvas(modifier = modifier) {
        val s = size.minDimension
        val sw = s * 0.11f
        drawLine(tint, Offset(s * 0.14f, s * 0.26f), Offset(s * 0.86f, s * 0.26f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.26f, s * 0.50f), Offset(s * 0.74f, s * 0.50f), sw, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.38f, s * 0.74f), Offset(s * 0.62f, s * 0.74f), sw, StrokeCap.Round)
    }
}

/* ========================================================================= */
/* AARVO Animated Brand Logo (Ported 100% from AarvoAnimatedLogo.tsx)        */
/* 5s initial trigger -> 40s repeating interval -> Tap to replay             */
/* Trolley squashes & slides left -> Ribbon jumps out -> Orange AARVO writes */
/* ========================================================================= */

private enum class BrandLogoPhase { IDLE, SQUASH, JUMP, WRITE, GLOW }

@Composable
fun AarvoAnimatedBrandLogo(
    isDarkTheme: Boolean = false,
    onTap: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    var phase by remember { mutableStateOf(BrandLogoPhase.IDLE) }
    var animCycle by remember { mutableIntStateOf(0) }

    val letterAlphas = remember { List(5) { Animatable(1f) } }
    val letterOffsets = remember { List(5) { Animatable(0f) } }

    fun triggerSequence() {
        scope.launch {
            phase = BrandLogoPhase.SQUASH
            triggerHaptic(haptics, strong = false)
            delay(320L)

            phase = BrandLogoPhase.JUMP
            triggerHaptic(haptics, strong = false)
            delay(530L)

            phase = BrandLogoPhase.WRITE
            triggerHaptic(haptics, strong = true)
            animCycle++

            // Reset letters to 0 before staggered fade-in
            letterAlphas.forEach { it.snapTo(0f) }
            letterOffsets.forEach { it.snapTo(8f) }

            letterAlphas.indices.forEach { idx ->
                launch {
                    delay(idx * 80L)
                    launch {
                        letterAlphas[idx].animateTo(1f, animationSpec = tween(320, easing = FastOutSlowInEasing))
                    }
                    letterOffsets[idx].animateTo(-2f, animationSpec = tween(220, easing = FastOutSlowInEasing))
                    letterOffsets[idx].animateTo(0f, animationSpec = tween(160, easing = LinearEasing))
                }
            }

            delay(750L)
            phase = BrandLogoPhase.GLOW
            delay(5200L)
            phase = BrandLogoPhase.IDLE
        }
    }

    // Exact requested timing: 5s after arriving on Home, then repeat every 40s
    LaunchedEffect(Unit) {
        delay(5000L)
        triggerSequence()
        while (true) {
            delay(40_000L)
            triggerSequence()
        }
    }

    // Trolley animation transforms
    val trolleyShiftDp by animateDpAsState(
        targetValue = when (phase) {
            BrandLogoPhase.SQUASH -> (-12).dp
            BrandLogoPhase.JUMP -> (-10).dp
            BrandLogoPhase.WRITE, BrandLogoPhase.GLOW -> (-4).dp
            BrandLogoPhase.IDLE -> 0.dp
        },
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "trolleyShift"
    )
    val trolleyScaleX by animateFloatAsState(
        targetValue = when (phase) {
            BrandLogoPhase.SQUASH -> 1.16f
            BrandLogoPhase.JUMP -> 1.04f
            else -> 1.0f
        },
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "trolleyScaleX"
    )
    val trolleyScaleY by animateFloatAsState(
        targetValue = when (phase) {
            BrandLogoPhase.SQUASH -> 0.78f
            BrandLogoPhase.JUMP -> 0.92f
            else -> 1.0f
        },
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "trolleyScaleY"
    )
    val trolleyRot by animateFloatAsState(
        targetValue = if (phase == BrandLogoPhase.SQUASH) -3f else 0f,
        animationSpec = tween(280),
        label = "trolleyRot"
    )

    // Ribbon jump animation transforms
    val ribbonOffsetY by animateDpAsState(
        targetValue = when (phase) {
            BrandLogoPhase.SQUASH -> 2.dp
            BrandLogoPhase.JUMP -> (-24).dp
            else -> 0.dp
        },
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "ribbonJumpY"
    )
    val ribbonScale by animateFloatAsState(
        targetValue = when (phase) {
            BrandLogoPhase.SQUASH -> 0.68f
            BrandLogoPhase.JUMP -> 1.28f
            BrandLogoPhase.WRITE -> 1.08f
            BrandLogoPhase.GLOW -> 1.03f
            BrandLogoPhase.IDLE -> 1.0f
        },
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "ribbonScale"
    )
    val ribbonRot by animateFloatAsState(
        targetValue = when (phase) {
            BrandLogoPhase.JUMP -> 14f
            BrandLogoPhase.WRITE -> -2f
            else -> 0f
        },
        animationSpec = tween(350),
        label = "ribbonRot"
    )
    val ribbonAlpha by animateFloatAsState(
        targetValue = if (phase == BrandLogoPhase.SQUASH) 0.65f else 1f,
        animationSpec = tween(250),
        label = "ribbonAlpha"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                triggerHaptic(haptics, strong = false)
                triggerSequence()
                onTap()
            }
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 1. Orange Shopping Trolley Vector
        Box(
            modifier = Modifier
                .size(28.dp)
                .offset(x = trolleyShiftDp)
                .graphicsLayer {
                    scaleX = trolleyScaleX
                    scaleY = trolleyScaleY
                    rotationZ = trolleyRot
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1.0f)
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val basketPath = Path().apply {
                    moveTo(w * 0.14f, h * 0.22f)
                    lineTo(w * 0.26f, h * 0.22f)
                    lineTo(w * 0.39f, h * 0.62f)
                    lineTo(w * 0.80f, h * 0.62f)
                    lineTo(w * 0.90f, h * 0.32f)
                    lineTo(w * 0.32f, h * 0.32f)
                }
                drawPath(
                    path = basketPath,
                    color = Color(0xFFFF5722),
                    style = Stroke(width = 2.6f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Inner wireframe grid
                drawLine(Color(0xFFFF7043), Offset(w * 0.38f, h * 0.42f), Offset(w * 0.85f, h * 0.42f), 1.6f * density, StrokeCap.Round)
                drawLine(Color(0xFFFF7043), Offset(w * 0.42f, h * 0.52f), Offset(w * 0.82f, h * 0.52f), 1.6f * density, StrokeCap.Round)
                drawLine(Color(0xFFFF7043), Offset(w * 0.52f, h * 0.32f), Offset(w * 0.52f, h * 0.60f), 1.6f * density, StrokeCap.Round)
                drawLine(Color(0xFFFF7043), Offset(w * 0.68f, h * 0.32f), Offset(w * 0.68f, h * 0.60f), 1.6f * density, StrokeCap.Round)
                // Wheels
                val wr = 3.0f * density
                drawCircle(Color(0xFFFF5722), radius = wr, center = Offset(w * 0.45f, h * 0.79f))
                drawCircle(Color.White, radius = wr * 0.42f, center = Offset(w * 0.45f, h * 0.79f))
                drawCircle(Color(0xFFFF5722), radius = wr, center = Offset(w * 0.74f, h * 0.79f))
                drawCircle(Color.White, radius = wr * 0.42f, center = Offset(w * 0.74f, h * 0.79f))
            }
        }

        Spacer(Modifier.width(4.dp))

        // 2. Real AARVO 3D Ribbon Icon (jumps cleanly between Trolley & AARVO text)
        Box(
            modifier = Modifier
                .size(28.dp)
                .offset(y = ribbonOffsetY)
                .alpha(ribbonAlpha)
                .graphicsLayer {
                    scaleX = ribbonScale
                    scaleY = ribbonScale
                    rotationZ = ribbonRot
                },
            contentAlignment = Alignment.Center
        ) {
            if (phase == BrandLogoPhase.JUMP || phase == BrandLogoPhase.GLOW) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF9800).copy(alpha = 0.25f))
                )
            }
            Image(
                painter = painterResource(id = R.drawable.aarvo_ribbon_logo),
                contentDescription = "AARVO Ribbon Icon",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(Modifier.width(6.dp))

        // 3. Vibrant Orange AARVO Text with per-letter fade & rise
        Row(verticalAlignment = Alignment.CenterVertically) {
            val chars = remember { listOf("A", "A", "R", "V", "O") }
            chars.forEachIndexed { idx, ch ->
                val yOff = letterOffsets[idx].value
                val al = letterAlphas[idx].value
                Text(
                    text = ch,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = if (phase == BrandLogoPhase.GLOW) Color(0xFFFF6D00) else Color(0xFFFF5722),
                    modifier = Modifier
                        .alpha(al)
                        .offset(y = yOff.dp)
                )
            }
        }
    }
}

/* ========================================================================= */
/* Top App Bar + Search + Animated Logo + Horizontal Department Filter Chips */
/* ========================================================================= */

@Composable
fun AarvoPreviewTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onVoiceClick: () -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    unreadNotificationCount: Int,
    onNotificationsClick: () -> Unit,
    cartBadgeCount: Int,
    onCartClick: () -> Unit,
    onLogoClick: () -> Unit,
    departments: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val topIconTint = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF1F1612)
    val headerBrush = if (isDarkTheme) {
        Brush.verticalGradient(listOf(Color(0xFF26160C), Color(0xFF1B120E), Color(0xFF181311)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFFFE5CC), Color(0xFFFFF4E8), Color(0xFFFFFFFF)))
    }
    val bottomBorderColor = if (isDarkTheme) Color(0xFF2E221C) else Color(0xFFFFE8D6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBrush)
    ) {
        // Row 1: Hamburger | Search Pill (with clear & Mic) | Sun/Moon Theme | Bell | Cart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    triggerHaptic(haptics)
                    onMenuClick()
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = topIconTint, modifier = Modifier.size(22.dp))
            }

            // Rounded Pill Search Bar (matching preview screenshot 100%)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                shape = CircleShape,
                color = if (isDarkTheme) Color(0xFF211A17) else Color.White,
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF4A3B34) else Color(0xFFFED7AA)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isDarkTheme) Color(0xFFFF8A65) else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "Search for products...",
                                color = if (isDarkTheme) Color(0xFFA8988F) else Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = if (isDarkTheme) Color(0xFFFAF3EE) else Color(0xFF0F172A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChange("") },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = {
                            triggerHaptic(haptics)
                            onVoiceClick()
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        MicVectorIcon(
                            tint = if (isDarkTheme) Color(0xFFCBBDB4) else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Theme toggle icon button (Moon in light mode, Sun in dark mode)
            IconButton(
                onClick = {
                    triggerHaptic(haptics)
                    onToggleTheme()
                },
                modifier = Modifier.size(34.dp)
            ) {
                if (isDarkTheme) {
                    SunVectorIcon(tint = Color(0xFFFBBF24), modifier = Modifier.size(19.dp))
                } else {
                    MoonVectorIcon(tint = Color(0xFF334155), modifier = Modifier.size(19.dp))
                }
            }

            // Notification bell (badge ONLY if unreadNotificationCount > 0)
            IconButton(
                onClick = {
                    triggerHaptic(haptics)
                    onNotificationsClick()
                },
                modifier = Modifier.size(34.dp)
            ) {
                if (unreadNotificationCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = Color(0xFFFF5722), contentColor = Color.White) {
                                Text(unreadNotificationCount.toString(), fontSize = 9.sp)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = topIconTint, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = topIconTint, modifier = Modifier.size(20.dp))
                }
            }

            // Cart icon with orange badge
            IconButton(
                onClick = {
                    triggerHaptic(haptics)
                    onCartClick()
                },
                modifier = Modifier.size(36.dp)
            ) {
                if (cartBadgeCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(containerColor = Color(0xFFFF5722), contentColor = Color.White) {
                                Text(if (cartBadgeCount > 99) "99+" else cartBadgeCount.toString(), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = topIconTint, modifier = Modifier.size(21.dp))
                    }
                } else {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = topIconTint, modifier = Modifier.size(21.dp))
                }
            }
        }

        // Row 2: Centered Animated Brand Logo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp, bottom = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            AarvoAnimatedBrandLogo(isDarkTheme = isDarkTheme, onTap = onLogoClick)
        }

        // Row 3: Horizontal Scrollable Department Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            departments.forEach { dept ->
                val isSelected = selectedCategory.equals(dept, ignoreCase = true) ||
                    (dept == "Home & Living" && selectedCategory.equals("Home", ignoreCase = true)) ||
                    (dept == "Beauty & Care" && selectedCategory.equals("Beauty", ignoreCase = true))
                Surface(
                    shape = CircleShape,
                    color = when {
                        isSelected -> Color(0xFFFF5722)
                        isDarkTheme -> Color(0xFF211A17)
                        else -> Color.White
                    },
                    border = if (isSelected) null else BorderStroke(
                        1.dp,
                        if (isDarkTheme) Color(0xFF3A2D27) else Color(0xFFFED7AA)
                    ),
                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                    modifier = Modifier.clickable {
                        triggerHaptic(haptics)
                        onSelectCategory(dept)
                    }
                ) {
                    Text(
                        text = dept,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = when {
                            isSelected -> Color.White
                            isDarkTheme -> Color(0xFFFAF3EE)
                            else -> Color(0xFF334155)
                        },
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(bottomBorderColor))
    }
}
