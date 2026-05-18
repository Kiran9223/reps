package com.reps.app.feature.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reps.app.ui.theme.RepsTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SplashScreen(onComplete: () -> Unit = {}) {

    val barbellOffset = remember { Animatable(-2000f) }
    val plateScales  = remember { List(3) { Animatable(0f) } }
    val shakeX       = remember { Animatable(0f) }
    val shakeY       = remember { Animatable(0f) }
    val flashAlpha   = remember { Animatable(0f) }

    // Letters start far off-screen; snapped to exact corners once we know screen size
    val letterXAnims = remember { List(4) { Animatable(6000f) } }
    val letterYAnims = remember { List(4) { Animatable(6000f) } }

    val lightningAlpha   = remember { Animatable(0f) }
    val strikeFlashAlpha = remember { Animatable(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        val W       = constraints.maxWidth.toFloat()
        val H       = constraints.maxHeight.toFloat()
        val density = LocalDensity.current

        // Corner starting offsets — recomputed only when screen size changes
        val cornerOffsets = remember(W, H) { computeLetterCornerOffsets(W, H, density) }

        LaunchedEffect(cornerOffsets) {

            // Snap letters to their screen corners before any animation
            cornerOffsets.forEachIndexed { i, (cx, cy) ->
                letterXAnims[i].snapTo(cx)
                letterYAnims[i].snapTo(cy)
            }

            // ── Phase 1: Bar slides in slowly ─────────────────────────────
            barbellOffset.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
            delay(100)

            // ── Phase 2: Plates slam on one by one ────────────────────────
            for (i in plateScales.indices) {
                plateScales[i].animateTo(
                    1f,
                    spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)
                )
                if (i < plateScales.lastIndex) delay(250)
            }

            // ── Phase 3: EARTHQUAKE ───────────────────────────────────────
            coroutineScope {
                launch {
                    flashAlpha.animateTo(0.92f, tween(30))
                    flashAlpha.animateTo(0f, tween(320))
                }
                launch {
                    shakeX.animateTo(0f, keyframes {
                        durationMillis = 750
                        42f at 40
                        -36f at 85
                        30f at 130
                        -25f at 172
                        20f at 215
                        -16f at 260
                        13f at 305
                        -10f at 350
                        7f at 395
                        -5f at 445
                        3f at 495
                        -2f at 545
                        0f at 750
                    })
                }
                launch {
                    shakeY.animateTo(0f, keyframes {
                        durationMillis = 750
                        -24f at 50
                        20f at 105
                        -17f at 158
                        13f at 210
                        -10f at 265
                        8f at 320
                        -6f at 375
                        4f at 430
                        -3f at 485
                        2f at 540
                        0f at 750
                    })
                }
            }

            // ── Phase 4: Letters fly from all four corners simultaneously ─
            coroutineScope {
                letterXAnims.forEach { anim ->
                    launch {
                        anim.animateTo(
                            0f,
                            spring(Spring.DampingRatioLowBouncy, Spring.StiffnessHigh)
                        )
                    }
                }
                letterYAnims.forEach { anim ->
                    launch {
                        anim.animateTo(
                            0f,
                            spring(Spring.DampingRatioLowBouncy, Spring.StiffnessHigh)
                        )
                    }
                }
            }

            // ── Phase 5: Aftershock when letters collide ──────────────────
            coroutineScope {
                launch {
                    shakeX.animateTo(0f, keyframes {
                        durationMillis = 340
                        16f at 35
                        -13f at 72
                        10f at 110
                        -7f at 150
                        5f at 190
                        -3f at 232
                        2f at 275
                        0f at 340
                    })
                }
                launch {
                    shakeY.animateTo(0f, keyframes {
                        durationMillis = 340
                        -9f at 42
                        7f at 85
                        -5f at 130
                        4f at 175
                        -2f at 220
                        0f at 340
                    })
                }
            }

            delay(420) // REPS formed — dramatic pause

            // ── Phase 6: Lightning strikes (3 bolts) ─────────────────────
            repeat(3) {
                coroutineScope {
                    launch {
                        lightningAlpha.animateTo(1f, tween(35))
                        lightningAlpha.animateTo(0f, tween(110))
                    }
                    launch {
                        strikeFlashAlpha.animateTo(0.6f, tween(35))
                        strikeFlashAlpha.animateTo(0f, tween(140))
                    }
                }
                delay(95)
            }

            delay(1000)
            onComplete()
        }

        // ── Read all animated values in composable scope ──────────────────
        val cShakeX        = shakeX.value
        val cShakeY        = shakeY.value
        val cFlash         = flashAlpha.value
        val cStrikeFlash   = strikeFlashAlpha.value
        val cBarbellOffset = barbellOffset.value
        val s0 = plateScales[0].value
        val s1 = plateScales[1].value
        val s2 = plateScales[2].value
        val lx0 = letterXAnims[0].value; val ly0 = letterYAnims[0].value
        val lx1 = letterXAnims[1].value; val ly1 = letterYAnims[1].value
        val lx2 = letterXAnims[2].value; val ly2 = letterYAnims[2].value
        val lx3 = letterXAnims[3].value; val ly3 = letterYAnims[3].value
        val cLightning     = lightningAlpha.value

        // ── Shakeable content layer ───────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(cShakeX.roundToInt(), cShakeY.roundToInt()) },
            contentAlignment = Alignment.Center
        ) {

            // Barbell
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .offset { IntOffset(cBarbellOffset.roundToInt(), 0) }
            ) {
                drawBarbell(s0, s1, s2)
            }

            // REPS letters + lightning/crack overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val offsets = listOf(lx0 to ly0, lx1 to ly1, lx2 to ly2, lx3 to ly3)
                    "REPS".forEachIndexed { i, char ->
                        val (ox, oy) = offsets[i]
                        Text(
                            text = char.toString(),
                            modifier = Modifier.offset {
                                IntOffset(ox.roundToInt(), oy.roundToInt())
                            },
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE8FF47)
                        )
                    }
                }

                // Lightning bolt drawn over the text
                Canvas(modifier = Modifier.size(320.dp, 190.dp)) {
                    if (cLightning > 0f) drawLightningBolt(cLightning)
                }
            }
        }

        // Plate-impact flash (full white)
        if (cFlash > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = cFlash))
            )
        }

        // Lightning flash (yellow-white, smaller pulse)
        if (cStrikeFlash > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFE8FF47).copy(alpha = cStrikeFlash))
            )
        }
    }
}

