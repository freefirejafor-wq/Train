package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Camera3DMode
import com.example.data.Environment3DTheme
import com.example.data.LiveTrainTelemetry
import com.example.ui.theme.CrimsonExpress
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.SignalGold
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Live3DTrainStage(
    telemetry: LiveTrainTelemetry,
    cameraMode: Camera3DMode,
    environmentTheme: Environment3DTheme,
    showOppositeTrain: Boolean,
    stageHeight: Dp = 270.dp,
    onCycleCamera: () -> Unit,
    onCycleEnvironment: () -> Unit,
    onToggleOppositeTrain: () -> Unit,
    onExpandFullScreen: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "train_3d_motion")
    val rawPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "track_phase"
    )
    val crossingPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "crossing_phase"
    )

    val speedFactor = if (telemetry.currentSpeedKmh <= 0) 0f else (telemetry.currentSpeedKmh / 90f).coerceIn(0.25f, 1.5f)
    val effectivePhase = (rawPhase * speedFactor) % 1f

    val calHour = Calendar.getInstance(TimeZone.getTimeZone("GMT+6")).get(Calendar.HOUR_OF_DAY)
    val isNight = when (environmentTheme) {
        Environment3DTheme.NIGHT_VISION -> true
        Environment3DTheme.DAYLIGHT -> false
        Environment3DTheme.MONSOON_RAIN -> false
        Environment3DTheme.AUTO -> calHour < 6 || calHour >= 18
    }
    val isRain = environmentTheme == Environment3DTheme.MONSOON_RAIN

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(stageHeight)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(24.dp))
            .testTag("live_3d_train_stage")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (cameraMode) {
                Camera3DMode.COCKPIT_3D -> drawCockpit3DScene(
                    telemetry = telemetry,
                    phase = effectivePhase,
                    crossingPhase = crossingPhase,
                    isNight = isNight,
                    isRain = isRain,
                    showOppositeTrain = showOppositeTrain
                )
                Camera3DMode.ISOMETRIC_DRONE -> drawRoadsideIsometric3DScene(
                    telemetry = telemetry,
                    phase = effectivePhase,
                    crossingPhase = crossingPhase,
                    isNight = isNight,
                    isRain = isRain,
                    showOppositeTrain = showOppositeTrain
                )
                Camera3DMode.TOP_RADAR -> drawTopRadar3DScene(
                    telemetry = telemetry,
                    phase = effectivePhase,
                    crossingPhase = crossingPhase,
                    showOppositeTrain = showOppositeTrain
                )
            }
        }

        // Top HUD Overlay: Live Speedometer + Status + Camera & Environment Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = Color(0xCC041A13),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, SignalGold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    telemetry.isOffToday -> CrimsonExpress
                                    telemetry.currentSpeedKmh > 0 -> LiveGreen
                                    else -> SignalGold
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "${telemetry.currentSpeedKmh}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = SignalGold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "km/h",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Text(
                            text = "${telemetry.train.nameBn} (${telemetry.train.trainNumberBn})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HudIconChip(
                    icon = Icons.Default.Cameraswitch,
                    label = cameraMode.labelBn,
                    onClick = onCycleCamera,
                    tag = "btn_cycle_3d_camera"
                )
                HudIconChip(
                    icon = Icons.Default.WbSunny,
                    label = if (isNight) "রাত 3D" else if (isRain) "বৃষ্টি 3D" else "দিন 3D",
                    onClick = onCycleEnvironment,
                    tag = "btn_cycle_3d_env"
                )
                if (onExpandFullScreen != null) {
                    Surface(
                        color = Color(0xCC041A13),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onExpandFullScreen() }
                            .testTag("btn_expand_3d_fullscreen")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "ফুলস্ক্রিন 3D",
                                tint = SignalGold,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom HUD Overlay: Current Station -> Next Station + Arrival/Departure + Opposite Train Alert
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xE603150F), Color(0xFA03150F))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF143D2D))
                        .clickable { onToggleOppositeTrain() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("toggle_opposite_train_chip")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "অন্য ট্রেন ক্রসিং",
                        tint = SignalGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showOppositeTrain) {
                            "ক্রসিং: ${telemetry.train.oppositeTrainNameBn} (${String.format("%.1f", telemetry.oppositeTrainDistanceKm)} কিমি)"
                        } else {
                            "ক্রসিং ট্রেন দেখুন"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFE082),
                        fontSize = 11.sp
                    )
                }

                Surface(
                    color = when {
                        telemetry.isOffToday -> CrimsonExpress
                        telemetry.delayMinutes <= 5 -> Color(0xFF0B5E3C)
                        else -> Color(0xFF8E1C1C)
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = when {
                            telemetry.isOffToday -> "আজ বন্ধ (${telemetry.todayNameBn})"
                            telemetry.isNearBridge -> "🌉 ${telemetry.bridgeNameBn}"
                            telemetry.delayMinutes <= 0 -> "🟢 অন-টাইম"
                            else -> "⏱️ লেট: ${telemetry.delayMinutes} মি."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            telemetry.isOffToday -> "🔴 আজ ${telemetry.train.offDayBn} — এই ট্রেনের সাপ্তাহিক বন্ধ (সিডিউল প্রিভিউ)"
                            telemetry.isAtStation -> "📍 এখন স্টেশনে: ${telemetry.lastStation.nameBn} (প্ল্যাটফর্ম ${telemetry.lastStation.platformNumber})"
                            else -> "📍 ছাড়িয়েছে: ${telemetry.lastStation.nameBn} ➔ পরবর্তী: ${telemetry.nextStation.nameBn}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "আসবে: ${telemetry.expectedNextArrivalFormatted} • ছাড়বে: ${telemetry.expectedNextDepartureFormatted} • বাকি ${String.format("%.1f", telemetry.distanceRemainingToNextKm)} কিমি (${telemetry.etaNextStationMinutes} মি.)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB9F6CA),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HudIconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        color = Color(0xCC041A13),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50))
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = SignalGold,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * 1. COCKPIT / FRONT 3D PERSPECTIVE SCENE WITH PARALLEL ROADSIDE HIGHWAY, TREES & LEVEL CROSSING:
 * Shows dual 3D railway tracks + adjacent asphalt highway on the left with moving vehicles & trees!
 */
