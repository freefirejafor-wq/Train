package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BangladeshRailwayCatalog
import com.example.data.ChatMessageEntity
import com.example.data.LostFoundEntity
import com.example.ui.RailPulseUiState
import com.example.ui.theme.CrimsonExpress
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.SignalGold

@Composable
fun ScreenTopHeader(
    titleBn: String,
    subtitleBn: String,
    onBack: () -> Unit,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Surface(color = Color(0xFF04241A), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .testTag("btn_header_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = titleBn,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitleBn,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA7F3D0)
                    )
                }
            }
            trailingContent?.invoke()
        }
    }
}

/**
 * 1. লাইভ চ্যাট (Passenger Real-Time Community Chat & Status Sharing)
 */
@Composable
fun LiveChatScreen(
    uiState: RailPulseUiState,
    messages: List<ChatMessageEntity>,
    onBack: () -> Unit,
    onSendMessage: (String, String, String, String, Int) -> Unit
) {
    BackHandler { onBack() }

    var selectedFilterTrain by remember { mutableStateOf("ALL") }
    var senderName by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    var delayInput by remember { mutableIntStateOf(0) }

    val activeTrain = BangladeshRailwayCatalog.allTrains.find { it.trainNumber == uiState.selectedTrainNumber }
        ?: BangladeshRailwayCatalog.allTrains.first()
    val activeTel = uiState.allTelemetries[activeTrain.trainNumber]

    val filteredMessages = if (selectedFilterTrain == "ALL") {
        messages
    } else {
        messages.filter { it.trainNumber == selectedFilterTrain }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("live_chat_screen")
    ) {
        ScreenTopHeader(
            titleBn = "যাত্রী লাইভ চ্যাট ও আপডেট",
            subtitleBn = "ট্রেন কোথায় আছে বা কত মিনিট লেট সরাসরি অন্য যাত্রীদের জানান",
            onBack = onBack
        )

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = if (selectedFilterTrain == "ALL") EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50),
                modifier = Modifier.clickable { selectedFilterTrain = "ALL" }
            ) {
                Text(
                    text = "সব ট্রেন (${messages.size})",
                    color = if (selectedFilterTrain == "ALL") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
            BangladeshRailwayCatalog.allTrains.forEach { tr ->
                val selected = selectedFilterTrain == tr.trainNumber
                Surface(
                    color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { selectedFilterTrain = tr.trainNumber }
                ) {
                    Text(
                        text = "${tr.trainNumberBn} ${tr.nameBn}",
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredMessages, key = { it.id }) { msg ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg.senderName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = msg.trainNameBn,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = msg.messageBn,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📍 স্টেশন: ${msg.currentStationTag}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (msg.reportedDelayMin <= 0) "🟢 অন-টাইম" else "⏱️ ${msg.reportedDelayMin} মি. লেট",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (msg.reportedDelayMin <= 5) LiveGreen else CrimsonExpress
                            )
                        }
                    }
                }
            }
        }

        // Compose New Chat Message Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = senderName,
                        onValueChange = { senderName = it },
                        placeholder = { Text("আপনার নাম/বগি (যেমন: রাকিব, বগি-খ)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("${activeTrain.nameBn}-এর লাইভ আপডেট লিখুন...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_chat_message"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Button(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                val targetTrainNum = if (selectedFilterTrain == "ALL") activeTrain.trainNumber else selectedFilterTrain
                                onSendMessage(
                                    targetTrainNum,
                                    senderName,
                                    messageInput.trim(),
                                    activeTel?.lastStation?.nameBn ?: "কমলাপুর",
                                    delayInput
                                )
                                messageInput = ""
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("btn_send_chat_message"),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "পাঠান", tint = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * 2. স্টেশন বোর্ড (Digital LED Station Arrival & Departure Board)
 */
@Composable
fun StationBoardScreen(
    uiState: RailPulseUiState,
    onBack: () -> Unit,
    onSelectStation: (String) -> Unit,
    onOpenTrain3D: (String) -> Unit
) {
    BackHandler { onBack() }

    val selectedStation = BangladeshRailwayCatalog.allStations.find { it.code == uiState.selectedStationCode }
        ?: BangladeshRailwayCatalog.allStations.first()

    val stationTrains = BangladeshRailwayCatalog.allTrains.mapNotNull { train ->
        val stop = train.stops.find { it.stationCode == selectedStation.code }
        if (stop != null) Pair(train, stop) else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("station_board_screen")
    ) {
        ScreenTopHeader(
            titleBn = "ডিজিটাল স্টেশন বোর্ড",
            subtitleBn = "${selectedStation.nameBn} • কোন ট্রেন কয়টায় আসবে ও ছাড়বে",
            onBack = onBack
        )

        // Station Selector Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BangladeshRailwayCatalog.allStations.forEach { st ->
                val selected = st.code == selectedStation.code
                Surface(
                    color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { onSelectStation(st.code) }
                ) {
                    Text(
                        text = st.nameBn,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // LED Digital Board Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF071B14))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🚉 ${selectedStation.nameBn}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SignalGold
                    )
                    Text(
                        text = "মোট প্ল্যাটফর্ম: ${selectedStation.totalPlatforms}টি • মোট ট্রেন: ${stationTrains.size}টি",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA7F3D0)
                    )
                }
                Text(
                    text = uiState.currentBstClock,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = LiveGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(stationTrains) { (train, stop) ->
                val tel = uiState.allTelemetries[train.trainNumber]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTrain3D(train.trainNumber) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${train.nameBn} (${train.trainNumberBn})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${train.corridorBn} • বন্ধ: ${train.offDayBn}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = Color(0xFF0A4D36),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "প্ল্যাটফর্ম ${stop.platformNumber}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SignalGold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("আসবে (Arrival)", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = stop.arrivalTime,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column {
                                Text("ছাড়বে (Departure)", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = stop.departureTime,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonExpress
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("বর্তমান অবস্থান", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = tel?.lastStation?.nameBn ?: "",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. স্টেশন অবস্থা (Station Crowd Density, Facilities & Live Crowd Reporting)
 */
@Composable
fun StationStatusScreen(
    uiState: RailPulseUiState,
    onBack: () -> Unit,
    onSelectStation: (String) -> Unit,
    onReportCrowd: (String, String, Int) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("station_status_screen")
    ) {
        ScreenTopHeader(
            titleBn = "স্টেশন অবস্থা ও সুবিধাসমূহ",
            subtitleBn = "প্ল্যাটফর্মে ভিড়ের মাত্রা, ওয়েটিং রুম ও রেলওয়ে পুলিশ কন্টাক্ট",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(BangladeshRailwayCatalog.allStations) { st ->
                val customCrowd = uiState.customStationCrowds[st.code]
                val crowdLabel = customCrowd?.first ?: st.defaultCrowdLevelBn
                val crowdPct = customCrowd?.second ?: st.crowdPercentage
                val crowdColor = when {
                    crowdPct >= 75 -> CrimsonExpress
                    crowdPct >= 55 -> Color(0xFFD84315)
                    else -> LiveGreen
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${st.nameBn} (${st.code})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${st.divisionBn} • মোট প্ল্যাটফর্ম: ${st.totalPlatforms}টি",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = crowdColor.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "$crowdLabel ($crowdPct%)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = crowdColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { crowdPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = crowdColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Facilities Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (st.hasWifi) FacilityChip(Icons.Default.Wifi, "Wi-Fi")
                            if (st.hasAcWaitingRoom) FacilityChip(Icons.Default.AcUnit, "AC ওয়েটিং")
                            if (st.hasFoodCourt) FacilityChip(Icons.Default.Restaurant, "ফুড কোর্ট")
                            if (st.hasParking) FacilityChip(Icons.Default.LocalParking, "পার্কিং")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Crowd Report + GRP Call Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.clickable {
                                        onReportCrowd(st.code, "স্বাভাবিক", 38)
                                    }
                                ) {
                                    Text("স্বাভাবিক", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.clickable {
                                        onReportCrowd(st.code, "মাঝারি ভিড়", 65)
                                    }
                                ) {
                                    Text("মাঝারি", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.clickable {
                                        onReportCrowd(st.code, "অতিরিক্ত ভিড়", 90)
                                    }
                                ) {
                                    Text("অতিরিক্ত ভিড়", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }

                            TextButton(
                                onClick = {
                                    try {
                                        context.startActivity(
                                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:${st.grpPhone}"))
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("রেল পুলিশ", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FacilityChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

/**
 * 4. হারানো-পাওয়া (Lost & Found Hub with Add Post Dialog)
 */
@Composable
fun LostFoundScreen(
    posts: List<LostFoundEntity>,
    onBack: () -> Unit,
    onAddPost: (String, String, String, String, String, String, String) -> Unit,
    onDeletePost: (Int) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filtered = when (selectedTypeFilter) {
        "LOST" -> posts.filter { it.postType == "LOST" }
        "FOUND" -> posts.filter { it.postType == "FOUND" }
        else -> posts
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("lost_found_screen")
    ) {
        ScreenTopHeader(
            titleBn = "হারানো-পাওয়া সহায়তা কেন্দ্র",
            subtitleBn = "ট্রেনে বা স্টেশনে কিছু হারালে বা কুড়িয়ে পেলে এখানে জানান",
            onBack = onBack,
            trailingContent = {
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SignalGold, contentColor = Color(0xFF1F1300)),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_add_lost_found")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("পোস্ট করুন", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("ALL" to "সব পোস্ট", "LOST" to "হারিয়েছে (Lost)", "FOUND" to "পাওয়া গেছে (Found)").forEach { (key, label) ->
                val selected = selectedTypeFilter == key
                Surface(
                    color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { selectedTypeFilter = key }
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered, key = { it.id }) { item ->
                val isLost = item.postType == "LOST"
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isLost) CrimsonExpress.copy(alpha = 0.14f) else LiveGreen.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = if (isLost) "🔴 হারিয়েছে (LOST)" else "🟢 পাওয়া গেছে (FOUND)",
                                    color = if (isLost) CrimsonExpress else EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeletePost(item.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "মুছুন",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.itemTitleBn,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "🚆 ${item.trainNameBn} • 📍 ${item.stationNameBn} • ${item.coachSerialBn}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.detailsBn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.contactPhone}"))
                                    )
                                } catch (_: Exception) {
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("যোগাযোগ করুন: ${item.contactPhone}")
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var postType by remember { mutableStateOf("LOST") }
        var title by remember { mutableStateOf("") }
        var trainName by remember { mutableStateOf("সুবর্ণ এক্সপ্রেস (৭০১)") }
        var stationName by remember { mutableStateOf("কমলাপুর স্টেশন") }
        var coachName by remember { mutableStateOf("বগি - গ") }
        var phone by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("হারানো বা পাওয়া জিনিসের পোস্ট") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { postType = "LOST" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (postType == "LOST") CrimsonExpress else Color.Gray
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("হারিয়েছে") }
                        Button(
                            onClick = { postType = "FOUND" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (postType == "FOUND") EmeraldPrimary else Color.Gray
                            ),
                            modifier = Modifier.weight(1f)
                        ) { Text("পাওয়া গেছে") }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("জিনিসের নাম (যেমন: কালো ব্যাগ / মোবাইল)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = trainName,
                        onValueChange = { trainName = it },
                        label = { Text("ট্রেনের নাম") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("মোবাইল নম্বর") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("বিস্তারিত বিবরণ ও বগি নম্বর") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onAddPost(
                                postType,
                                title.trim(),
                                trainName.trim(),
                                stationName.trim(),
                                coachName.trim(),
                                phone.ifBlank { "01700-000000" },
                                details.ifBlank { "বিস্তারিত তথ্যের জন্য ফোনে যোগাযোগ করুন।" }
                            )
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("প্রকাশ করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

/**
 * 5. বগির অবস্থান ও ভাড়া (Interactive Coach Formation Visualizer + Ticket Fare Calculator)
 */
@Composable
fun CoachPositionScreen(
    uiState: RailPulseUiState,
    onBack: () -> Unit,
    onSelectTrain: (String) -> Unit
) {
    BackHandler { onBack() }

    val train = BangladeshRailwayCatalog.allTrains.find { it.trainNumber == uiState.selectedTrainNumber }
        ?: BangladeshRailwayCatalog.allTrains.first()
    var selectedCoachIndex by remember(train.trainNumber) { mutableIntStateOf(2) }
    val selectedCoach = train.coaches.getOrElse(selectedCoachIndex) { train.coaches.first() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("coach_position_screen")
    ) {
        ScreenTopHeader(
            titleBn = "বগির অবস্থান ও টিকিট ভাড়া",
            subtitleBn = "ইঞ্জিন থেকে ক, খ, গ, ঘ... প্রতিটি বগির সিরিয়াল ও সিট সংখ্যা",
            onBack = onBack
        )

        // Train Selector Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BangladeshRailwayCatalog.allTrains.forEach { tr ->
                val selected = tr.trainNumber == train.trainNumber
                Surface(
                    color = if (selected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { onSelectTrain(tr.trainNumber) }
                ) {
                    Text(
                        text = "${tr.trainNumberBn} ${tr.nameBn}",
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Interactive Horizontal Train Coach Track
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF06251B))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🚆 ${train.nameBn} (${train.trainNumberBn}) — বগির সিরিয়াল ম্যাপ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SignalGold
                        )
                        Text(
                            text = "যেকোনো বগির ওপর ট্যাপ করে সিট সংখ্যা ও ভাড়া দেখুন (বাম দিকে ইঞ্জিন ➔)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA7F3D0)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            train.coaches.forEachIndexed { idx, coach ->
                                val isSel = idx == selectedCoachIndex
                                val isLoco = idx == 0
                                Surface(
                                    color = when {
                                        isLoco -> CrimsonExpress
                                        isSel -> SignalGold
                                        coach.isAc -> Color(0xFF0E6BA8)
                                        else -> Color(0xFF0E7452)
                                    },
                                    shape = RoundedCornerShape(
                                        topStart = if (isLoco) 22.dp else 10.dp,
                                        bottomStart = if (isLoco) 12.dp else 10.dp,
                                        topEnd = 10.dp,
                                        bottomEnd = 10.dp
                                    ),
                                    modifier = Modifier
                                        .width(82.dp)
                                        .height(68.dp)
                                        .border(
                                            width = if (isSel) 2.5.dp else 1.dp,
                                            color = if (isSel) Color.White else Color.White.copy(alpha = 0.25f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedCoachIndex = idx }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = coach.serialBn,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel && !isLoco) Color(0xFF1F1300) else Color.White
                                        )
                                        Text(
                                            text = if (coach.seatCount > 0) "${coach.seatCount} সিট" else coach.serialEn,
                                            fontSize = 10.sp,
                                            color = if (isSel && !isLoco) Color(0xFF1F1300) else Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Selected Coach Detail Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "বগি: ${selectedCoach.serialBn} (${selectedCoach.serialEn}) • ইঞ্জিন থেকে #${selectedCoachIndex + 1} নং পজিশন",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (selectedCoach.isAc) {
                                Surface(
                                    color = Color(0xFF0277BD),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "❄️ এসি কোচ",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "শ্রেণি: ${selectedCoach.classTypeBn}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (selectedCoach.seatCount > 0) {
                                "মোট আসন সংখ্যা: ${selectedCoach.seatCount}টি • টিকিট মূল্য: ৳${selectedCoach.baseFareBdt} (অনলাইন চার্জ ৳২০ অতিরিক্ত)"
                            } else {
                                "এটি অপারেশনাল লোকোমোটিভ / পাওয়ার কোচ (যাত্রী আসন নেই)"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Full Coach List Table
            item {
                Text(
                    text = "সকল বগির তালিকা ও ভাড়ার চার্ট (${train.corridorBn})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(train.coaches) { coach ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                color = if (coach.isAc) Color(0xFF0277BD) else EmeraldPrimary,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = coach.serialBn,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = coach.classTypeBn,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (coach.seatCount > 0) "আসন: ${coach.seatCount}টি" else "ইঞ্জিন / সার্ভিস কার",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (coach.baseFareBdt > 0) {
                            Text(
                                text = "৳${coach.baseFareBdt}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6. রুট বিশ্লেষণ (Route Analytics, Punctuality & Speed Comparison)
 */
@Composable
fun RouteAnalysisScreen(
    uiState: RailPulseUiState,
    onBack: () -> Unit,
    onOpenTrain3D: (String) -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("route_analysis_screen")
    ) {
        ScreenTopHeader(
            titleBn = "রুট বিশ্লেষণ ও সময়ানুবর্তিতা রিপোর্ট",
            subtitleBn = "কোন ট্রেনে গড়ে কত মিনিট লেট হয়, সর্বোচ্চ গতি ও দূরত্বের পরিসংখ্যান",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(BangladeshRailwayCatalog.allTrains) { train ->
                val totalDistance = train.stops.last().distanceKm
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTrain3D(train.trainNumber) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${train.nameBn} (${train.trainNumberBn})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${train.corridorBn} • মোট দূরত্ব: $totalDistance কিমি • স্টপেজ: ${train.stops.size}টি",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = LiveGreen.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "${train.punctualityPercent}% অন-টাইম",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "সময়ানুবর্তিতা হার (${train.punctualityPercent}%) • গড় বিলম্ব: ${train.averageDelayMinutes} মিনিট • সর্বোচ্চ গতি: ${train.maxSpeedKmh} km/h",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { train.punctualityPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(50)),
                            color = if (train.punctualityPercent >= 92) LiveGreen else SignalGold,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}