// ── Corner offset calculator ──────────────────────────────────────────────────

private fun computeLetterCornerOffsets(
    W: Float,
    H: Float,
    density: Density
): List<Pair<Float, Float>> {
    val charW   = with(density) { 38.dp.toPx() }
    val charH   = with(density) { 66.dp.toPx() }
    val spacing = with(density) { 6.dp.toPx() }
    val rowYOff = with(density) { 80.dp.toPx() }
    val pad     = with(density) { 56.dp.toPx() }

    val rowW   = 4 * charW + 3 * spacing
    val rowLeft = (W - rowW) / 2f
    val naturalY = H / 2f + rowYOff

    // Natural x-center of each letter in the centered Row
    val naturalX = (0..3).map { i -> rowLeft + i * (charW + spacing) + charW / 2f }

    // Visual center targets at each corner
    val targets = listOf(
        Pair(pad + charW / 2f,         pad + charH / 2f),         // R → top-left
        Pair(W - pad - charW / 2f,     pad + charH / 2f),         // E → top-right
        Pair(pad + charW / 2f,         H - pad - charH / 2f),     // P → bottom-left
        Pair(W - pad - charW / 2f,     H - pad - charH / 2f)      // S → bottom-right
    )

    return (0..3).map { i ->
        Pair(targets[i].first - naturalX[i], targets[i].second - naturalY)
    }
}

// ── Barbell drawing ───────────────────────────────────────────────────────────

private fun DrawScope.drawBarbell(scale0: Float, scale1: Float, scale2: Float) {
    val cy      = size.height / 2f
    val gap     = 2.dp.toPx()
    val barH    = 8.dp.toPx()
    val capW    = 18.dp.toPx()
    val capH    = 28.dp.toPx()
    val corner  = CornerRadius(2.dp.toPx())

    val pW = floatArrayOf(14.dp.toPx(), 11.dp.toPx(), 8.dp.toPx())
    val pH = floatArrayOf(70.dp.toPx(), 54.dp.toPx(), 40.dp.toPx())
    val pC = arrayOf(Color(0xFFE8FF47), Color(0xFFFF6B35), Color(0xFFD0D0D0))
    val pS = floatArrayOf(scale0, scale1, scale2)

    // Left x of each plate (inner→outer: large, medium, small)
    val lx = floatArrayOf(
        capW + gap + pW[2] + gap + pW[1] + gap,
        capW + gap + pW[2] + gap,
        capW + gap
    )

    drawRect(Color(0xFFBCBCBC), topLeft = Offset(0f, cy - barH / 2f), size = Size(size.width, barH))

    listOf(Offset(0f, cy - capH / 2f), Offset(size.width - capW, cy - capH / 2f)).forEach { tl ->
        drawRoundRect(Color(0xFF787878), topLeft = tl, size = Size(capW, capH), cornerRadius = corner)
    }

    for (i in 0..2) {
        val s = pS[i]; if (s <= 0f) continue
        val h = pH[i] * s
        drawRoundRect(pC[i], topLeft = Offset(lx[i], cy - h / 2f),                    size = Size(pW[i], h), cornerRadius = corner)
        drawRoundRect(pC[i], topLeft = Offset(size.width - lx[i] - pW[i], cy - h / 2f), size = Size(pW[i], h), cornerRadius = corner)
    }
}

// ── Lightning bolt drawing ────────────────────────────────────────────────────

private fun DrawScope.drawLightningBolt(alpha: Float) {
    val cx    = size.width / 2f
    val cy    = size.height / 2f
    val scale = size.height / 190f   // normalise relative offsets to canvas height

    // Zigzag points relative to canvas centre (unitless, scaled below)
    val pts = listOf(
        Offset(4f, -88f),  Offset(-20f, -58f), Offset(12f, -34f),
        Offset(-22f, -10f), Offset(16f, 14f),  Offset(-8f, 40f),
        Offset(20f, 62f),  Offset(-3f, 86f)
    )

    val path = Path().apply {
        moveTo(cx + pts[0].x * scale, cy + pts[0].y * scale)
        pts.drop(1).forEach { p -> lineTo(cx + p.x * scale, cy + p.y * scale) }
    }

    // Outer glow
    drawPath(path, Color.White.copy(alpha = alpha * 0.25f),
        style = Stroke(14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Mid corona
    drawPath(path, Color.White.copy(alpha = alpha * 0.55f),
        style = Stroke(5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Core white bolt
    drawPath(path, Color.White.copy(alpha = alpha),
        style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Bright yellow inner channel
    drawPath(path, Color(0xFFE8FF47).copy(alpha = alpha * 0.9f),
        style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// ── Preview ───────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun SplashScreenPreview() {
    RepsTheme { SplashScreen() }
}