private fun DrawScope.drawCockpit3DScene(
    telemetry: LiveTrainTelemetry,
    phase: Float,
    crossingPhase: Float,
    isNight: Boolean,
    isRain: Boolean,
    showOppositeTrain: Boolean
) {
    val w = size.width
    val h = size.height
    val horizonY = h * 0.35f
    val vanishX = w * 0.54f

    // 1. Sky Gradient
    val skyColors = when {
        isNight -> listOf(Color(0xFF020B14), Color(0xFF071E26), Color(0xFF0E342A))
        isRain -> listOf(Color(0xFF1C2D38), Color(0xFF2E4756), Color(0xFF3F5E5A))
        else -> listOf(Color(0xFF0A4D68), Color(0xFF088395), Color(0xFF78D6C6))
    }
    drawRect(brush = Brush.verticalGradient(skyColors, startY = 0f, endY = horizonY))

    if (isNight) {
        drawCircle(color = Color(0xFFFFF9C4), radius = 16f, center = Offset(w * 0.82f, horizonY * 0.42f))
        for (i in 1..12) {
            val sx = (w * ((i * 37) % 100) / 100f)
            val sy = (horizonY * ((i * 23) % 80) / 100f)
            drawCircle(color = Color.White.copy(alpha = 0.65f), radius = 2f, center = Offset(sx, sy))
        }
    } else if (!isRain) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFFE082), Color(0x00FFE082)),
                center = Offset(w * 0.80f, horizonY * 0.45f),
                radius = 48f
            ),
            radius = 48f,
            center = Offset(w * 0.80f, horizonY * 0.45f)
        )
    }

    // 2. Ground & Green Bengal Landscape below Horizon
    val groundColors = if (isNight) {
        listOf(Color(0xFF062018), Color(0xFF04140E), Color(0xFF020B08))
    } else {
        listOf(Color(0xFF176B47), Color(0xFF0E4930), Color(0xFF07281A))
    }
    drawRect(
        brush = Brush.verticalGradient(groundColors, startY = horizonY, endY = h),
        topLeft = Offset(0f, horizonY),
        size = Size(w, h - horizonY)
    )

    // 2B. Parallel Roadside Highway (Left Side Asphalt Road with dashed white lane dividers & moving vehicle!)
    val roadPath = Path().apply {
        moveTo(vanishX - w * 0.16f, horizonY)
        lineTo(vanishX - w * 0.09f, horizonY)
        lineTo(w * 0.08f, h)
        lineTo(-w * 0.18f, h)
        close()
    }
    drawPath(path = roadPath, color = Color(0xFF263238))
    drawLine(
        color = Color(0xFFFFF59D).copy(alpha = 0.7f),
        start = Offset(vanishX - w * 0.125f, horizonY),
        end = Offset(-w * 0.04f, h),
        strokeWidth = 2.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), phase * 50f)
    )

    // Moving Bus/Car on the Roadside Highway
    val carDepth = ((crossingPhase + 0.4f) % 1f).coerceIn(0.18f, 0.85f)
    val carY = horizonY + (h - horizonY) * carDepth
    val carX = (vanishX - w * 0.12f) - (w * 0.16f) * carDepth
    val carW = 16f + 28f * carDepth
    val carH = 12f + 18f * carDepth
    drawRoundRect(
        color = Color(0xFFFF7043),
        topLeft = Offset(carX - carW / 2f, carY - carH),
        size = Size(carW, carH),
        cornerRadius = CornerRadius(5f, 5f)
    )
    drawCircle(Color(0xFFFFF59D), radius = 3f, center = Offset(carX - carW * 0.25f, carY - 3f))
    drawCircle(Color(0xFFFFF59D), radius = 3f, center = Offset(carX + carW * 0.25f, carY - 3f))

    // 3. 3D Railway Ballast Roadbed (Trapezoid from horizon to bottom)
    val ballastPath = Path().apply {
        moveTo(vanishX - w * 0.08f, horizonY)
        lineTo(vanishX + w * 0.08f, horizonY)
        lineTo(w * 0.96f, h)
        lineTo(w * 0.10f, h)
        close()
    }
    drawPath(
        path = ballastPath,
        brush = Brush.verticalGradient(
            listOf(Color(0xFF283531), Color(0xFF1B2421), Color(0xFF111715)),
            startY = horizonY,
            endY = h
        )
    )

    // 4. Perspective Scrolling Sleepers + Roadside Trees + Catenary / Bridge Trusses
    val numSleepers = 14
    for (i in 0 until numSleepers) {
        val rawT = ((i + phase) / numSleepers.toFloat()).coerceIn(0.01f, 1f)
        val depth = rawT * rawT
        val y = horizonY + (h - horizonY) * depth
        val halfSpread = (w * 0.06f) + (w * 0.38f) * depth
        val strokeW = (1.5f + 5f * depth)

        // Left track sleeper
        drawLine(
            color = Color(0xFF546E7A).copy(alpha = 0.35f + 0.55f * depth),
            start = Offset(vanishX - halfSpread * 0.95f, y),
            end = Offset(vanishX - halfSpread * 0.12f, y),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        // Right track sleeper
        drawLine(
            color = Color(0xFF546E7A).copy(alpha = 0.35f + 0.55f * depth),
            start = Offset(vanishX + halfSpread * 0.12f, y),
            end = Offset(vanishX + halfSpread * 0.95f, y),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // 3D Trees & Catenary Poles passing by alongside the track
        if (i % 3 == 0) {
            val poleHeight = 18f + 95f * depth
            val leftPoleX = vanishX - halfSpread * 1.08f
            val rightPoleX = vanishX + halfSpread * 1.08f
            val poleColor = if (telemetry.isNearBridge) Color(0xFFB0BEC5) else Color(0xFF37474F)

            drawLine(
                color = poleColor,
                start = Offset(leftPoleX, y),
                end = Offset(leftPoleX, y - poleHeight),
                strokeWidth = 2f + 4f * depth
            )
            drawLine(
                color = poleColor,
                start = Offset(rightPoleX, y),
                end = Offset(rightPoleX, y - poleHeight),
                strokeWidth = 2f + 4f * depth
            )
            if (telemetry.isNearBridge) {
                drawLine(
                    color = Color(0xFF90A4AE).copy(alpha = 0.7f),
                    start = Offset(leftPoleX, y - poleHeight),
                    end = Offset(rightPoleX, y - poleHeight),
                    strokeWidth = 2f + 3f * depth
                )
            } else {
                // Green 3D Tree Canopy on the right embankment
                val treeX = rightPoleX + 16f * depth
                val canopyRadius = 8f + 22f * depth
                drawCircle(
                    color = if (isNight) Color(0xFF0A3A28) else Color(0xFF2E7D32),
                    radius = canopyRadius,
                    center = Offset(treeX, y - poleHeight * 0.75f)
                )
            }
        }
    }

    // 5. Gleaming Steel Rails
    val railColor = Color(0xFFCFD8DC)
    drawLine(railColor, Offset(vanishX - w * 0.05f, horizonY), Offset(w * 0.18f, h), strokeWidth = 3.5f)
    drawLine(railColor, Offset(vanishX - w * 0.015f, horizonY), Offset(w * 0.46f, h), strokeWidth = 3.5f)
    drawLine(railColor, Offset(vanishX + w * 0.015f, horizonY), Offset(w * 0.59f, h), strokeWidth = 3.5f)
    drawLine(railColor, Offset(vanishX + w * 0.05f, horizonY), Offset(w * 0.88f, h), strokeWidth = 3.5f)

    // 6. Opposite Train on Right Track
    if (showOppositeTrain) {
        val oppDepth = (0.18f + crossingPhase * 0.65f).coerceIn(0.15f, 0.82f)
        val oppY = horizonY + (h - horizonY) * oppDepth
        val oppCenterX = vanishX + (w * 0.22f) * oppDepth
        val oppW = w * 0.16f * oppDepth + 24f
        val oppH = h * 0.22f * oppDepth + 22f

        drawRoundRect(
            color = Color(0xFF1565C0),
            topLeft = Offset(oppCenterX - oppW / 2f, oppY - oppH),
            size = Size(oppW, oppH),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRect(
            color = Color(0xFFFFB300),
            topLeft = Offset(oppCenterX - oppW / 2f, oppY - oppH * 0.45f),
            size = Size(oppW, oppH * 0.16f)
        )
        drawRoundRect(
            color = Color(0xFF0A192F),
            topLeft = Offset(oppCenterX - oppW * 0.38f, oppY - oppH * 0.85f),
            size = Size(oppW * 0.76f, oppH * 0.28f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawCircle(Color(0xFFFFF59D), radius = 3.5f + 3f * oppDepth, center = Offset(oppCenterX - oppW * 0.28f, oppY - oppH * 0.2f))
        drawCircle(Color(0xFFFFF59D), radius = 3.5f + 3f * oppDepth, center = Offset(oppCenterX + oppW * 0.28f, oppY - oppH * 0.2f))
    }

    // 7. Main 3D Bangladesh Railway Train on Left Track
    val trainBaseY = h * 0.76f
    val trainCenterX = w * 0.34f
    val locoW = w * 0.25f
    val locoH = h * 0.32f

    // 3D Trailing Coaches receding toward horizon
    for (c in 3 downTo 1) {
        val coachFactor = c * 0.18f
        val cx = trainCenterX + (vanishX - trainCenterX) * coachFactor - (c * 6f)
        val cy = trainBaseY - (trainBaseY - horizonY) * coachFactor
        val cw = locoW * (1f - coachFactor * 0.65f)
        val ch = locoH * (1f - coachFactor * 0.65f)
        drawRoundRect(
            color = if (telemetry.train.liveryType == "WHITE_RED") Color(0xFFECEFF1) else Color(0xFF0A6847),
            topLeft = Offset(cx - cw / 2f, cy - ch),
            size = Size(cw, ch),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRect(
            color = Color(0xFFD92B2B),
            topLeft = Offset(cx - cw / 2f, cy - ch * 0.52f),
            size = Size(cw, ch * 0.18f)
        )
    }

    // Diesel Exhaust Plume when train is moving
    if (telemetry.currentSpeedKmh > 0) {
        for (p in 1..3) {
            val smokeProgress = (phase + p * 0.25f) % 1f
            drawCircle(
                color = Color.White.copy(alpha = (1f - smokeProgress) * 0.22f),
                radius = 8f + smokeProgress * 18f,
                center = Offset(
                    trainCenterX + smokeProgress * 12f,
                    trainBaseY - locoH - 6f - smokeProgress * 28f
                )
            )
        }
    }

    drawOval(
        color = Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(trainCenterX - locoW * 0.56f, trainBaseY - 8f),
        size = Size(locoW * 1.12f, 20f)
    )

    val bodyPrimary = if (telemetry.train.liveryType == "WHITE_RED") Color(0xFFF5F7F8) else Color(0xFF0D7A54)
    drawRoundRect(
        color = bodyPrimary,
        topLeft = Offset(trainCenterX - locoW / 2f, trainBaseY - locoH),
        size = Size(locoW, locoH),
        cornerRadius = CornerRadius(14f, 14f)
    )

    drawRect(
        color = Color(0xFFE53935),
        topLeft = Offset(trainCenterX - locoW / 2f, trainBaseY - locoH * 0.48f),
        size = Size(locoW, locoH * 0.22f)
    )

    drawRect(
        color = SignalGold,
        topLeft = Offset(trainCenterX - locoW * 0.35f, trainBaseY - locoH * 0.26f),
        size = Size(locoW * 0.70f, 5f)
    )

    drawRoundRect(
        color = Color(0xFF062019),
        topLeft = Offset(trainCenterX - locoW * 0.40f, trainBaseY - locoH * 0.84f),
        size = Size(locoW * 0.80f, locoH * 0.28f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    drawRoundRect(
        color = Color(0xFF263238),
        topLeft = Offset(trainCenterX - locoW * 0.46f, trainBaseY - locoH * 0.14f),
        size = Size(locoW * 0.92f, locoH * 0.16f),
        cornerRadius = CornerRadius(4f, 4f)
    )

    val headlightColor = Color(0xFFFFF176)
    drawCircle(headlightColor, radius = 7f, center = Offset(trainCenterX - locoW * 0.30f, trainBaseY - locoH * 0.36f))
    drawCircle(headlightColor, radius = 7f, center = Offset(trainCenterX + locoW * 0.30f, trainBaseY - locoH * 0.36f))
    drawCircle(SignalGold, radius = 5f, center = Offset(trainCenterX, trainBaseY - locoH * 0.92f))

    if (isNight || isRain) {
        val beamPath = Path().apply {
            moveTo(trainCenterX - locoW * 0.30f, trainBaseY - locoH * 0.36f)
            lineTo(trainCenterX + locoW * 0.30f, trainBaseY - locoH * 0.36f)
            lineTo(w * 0.56f, h)
            lineTo(w * 0.08f, h)
            close()
        }
        drawPath(
            path = beamPath,
            brush = Brush.verticalGradient(
                listOf(Color(0x66FFF59D), Color(0x00FFF59D)),
                startY = trainBaseY - locoH * 0.36f,
                endY = h
            )
        )
    }

    // 8. 3D Railway Signal Post & Level-Crossing Gate Bar
    val sigX = w * 0.12f
    val sigBottomY = h * 0.72f
    val sigTopY = h * 0.32f
    drawLine(Color(0xFF90A4AE), Offset(sigX, sigBottomY), Offset(sigX, sigTopY), strokeWidth = 4f)
    drawRoundRect(
        color = Color(0xFF1C262B),
        topLeft = Offset(sigX - 10f, sigTopY),
        size = Size(20f, 46f),
        cornerRadius = CornerRadius(6f, 6f)
    )
    val sigColor = when {
        telemetry.isAtStation || telemetry.isOffToday -> Color(0xFFFF5252)
        telemetry.segmentProgress > 0.82f -> SignalGold
        else -> LiveGreen
    }
    drawCircle(sigColor, radius = 6f, center = Offset(sigX, sigTopY + 14f))

    if (isRain) {
        for (r in 0..22) {
            val rx = ((r * 53 + (phase * 300).toInt()) % w.toInt()).toFloat()
            val ry = ((r * 41 + (phase * 400).toInt()) % h.toInt()).toFloat()
            drawLine(
                color = Color(0x88B3E5FC),
                start = Offset(rx, ry),
                end = Offset(rx - 10f, ry + 24f),
                strokeWidth = 1.8f
            )
        }
    }
}

/**
 * 2. ROADSIDE & TRACK ISOMETRIC SIDE 3D SCENE:
 * Shows the train running on the embankment alongside a parallel road with moving buses/cars, trees & bridges!
 */
private fun DrawScope.drawRoadsideIsometric3DScene(
    telemetry: LiveTrainTelemetry,
    phase: Float,
    crossingPhase: Float,
    isNight: Boolean,
    isRain: Boolean,
    showOppositeTrain: Boolean
) {
    val w = size.width
    val h = size.height
    val skyColors = if (isNight) {
        listOf(Color(0xFF030D14), Color(0xFF09222C), Color(0xFF0D382B))
    } else {
        listOf(Color(0xFF07415A), Color(0xFF0E7C7B), Color(0xFF88E0D0))
    }
    drawRect(brush = Brush.verticalGradient(skyColors))

    val trackY = h * 0.56f
    val roadY = h * 0.72f

    // Green Bengal Countryside between horizon and road
    drawRect(
        color = if (telemetry.isNearBridge) Color(0xFF064663) else Color(0xFF145A3B),
        topLeft = Offset(0f, trackY - 20f),
        size = Size(w, h - trackY + 20f)
    )

    // Scrolling Roadside Trees in Background
    for (t in 0..6) {
        val tx = ((t - phase) / 6f) * w
        val treeTopY = trackY - 72f
        drawLine(Color(0xFF4E342E), Offset(tx, trackY - 12f), Offset(tx, treeTopY + 14f), strokeWidth = 4f)
        drawCircle(
            color = if (isNight) Color(0xFF0B3D2A) else Color(0xFF2E7D32),
            radius = 18f,
            center = Offset(tx, treeTopY + 10f)
        )
    }

    // Opposite Train on Background Track
    if (showOppositeTrain) {
        val oppX = w * (1.1f - crossingPhase * 1.4f)
        val oppY = trackY - 46f
        for (c in 0..3) {
            drawRoundRect(
                color = Color(0xFF1565C0).copy(alpha = 0.88f),
                topLeft = Offset(oppX + c * 74f, oppY),
                size = Size(68f, 26f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRect(
                color = SignalGold,
                topLeft = Offset(oppX + c * 74f, oppY + 11f),
                size = Size(68f, 5f)
            )
        }
    }

    // 3D Viaduct / Embankment Sleepers
    val sleeperCount = 22
    for (i in 0 until sleeperCount) {
        val sx = ((i - phase) / sleeperCount.toFloat()) * w
        drawRect(
            color = Color(0xFF546E7A),
            topLeft = Offset(sx, trackY - 2f),
            size = Size(12f, 8f)
        )
    }
    drawLine(Color(0xFFECEFF1), Offset(0f, trackY - 3f), Offset(w, trackY - 3f), strokeWidth = 4f)

    // Main Train: Locomotive + 3 Coaches moving forward
    val baseTrainX = w * 0.58f
    val coachW = w * 0.22f
    val coachH = 46f

    for (idx in 0..2) {
        val cx = baseTrainX - idx * (coachW + 10f)
        val cy = trackY - coachH - 6f
        val isLoco = idx == 0

        drawRoundRect(
            color = Color(0xFFCFD8DC),
            topLeft = Offset(cx + 4f, cy - 5f),
            size = Size(coachW - 8f, 8f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        drawRoundRect(
            color = if (telemetry.train.liveryType == "WHITE_RED") Color(0xFFF5F7F8) else Color(0xFF0C7450),
            topLeft = Offset(cx, cy),
            size = Size(coachW, coachH),
            cornerRadius = CornerRadius(if (isLoco) 12f else 6f, 6f)
        )
        drawRect(
            color = Color(0xFFE53935),
            topLeft = Offset(cx, cy + coachH * 0.48f),
            size = Size(coachW, coachH * 0.22f)
        )
        for (win in 0..3) {
            drawRoundRect(
                color = if (isNight) Color(0xFFFFF59D) else Color(0xFF07221B),
                topLeft = Offset(cx + 10f + win * ((coachW - 24f) / 4f), cy + 8f),
                size = Size((coachW - 44f) / 4f, 12f),
                cornerRadius = CornerRadius(3f, 3f)
            )
        }
        val wheelPositions = listOf(cx + 18f, cx + coachW - 18f)
        wheelPositions.forEach { wx ->
            val wy = trackY - 6f
            drawCircle(Color(0xFF263238), radius = 8f, center = Offset(wx, wy))
            val angle = phase * 6.28f
            drawLine(
                color = SignalGold,
                start = Offset(wx - cos(angle) * 6f, wy - sin(angle) * 6f),
                end = Offset(wx + cos(angle) * 6f, wy + sin(angle) * 6f),
                strokeWidth = 2f
            )
        }
    }

    // Parallel Asphalt Highway Road below the railway embankment with moving vehicles!
    drawRect(
        color = Color(0xFF263238),
        topLeft = Offset(0f, roadY - 12f),
        size = Size(w, 28f)
    )
    drawLine(
        color = Color.White.copy(alpha = 0.7f),
        start = Offset(0f, roadY + 2f),
        end = Offset(w, roadY + 2f),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), phase * 60f)
    )
    // Moving Red Bus & White Car on the Highway
    val busX = w * ((1.2f - crossingPhase * 1.4f + 1f) % 1f)
    drawRoundRect(
        color = Color(0xFFE53935),
        topLeft = Offset(busX, roadY - 20f),
        size = Size(48f, 18f),
        cornerRadius = CornerRadius(4f, 4f)
    )
}

/**
 * 3. TOP-DOWN DOUBLE-LINE TELEMETRY RADAR SCENE
 */
private fun DrawScope.drawTopRadar3DScene(
    telemetry: LiveTrainTelemetry,
    phase: Float,
    crossingPhase: Float,
    showOppositeTrain: Boolean
) {
    val w = size.width
    val h = size.height
    drawRect(color = Color(0xFF041812))

    val gridStep = 40f
    var gx = 0f
    while (gx < w) {
        drawLine(Color(0x1826D092), Offset(gx, 0f), Offset(gx, h), strokeWidth = 1f)
        gx += gridStep
    }
    var gy = 0f
    while (gy < h) {
        drawLine(Color(0x1826D092), Offset(0f, gy), Offset(w, gy), strokeWidth = 1f)
        gy += gridStep
    }

    val upTrackY = h * 0.44f
    val downTrackY = h * 0.58f

    drawLine(Color(0xFF26D092), Offset(w * 0.08f, upTrackY), Offset(w * 0.92f, upTrackY), strokeWidth = 5f)
    drawLine(Color(0xFF00B8D4).copy(alpha = 0.7f), Offset(w * 0.08f, downTrackY), Offset(w * 0.92f, downTrackY), strokeWidth = 4f)

    val stops = telemetry.train.stops
    val count = stops.size.coerceAtLeast(2)
    stops.forEachIndexed { idx, _ ->
        val sx = w * 0.10f + (w * 0.80f) * (idx.toFloat() / (count - 1))
        val passed = idx <= telemetry.currentStationIndex
        drawCircle(
            color = if (passed) LiveGreen else Color(0xFF546E7A),
            radius = 8f,
            center = Offset(sx, upTrackY)
        )
    }

    val trainX = w * 0.10f + (w * 0.80f) * telemetry.overallProgress
    drawCircle(
        color = SignalGold.copy(alpha = 0.28f + 0.3f * phase),
        radius = 18f + 12f * phase,
        center = Offset(trainX, upTrackY)
    )
    drawRoundRect(
        color = SignalGold,
        topLeft = Offset(trainX - 18f, upTrackY - 8f),
        size = Size(36f, 16f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    if (showOppositeTrain) {
        val oppX = w * 0.88f - (w * 0.76f) * crossingPhase
        drawRoundRect(
            color = Color(0xFF00E5FF),
            topLeft = Offset(oppX - 16f, downTrackY - 7f),
            size = Size(32f, 14f),
            cornerRadius = CornerRadius(5f, 5f)
        )
    }
}
