package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.BangladeshRailwayCatalog
import com.example.data.FavoriteTrainEntity
import com.example.data.StationAlarmEntity
import com.example.data.StationDirectoryInfo
import com.example.ui.RailPulseUiState
import com.example.ui.components.Live3DTrainStage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.SignalGold
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LiveTrain3DScreen(
    uiState: RailPulseUiState,
    favorites: List<FavoriteTrainEntity>,
    alarms: List<StationAlarmEntity>,
    onBack: () -> Unit,
    onSelectTrain: (String) -> Unit,
    onCycleCamera: () -> Unit,
    onCycleEnvironment: () -> Unit,
    onToggleOppositeTrain: () -> Unit,
    onAdjustTimeShift: (Int) -> Unit,
    onSetOnBoardGps: (Boolean, Int?) -> Unit,
    onReportDelay: (String, Int, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onAddStationAlarm: (StationDirectoryInfo, Int) -> Unit,
    onOpenCoachPosition: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var showDelayDialog by remember { mutableStateOf(false) }
    var showAlarmDialog by remember { mutableStateOf(false) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var announcementBanner by remember { mutableStateOf<String?>(null) }

    val train = BangladeshRailwayCatalog.allTrains.find { it.trainNumber == uiState.selectedTrainNumber }
        ?: BangladeshRailwayCatalog.allTrains.first()
    val telemetry = uiState.allTelemetries[train.trainNumber]
        ?: BangladeshRailwayCatalog.calculateLiveTelemetry(train)
    val isFavorite = favorites.any { it.trainNumber == train.trainNumber }

    DisposableEffect(Unit) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale("bn", "BD")
            }
        }
        ttsEngine = instance
        onDispose {
            instance?.stop()
            instance?.shutdown()
        }
    }

    // Real Android GPS Speed Listener when On-Board GPS is active
    DisposableEffect(uiState.isOnBoardGpsActive) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (location.hasSpeed()) {
                    val speedKmh = (location.speed * 3.6f).roundToInt().coerceAtLeast(0)
                    onSetOnBoardGps(true, if (speedKmh > 0) speedKmh else telemetry.currentSpeedKmh)
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        if (uiState.isOnBoardGpsActive && locationManager != null) {
            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasFine) {
                try {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        2000L,
                        2f,
                        listener
                    )
                } catch (_: Exception) {
                }
            }
        }
        onDispose {
            try {
                locationManager?.removeUpdates(listener)
            } catch (_: Exception) {
            }
        }
    }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            onSetOnBoardGps(true, telemetry.currentSpeedKmh.coerceAtLeast(78))
        } else {
            onSetOnBoardGps(true, telemetry.currentSpeedKmh.coerceAtLeast(75))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("live_3d_screen_list"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Top Bar + Train Switcher
        item {
            Surface(
                color = Color(0xFF04241A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .testTag("btn_back_from_3d")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "ফিরে যান",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${train.nameBn} (${train.trainNumberBn})",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${train.corridorBn} • সাপ্তাহিক বন্ধ: ${train.offDayBn}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Station Voice Announcement Button (🔊 মাইক ঘোষণা)
                            IconButton(
                                onClick = {
                                    val speechText = if (telemetry.isOffToday) {
                                        "যাত্রী সাধারণের অবগতির জন্য জানানো যাচ্ছে যে, ${train.trainNumberBn} নম্বর ${train.nameBn} ট্রেনটি আজ ${train.offDayBn} উপলক্ষ্যে সাপ্তাহিক বন্ধ রয়েছে।"
                                    } else {
                                        "যাত্রী সাধারণের অবগতির জন্য জানানো যাচ্ছে যে, ${train.trainNumberBn} নম্বর ${train.nameBn} ট্রেনটি বর্তমানে ${telemetry.lastStation.nameBn} অতিক্রম করে ঘন্টায় ${telemetry.currentSpeedKmh} কিলোমিটার বেগে ${telemetry.nextStation.nameBn} স্টেশনের দিকে অগ্রসর হচ্ছে। ট্রেনটি ${telemetry.expectedNextArrivalFormatted} মিনিটে ${telemetry.nextStation.platformNumber} নম্বর প্ল্যাটফর্মে পৌঁছাবে।"
                                    }
                                    announcementBanner = "🔊 মাইক ঘোষণা: \"$speechText\""
                                    try {
                                        ttsEngine?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "station_announce")
                                    } catch (_: Exception) {
                                    }
                                },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SignalGold.copy(alpha = 0.2f))
                                    .testTag("btn_station_voice_announce")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "স্টেশন মাইক ঘোষণা",
                                    tint = SignalGold
                                )
                            }

                            IconButton(
                                onClick = { onToggleFavorite(train.trainNumber) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .testTag("btn_toggle_fav_train")
                            ) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "প্রিয় ট্রেন",
                                    tint = if (isFavorite) SignalGold else Color.White
                                )
                            }
                        }
                    }

                    if (!announcementBanner.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF0E4934),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, SignalGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { announcementBanner = null }
                        ) {
                            Text(
                                text = announcementBanner ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFF59D),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Horizontal Train Selector Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BangladeshRailwayCatalog.allTrains.forEach { item ->
                            val selected = item.trainNumber == train.trainNumber
                            Surface(
                                color = if (selected) SignalGold else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { onSelectTrain(item.trainNumber) }
                            ) {
                                Text(
                                    text = "${item.trainNumberBn} ${item.nameBn.substringBefore(" (")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color(0xFF1F1300) else Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Full 3D Stage
                    Live3DTrainStage(
                        telemetry = telemetry,
                        cameraMode = uiState.camera3DMode,
                        environmentTheme = uiState.environment3DTheme,
                        showOppositeTrain = uiState.showOppositeTrainIn3D,
                        stageHeight = 295.dp,
                        onCycleCamera = onCycleCamera,
                        onCycleEnvironment = onCycleEnvironment,
                        onToggleOppositeTrain = onToggleOppositeTrain,
                        onExpandFullScreen = null
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Interactive 3D Time Scrubber
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.simulationShiftMinutes == 0) {
                                "🟢 লাইভ ঘড়ির সময় (${uiState.currentBstClock})"
                            } else {
                                "⏩ 3D প্রিভিউ শিফট: ${if (uiState.simulationShiftMinutes > 0) "+" else ""}${uiState.simulationShiftMinutes} মিনিট"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA7F3D0)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { onAdjustTimeShift(-20) }
                            ) {
                                Text(
                                    text = "-২০ মি.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = SignalGold,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { onAdjustTimeShift(0) }
                            ) {
                                Text(
                                    text = "লাইভ (Now)",
                                    color = Color(0xFF1F1300),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.clickable { onAdjustTimeShift(20) }
                            ) {
                                Text(
                                    text = "+২০ মি.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. 4 Core Answer Cards: কোথায় আছে, কয়টায় আসবে, কয়টায় ছাড়বে, লাইভ স্পিড ও দূরত্ব
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "লাইভ টেলিমেট্রি ও সময়সূচি সারাংশ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = telemetry.statusBadgeBn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (!telemetry.liveServerReportBn.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "📡 রিয়েল-টাইম সার্ভার রিপোর্ট: ${telemetry.liveServerReportBn}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = EmeraldPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val syncInfo = telemetry.liveServerUpdatedAgoBn?.let { "• $it" } ?: ""
                        val delayInfo = if (telemetry.delayMinutes > 0) "বিলম্ব: ${telemetry.delayMinutes} মিনিট" else "নির্ধারিত সময়ে"
                        Text(
                            text = "নির্ধারিত ছাড়ে: ${telemetry.scheduledDepartureFormatted} • পৌঁছায়: ${telemetry.scheduledArrivalFormatted} • $delayInfo $syncInfo",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryStatCard(
                        modifier = Modifier.weight(1f),
                        labelBn = "ট্রেন এখন কোথায় আছে",
                        valueBn = when {
                            telemetry.isOffToday -> "আজ বন্ধ (${train.offDayBn})"
                            !telemetry.isRunningNow -> "${telemetry.lastStation.nameBn} স্টেশনে"
                            telemetry.isAtStation -> "${telemetry.lastStation.nameBn} স্টেশনে"
                            else -> "${telemetry.lastStation.nameBn} পার হয়ে"
                        },
                        subValueBn = "প্ল্যাটফর্ম নং ${telemetry.lastStation.platformNumber} • জিপিএস: ${String.format("%.2f", telemetry.currentLat)}°N",
                        accent = EmeraldPrimary
                    )
                    TelemetryStatCard(
                        modifier = Modifier.weight(1f),
                        labelBn = "পরবর্তী স্টেশন (${telemetry.nextStation.nameBn})",
                        valueBn = if (telemetry.isRunningNow) {
                            "আসবে: ${telemetry.expectedNextArrivalFormatted}"
                        } else {
                            "ছাড়ে: ${telemetry.scheduledDepartureFormatted}"
                        },
                        subValueBn = if (telemetry.isRunningNow) {
                            "পৌঁছাতে বাকি: ${telemetry.etaNextStationMinutes} মিনিট (${String.format("%.1f", telemetry.distanceRemainingToNextKm)} কিমি)"
                        } else {
                            "গন্তব্যে পৌঁছানোর সময়: ${telemetry.scheduledArrivalFormatted}"
                        },
                        accent = Color(0xFF1565C0)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryStatCard(
                        modifier = Modifier.weight(1f),
                        labelBn = "স্টেশন ও বিলম্ব তথ্য",
                        valueBn = if (telemetry.delayMinutes > 0) "বিলম্ব: ${telemetry.delayMinutes} মিনিট" else "নির্ধারিত সময়ে",
                        subValueBn = "বিরতি: ${telemetry.nextStation.haltMinutes} মিনিট • গন্তব্যে: ${telemetry.expectedFinalArrivalFormatted}",
                        accent = Color(0xFFD84315)
                    )
                    TelemetryStatCard(
                        modifier = Modifier.weight(1f),
                        labelBn = "লাইভ স্পিড ও ক্রসিং ট্রেন",
                        valueBn = "${telemetry.currentSpeedKmh} কিমি/ঘণ্টা",
                        subValueBn = "বিপরীত ট্রেন: ${train.oppositeTrainNameBn}",
                        accent = Color(0xFF6A1B9A)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Interactive Passenger Action Bar (On-Board GPS, Delay Report, Wake-up Alarm, 16318 SMS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            if (uiState.isOnBoardGpsActive) {
                                onSetOnBoardGps(false, null)
                            } else {
                                val hasFine = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasFine) {
                                    onSetOnBoardGps(true, telemetry.currentSpeedKmh.coerceAtLeast(82))
                                } else {
                                    gpsPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_onboard_gps"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isOnBoardGpsActive) LiveGreen else EmeraldPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.isOnBoardGpsActive) "GPS চালু" else "আমি ট্রেনে",
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = { showDelayDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_report_delay"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("লেট আপডেট", fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showAlarmDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_station_alarm"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.AlarmAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("অ্যালার্ম", fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:16318")).apply {
                                    putExtra("sms_body", "TR ${train.trainNumber}")
                                }
                                context.startActivity(smsIntent)
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_official_sms_16318"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("16318 SMS", fontSize = 11.sp, maxLines = 1)
                    }
                }

                // Active Alarms Strip if any
                val trainAlarms = alarms.filter { it.trainNumber == train.trainNumber }
                if (trainAlarms.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    trainAlarms.forEach { alarm ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "অ্যালার্ম চালু: ${alarm.stationNameBn} স্টেশনের ${alarm.alertBeforeKm} কিমি আগে ফোনে ভাইব্রেশন বাজবে",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Station-by-Station Vertical Timeline
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "পূর্ণাঙ্গ স্টেশন রুট ও লাইভ অবস্থান (${train.stops.size}টি স্টপেজ)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onOpenCoachPosition) {
                    Text("বগির অবস্থান দেখুন ➔")
                }
            }
        }

        itemsIndexed(train.stops) { index, stop ->
            val isPassed = index < telemetry.currentStationIndex ||
                (index == telemetry.currentStationIndex && !telemetry.isAtStation)
            val isCurrentStation = index == telemetry.currentStationIndex && telemetry.isAtStation
            val isTrainBetweenThisAndNext = index == telemetry.currentStationIndex && !telemetry.isAtStation

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isCurrentStation -> MaterialTheme.colorScheme.primaryContainer
                            isPassed -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentStation) 4.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isCurrentStation -> Icons.Default.DirectionsRailway
                                isPassed -> Icons.Default.CheckCircle
                                else -> Icons.Default.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            tint = when {
                                isCurrentStation -> EmeraldPrimary
                                isPassed -> LiveGreen
                                else -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stop.nameBn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "প্ল্যাটফর্ম ${stop.platformNumber} • ${stop.distanceKm} কিমি",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "আসবে: ${stop.arrivalTime} • ছাড়বে: ${stop.departureTime}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (stop.haltMinutes > 0) {
                                        "বিরতি: ${stop.haltMinutes} মিনিট"
                                    } else if (index == 0) {
                                        "যাত্রা শুরু"
                                    } else {
                                        "চূড়ান্ত গন্তব্য"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (isTrainBetweenThisAndNext && index < train.stops.lastIndex) {
                    val nextStop = train.stops[index + 1]
                    Surface(
                        color = Color(0xFF073B2A),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 12.dp)
                            .border(1.dp, SignalGold, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🚄 ট্রেন এখন এখানে চলছে (${telemetry.currentSpeedKmh} km/h)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SignalGold
                                )
                                Text(
                                    text = "${(telemetry.segmentProgress * 100).roundToInt()}% সম্পন্ন",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { telemetry.segmentProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = LiveGreen,
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "পরবর্তী স্টেশন ${nextStop.nameBn} পৌঁছাতে আর ${String.format("%.1f", telemetry.distanceRemainingToNextKm)} কিমি (${telemetry.etaNextStationMinutes} মিনিট) বাকি",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB9F6CA)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDelayDialog) {
        AlertDialog(
            onDismissRequest = { showDelayDialog = false },
            title = { Text("ট্রেনের বর্তমান লেট (Delay) রিপোর্ট করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${train.nameBn} (${train.trainNumberBn}) বর্তমানে কত মিনিট বিলম্বে চলছে তা অন্য যাত্রীদের জানান:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val options = listOf(0 to "একদম অন-টাইম (০ মিনিট)", 5 to "৫ মিনিট লেট", 15 to "১৫ মিনিট লেট", 30 to "৩০ মিনিট লেট", 45 to "৪৫ মিনিট লেট")
                    options.forEach { (minVal, label) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onReportDelay(train.trainNumber, minVal, telemetry.lastStation.nameBn)
                                    showDelayDialog = false
                                }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDelayDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    if (showAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showAlarmDialog = false },
            title = { Text("🔔 গন্তব্য স্টেশন ওয়েক-আপ অ্যালার্ম") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "কোন স্টেশনের ৫ কিমি আগে পৌঁছালে আপনার ফোনে অ্যালার্ম ও ভাইব্রেশন বাজবে সিলেক্ট করুন:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    train.stops.drop(1).forEach { stop ->
                        val stationInfo = BangladeshRailwayCatalog.allStations.find { it.code == stop.stationCode }
                            ?: BangladeshRailwayCatalog.allStations.first()
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    try {
                                        val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                                        vib?.vibrate(VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } catch (_: Exception) {
                                    }
                                    onAddStationAlarm(stationInfo, 5)
                                    showAlarmDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(stop.nameBn, fontWeight = FontWeight.Bold)
                                Text("আসবে: ${stop.arrivalTime}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAlarmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun TelemetryStatCard(
    modifier: Modifier = Modifier,
    labelBn: String,
    valueBn: String,
    subValueBn: String,
    accent: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = labelBn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = valueBn,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValueBn,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
