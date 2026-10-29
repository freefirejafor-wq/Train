package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppScreen
import com.example.data.BangladeshRailwayCatalog
import com.example.data.FavoriteTrainEntity
import com.example.data.FleetFilterMode
import com.example.data.SearchTabMode
import com.example.ui.RailPulseUiState
import com.example.ui.components.Live3DTrainStage
import com.example.ui.theme.CrimsonExpress
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.SignalGold
import com.example.ui.theme.TelemetryBlue
import com.example.ui.theme.TelemetryCyan
import kotlin.math.roundToInt

private data class BentoFeatureItem(
    val titleBn: String,
    val subtitleBn: String,
    val badgeBn: String,
    val icon: ImageVector,
    val accentColor: Color,
    val containerColor: Color,
    val targetScreen: AppScreen,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: RailPulseUiState,
    favorites: List<FavoriteTrainEntity>,
    onNavigate: (AppScreen) -> Unit,
    onSelectSearchTab: (SearchTabMode) -> Unit,
    onUpdateQuery: (String) -> Unit,
    onSelectTrainAndOpen3D: (String) -> Unit,
    onSelectTrainPreview: (String) -> Unit,
    onSetRouteStations: (String, String) -> Unit,
    onSwapRouteStations: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    onCycleCamera3D: () -> Unit,
    onCycleEnvironment3D: () -> Unit,
    onToggleOppositeTrain3D: () -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var showFavoritesSheet by remember { mutableStateOf(false) }
    var activeFleetModalFilter by remember { mutableStateOf<FleetFilterMode?>(null) }
    var voiceFeedbackText by remember { mutableStateOf<String?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                voiceFeedbackText = "শোনা হয়েছে: \"$spoken\""
                onUpdateQuery(spoken)
                val matched = BangladeshRailwayCatalog.allTrains.find {
                    spoken.contains(it.trainNumber) ||
                        spoken.contains(it.trainNumberBn) ||
                        spoken.contains(it.nameBn.substringBefore(" ")) ||
                        it.nameEn.contains(spoken, ignoreCase = true)
                }
                if (matched != null) {
                    onSelectTrainAndOpen3D(matched.trainNumber)
                }
            }
        }
    }

    val activeTrain = BangladeshRailwayCatalog.allTrains.find { it.trainNumber == uiState.selectedTrainNumber }
        ?: BangladeshRailwayCatalog.allTrains.first()
    val activeTelemetry = uiState.allTelemetries[activeTrain.trainNumber]
        ?: BangladeshRailwayCatalog.calculateLiveTelemetry(activeTrain)

    // Live Fleet Statistics (কয়টা ট্রেন চলতেছে, কয়টা স্টেশনে/অপেক্ষমাণ, কয়টা আজ বন্ধ)
    val allTelemetryList = remember(uiState.allTelemetries) {
        if (uiState.allTelemetries.isNotEmpty()) {
            uiState.allTelemetries.values.toList()
        } else {
            BangladeshRailwayCatalog.allTrains.map { BangladeshRailwayCatalog.calculateLiveTelemetry(it) }
        }
    }
    val totalTrainsCount = allTelemetryList.size
    val offTodayCount = allTelemetryList.count { it.isOffToday }
    val runningNowCount = allTelemetryList.count { it.isRunningNow }
    val waitingOrStationCount = (totalTrainsCount - offTodayCount - runningNowCount).coerceAtLeast(0)
    val todayNameBn = activeTelemetry.todayNameBn

    val mainBentoFeatures = listOf(
        BentoFeatureItem(
            titleBn = "ট্রেন লোকেটর",
            subtitleBn = "মাল্টি-সার্ভার লাইভ জিপিএস ও রেডার ম্যাপ",
            badgeBn = "LIVE GPS",
            icon = Icons.Default.LocationOn,
            accentColor = Color(0xFF00C853),
            containerColor = Color(0xFFE8F8EE),
            targetScreen = AppScreen.TRAIN_LOCATOR,
            testTag = "feature_train_locator"
        ),
        BentoFeatureItem(
            titleBn = "লাইভ স্ট্যাটাস",
            subtitleBn = "3D রানিং ভিউ, স্পিড ও স্টেশন সময়",
            badgeBn = "3D VIEW",
            icon = Icons.Default.DirectionsRailway,
            accentColor = Color(0xFF1565C0),
            containerColor = Color(0xFFE7F1FF),
            targetScreen = AppScreen.LIVE_3D_STATUS,
            testTag = "feature_live_status"
        ),
        BentoFeatureItem(
            titleBn = "লাইভ চ্যাট",
            subtitleBn = "যাত্রীদের তাৎক্ষণিক আপডেট ও মেসেজ",
            badgeBn = "CHAT",
            icon = Icons.AutoMirrored.Filled.Chat,
            accentColor = Color(0xFF00838F),
            containerColor = Color(0xFFE4F7F9),
            targetScreen = AppScreen.LIVE_CHAT,
            testTag = "feature_live_chat"
        ),
        BentoFeatureItem(
            titleBn = "স্টেশন বোর্ড",
            subtitleBn = "কয়টায় আসবে ও কয়টায় ছাড়বে",
            badgeBn = "LED BOARD",
            icon = Icons.Default.Tv,
            accentColor = Color(0xFFD84315),
            containerColor = Color(0xFFFFEFEA),
            targetScreen = AppScreen.STATION_BOARD,
            testTag = "feature_station_board"
        ),
        BentoFeatureItem(
            titleBn = "স্টেশন অবস্থা",
            subtitleBn = "প্ল্যাটফর্ম ভিড় ও স্টেশন সুবিধা",
            badgeBn = "CROWD",
            icon = Icons.Default.Storefront,
            accentColor = Color(0xFF6A1B9A),
            containerColor = Color(0xFFF3E8FF),
            targetScreen = AppScreen.STATION_STATUS,
            testTag = "feature_station_status"
        ),
        BentoFeatureItem(
            titleBn = "হারানো-পাওয়া",
            subtitleBn = "ট্রেনে হারানো জিনিস খোঁজা ও পোস্ট",
            badgeBn = "HELP",
            icon = Icons.Default.FindInPage,
            accentColor = Color(0xFF283593),
            containerColor = Color(0xFFEAEEFF),
            targetScreen = AppScreen.LOST_AND_FOUND,
            testTag = "feature_lost_found"
        ),
        BentoFeatureItem(
            titleBn = "বগির অবস্থান",
            subtitleBn = "ক, খ, গ বগির সিরিয়াল ও টিকিট ভাড়া",
            badgeBn = "COACH MAP",
            icon = Icons.Default.ViewCarousel,
            accentColor = Color(0xFF00695C),
            containerColor = Color(0xFFE5F6F3),
            targetScreen = AppScreen.COACH_POSITION,
            testTag = "feature_coach_position"
        ),
        BentoFeatureItem(
            titleBn = "রুট বিশ্লেষণ",
            subtitleBn = "গড় লেট, দূরত্ব ও স্পিড গ্রাফ",
            badgeBn = "STATS",
            icon = Icons.Default.Analytics,
            accentColor = Color(0xFFC62828),
            containerColor = Color(0xFFFFEBEB),
            targetScreen = AppScreen.ROUTE_ANALYSIS,
            testTag = "feature_route_analysis"
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Hero Cockpit Header + Live Fleet Counter (কয়টা চলতেছে / কয়টা অফ) + 3D Stage
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF042218),
                                Color(0xFF083D2B),
                                Color(0xFF0C563D)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Column {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(LiveGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "RailPulse BD • 3D Radar",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "আজ $todayNameBn • ${uiState.currentBstClock}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = Color(0xFFA7F3D0)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color(0xFF124E39),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .border(1.dp, SignalGold.copy(alpha = 0.6f), RoundedCornerShape(50))
                                    .clickable { showFavoritesSheet = true }
                                    .testTag("btn_open_favorites")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "প্রিয় ট্রেন",
                                        tint = SignalGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "প্রিয় ট্রেন (${favorites.size})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = onToggleDarkTheme,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .testTag("btn_toggle_theme")
                            ) {
                                Icon(
                                    imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "থিম পরিবর্তন",
                                    tint = SignalGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // LIVE FLEET COUNTER BAR: কয়টা ট্রেন চলতেছে, কয়টা স্টেশনে, কয়টা আজ অফ (ট্যাপ করলে তালিকা খুলবে!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FleetCounterPill(
                            modifier = Modifier.weight(1f),
                            count = runningNowCount,
                            labelBn = "এখন চলমান",
                            dotColor = LiveGreen,
                            onClick = { activeFleetModalFilter = FleetFilterMode.RUNNING_NOW },
                            tag = "counter_running_now"
                        )
                        FleetCounterPill(
                            modifier = Modifier.weight(1f),
                            count = waitingOrStationCount,
                            labelBn = "স্টেশনে/অপেক্ষায়",
                            dotColor = SignalGold,
                            onClick = { activeFleetModalFilter = FleetFilterMode.WAITING_OR_STATION },
                            tag = "counter_waiting"
                        )
                        FleetCounterPill(
                            modifier = Modifier.weight(1f),
                            count = offTodayCount,
                            labelBn = "আজ বন্ধ ($todayNameBn)",
                            dotColor = Color(0xFFFF5252),
                            onClick = { activeFleetModalFilter = FleetFilterMode.OFF_TODAY },
                            tag = "counter_off_today"
                        )
                        FleetCounterPill(
                            modifier = Modifier.weight(1f),
                            count = totalTrainsCount,
                            labelBn = "মোট ট্রেন",
                            dotColor = TelemetryCyan,
                            onClick = { activeFleetModalFilter = FleetFilterMode.ALL },
                            tag = "counter_total_trains"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live 3D Interactive Stage with Roadside Highway, Trees, Bridges & Crossing Train
                    Live3DTrainStage(
                        telemetry = activeTelemetry,
                        cameraMode = uiState.camera3DMode,
                        environmentTheme = uiState.environment3DTheme,
                        showOppositeTrain = uiState.showOppositeTrainIn3D,
                        stageHeight = 235.dp,
                        onCycleCamera = onCycleCamera3D,
                        onCycleEnvironment = onCycleEnvironment3D,
                        onToggleOppositeTrain = onToggleOppositeTrain3D,
                        onExpandFullScreen = { onNavigate(AppScreen.LIVE_3D_STATUS) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Train Switcher Strip (All 226 Trains starting with Titas Commuter & Top Intercity)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BangladeshRailwayCatalog.allTrains.take(40).forEach { train ->
                            val isSelected = train.trainNumber == activeTrain.trainNumber
                            val tel = uiState.allTelemetries[train.trainNumber]
                            val isOff = tel?.isOffToday == true
                            Surface(
                                color = when {
                                    isSelected -> SignalGold
                                    isOff -> Color(0x55B71C1C)
                                    else -> Color.White.copy(alpha = 0.12f)
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .clickable { onSelectTrainPreview(train.trainNumber) }
                                    .testTag("quick_train_chip_${train.trainNumber}")
                            ) {
                                Text(
                                    text = "${train.trainNumberBn} ${train.nameBn.substringBefore(" (")}${if (isOff) " (আজ বন্ধ)" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF1F1400) else Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Smart 3-Mode Search Deck ("ট্রেন", "রুট", "বলে খুঁজুন") + Instant Train Location & 3D Route Preview
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SearchModeTabButton(
                            modifier = Modifier.weight(1f),
                            selected = uiState.searchTabMode == SearchTabMode.TRAIN,
                            icon = Icons.Default.Train,
                            labelBn = "ট্রেন",
                            onClick = { onSelectSearchTab(SearchTabMode.TRAIN) },
                            tag = "tab_search_train"
                        )
                        SearchModeTabButton(
                            modifier = Modifier.weight(1f),
                            selected = uiState.searchTabMode == SearchTabMode.ROUTE,
                            icon = Icons.Default.LocationOn,
                            labelBn = "রুট",
                            onClick = { onSelectSearchTab(SearchTabMode.ROUTE) },
                            tag = "tab_search_route"
                        )
                        SearchModeTabButton(
                            modifier = Modifier.weight(1f),
                            selected = uiState.searchTabMode == SearchTabMode.VOICE,
                            icon = Icons.Default.Mic,
                            labelBn = "বলে খুঁজুন",
                            onClick = { onSelectSearchTab(SearchTabMode.VOICE) },
                            tag = "tab_search_voice"
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (uiState.searchTabMode) {
                        SearchTabMode.TRAIN -> {
                            var selectedCategoryFilter by remember { mutableStateOf("ALL") }

                            OutlinedTextField(
                                value = uiState.trainSearchQuery,
                                onValueChange = onUpdateQuery,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_train_search"),
                                placeholder = {
                                    Text("নাম, নম্বর বা স্টেশন দিন — যেমন তিতাস, ৩৩, ৭০১, সুবর্ণ")
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "খুঁজুন")
                                },
                                trailingIcon = {
                                    if (uiState.trainSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { onUpdateQuery("") }) {
                                            Icon(Icons.Default.Close, contentDescription = "মুছুন")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Category Quick Filter Strip (সব ট্রেন | তিতাস ও কমিউটার | আন্তঃনগর | মেইল ও শাটল)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val categories = listOf(
                                    "ALL" to "সব ট্রেন (${BangladeshRailwayCatalog.allTrains.size})",
                                    "COMMUTER" to "তিতাস ও কমিউটার",
                                    "INTERCITY" to "আন্তঃনগর এক্সপ্রেস",
                                    "MAIL" to "মেইল ও শাটল"
                                )
                                categories.forEach { (catKey, catLabel) ->
                                    val isCatSelected = selectedCategoryFilter == catKey
                                    Surface(
                                        color = if (isCatSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.clickable {
                                            selectedCategoryFilter = catKey
                                        }
                                    ) {
                                        Text(
                                            text = catLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontSize = 11.sp,
                                            color = if (isCatSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            val filteredTrains = remember(uiState.trainSearchQuery, selectedCategoryFilter) {
                                val q = uiState.trainSearchQuery.trim()
                                val baseList = when (selectedCategoryFilter) {
                                    "COMMUTER" -> BangladeshRailwayCatalog.allTrains.filter {
                                        it.nameBn.contains("কমিউটার") || it.nameBn.contains("তিতাস") || it.nameEn.contains("Commuter", ignoreCase = true)
                                    }
                                    "INTERCITY" -> BangladeshRailwayCatalog.allTrains.filter {
                                        (it.trainNumber.toIntOrNull() ?: 0) >= 700
                                    }
                                    "MAIL" -> BangladeshRailwayCatalog.allTrains.filter {
                                        val num = it.trainNumber.toIntOrNull() ?: 0
                                        num in 1..200 && (it.nameBn.contains("মেইল") || it.nameBn.contains("শাটল") || it.nameBn.contains("এক্সপ্রেস"))
                                    }
                                    else -> BangladeshRailwayCatalog.allTrains
                                }

                                if (q.isEmpty()) {
                                    if (selectedCategoryFilter == "ALL") {
                                        // Show Titas Commuter + popular trains by default so user sees instant updates
                                        baseList.take(8)
                                    } else {
                                        baseList.take(25)
                                    }
                                } else {
                                    BangladeshRailwayCatalog.allTrains.filter { tr ->
                                        tr.trainNumber.contains(q, ignoreCase = true) ||
                                            tr.trainNumberBn.contains(q) ||
                                            tr.nameBn.contains(q, ignoreCase = true) ||
                                            tr.nameEn.contains(q, ignoreCase = true) ||
                                            tr.corridorBn.contains(q, ignoreCase = true) ||
                                            tr.stops.any { st ->
                                                st.nameBn.contains(q, ignoreCase = true) ||
                                                    st.nameEn.contains(q, ignoreCase = true) ||
                                                    st.stationCode.equals(q, ignoreCase = true)
                                            }
                                    }.take(40)
                                }
                            }

                            AnimatedVisibility(visible = filteredTrains.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.padding(top = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filteredTrains.forEach { train ->
                                        val tel = uiState.allTelemetries[train.trainNumber]
                                            ?: BangladeshRailwayCatalog.calculateLiveTelemetry(train)
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    onSelectTrainPreview(train.trainNumber)
                                                }
                                                .testTag("search_result_${train.trainNumber}")
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "${train.nameBn} (${train.trainNumberBn})",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = tel.statusBadgeBn,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = when {
                                                                tel.isOffToday -> CrimsonExpress
                                                                tel.isRunningNow -> EmeraldPrimary
                                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                            },
                                                            fontWeight = if (tel.isRunningNow) FontWeight.SemiBold else FontWeight.Normal
                                                        )
                                                    }
                                                    Surface(
                                                        color = when {
                                                            tel.isOffToday -> CrimsonExpress
                                                            tel.isRunningNow -> EmeraldPrimary
                                                            else -> Color(0xFF455A64)
                                                        },
                                                        shape = RoundedCornerShape(50),
                                                        modifier = Modifier.clickable {
                                                            onSelectTrainAndOpen3D(train.trainNumber)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = when {
                                                                tel.isOffToday -> "আজ বন্ধ • 3D"
                                                                !tel.isRunningNow -> "স্টেশনে • 3D ➔"
                                                                tel.isAtStation -> "স্টেশনে হল্ট • 3D ➔"
                                                                else -> "${tel.currentSpeedKmh} km/h • 3D ➔"
                                                            },
                                                            fontFamily = JetBrainsMonoFontFamily,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                        )
                                                    }
                                                }
                                                if (!tel.liveServerReportBn.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "📡 ${tel.liveServerReportBn}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = EmeraldPrimary
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LinearProgressIndicator(
                                                    progress = { tel.overallProgress },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(RoundedCornerShape(50)),
                                                    color = when {
                                                        tel.isOffToday -> CrimsonExpress
                                                        tel.isRunningNow -> LiveGreen
                                                        else -> SignalGold
                                                    },
                                                    trackColor = MaterialTheme.colorScheme.surface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val delayNote = if (tel.delayMinutes > 0) "বিলম্ব: ${tel.delayMinutes} মিনিট • " else "নির্ধারিত সময়ে • "
                                                Text(
                                                    text = "${delayNote}ছাড়ে: ${tel.scheduledDepartureFormatted} • পৌঁছায়: ${tel.scheduledArrivalFormatted} • বন্ধ: ${train.offDayBn}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Button to open full 226-train searchable directory modal
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activeFleetModalFilter = FleetFilterMode.ALL }
                                    .testTag("btn_open_all_trains_modal")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📋 সব ${BangladeshRailwayCatalog.allTrains.size}টি ট্রেনের সম্পূর্ণ লাইভ তালিকা দেখুন",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    val q = uiState.trainSearchQuery.trim()
                                    val target = BangladeshRailwayCatalog.allTrains.find {
                                        it.trainNumber.contains(q, ignoreCase = true) ||
                                            it.trainNumberBn.contains(q) ||
                                            it.nameBn.contains(q) ||
                                            it.nameEn.contains(q, ignoreCase = true)
                                    } ?: activeTrain
                                    onSelectTrainAndOpen3D(target.trainNumber)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_view_train_primary"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Radar, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ট্রেন দেখুন (রাস্তা ও 3D লাইভ লোকেশন)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        SearchTabMode.ROUTE -> {
                            RouteStationSelectorSection(
                                originCode = uiState.originStationCode,
                                destinationCode = uiState.destinationStationCode,
                                onSetRoute = onSetRouteStations,
                                onSwap = onSwapRouteStations,
                                onSelectTrain = onSelectTrainAndOpen3D
                            )
                        }

                        SearchTabMode.VOICE -> {
                            VoiceSearchSection(
                                feedbackText = voiceFeedbackText,
                                onLaunchSystemSpeech = {
                                    try {
                                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                            )
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                                            putExtra(
                                                RecognizerIntent.EXTRA_PROMPT,
                                                "ট্রেনের নাম বা নম্বর বলুন (যেমন: সুবর্ণ এক্সপ্রেস ৭০১)"
                                            )
                                        }
                                        speechLauncher.launch(intent)
                                    } catch (_: Exception) {
                                        voiceFeedbackText = "ভয়েস ইনপুট প্রস্তুত — নিচের যেকোনো ভয়েস কমান্ড ট্যাপ করুন"
                                    }
                                },
                                onQuickVoiceCommand = { trainNum, spokenLabel ->
                                    voiceFeedbackText = "ভয়েস কমান্ড: \"$spokenLabel\""
                                    onSelectTrainAndOpen3D(trainNum)
                                }
                            )
                        }
                    }
                }
            }
        }

        // 3. Main 8 Feature Cards in an Original 2-Column Bento Command Deck
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "লাইভ ট্র্যাকিং ও যাত্রী কমান্ড হাব",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "৮টি প্রধান সেবা",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                for (rowIdx in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val leftItem = mainBentoFeatures[rowIdx * 2]
                        val rightItem = mainBentoFeatures[rowIdx * 2 + 1]
                        BentoServiceCard(
                            item = leftItem,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(leftItem.targetScreen) }
                        )
                        BentoServiceCard(
                            item = rightItem,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(rightItem.targetScreen) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        // 4. Rail Safety Interactive Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { onNavigate(AppScreen.RAIL_SAFETY) }
                    .testTag("card_rail_safety_banner"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0A4D36)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = SignalGold.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "রেল নিরাপত্তা",
                                tint = SignalGold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "রেল নিরাপত্তা গাইড ও জরুরি সতর্কতা",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "ট্রেন, প্ল্যাটফর্ম ও রেললাইনে নিরাপদ থাকুন • ছাদে ভ্রমণ নিষিদ্ধ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFB9F6CA)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = SignalGold
                    )
                }
            }
        }

        // 5. "আরও সেবা" (More Services)
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    text = "আরও সেবা",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MoreServicePillCard(
                        modifier = Modifier.weight(1f),
                        titleBn = "নির্দেশিকা",
                        icon = Icons.Default.MenuBook,
                        tint = TelemetryBlue,
                        onClick = { onNavigate(AppScreen.TRAVEL_GUIDE) },
                        tag = "service_guide"
                    )
                    MoreServicePillCard(
                        modifier = Modifier.weight(1f),
                        titleBn = "প্রশ্নোত্তর",
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        tint = TelemetryCyan,
                        onClick = { onNavigate(AppScreen.FAQ) },
                        tag = "service_faq"
                    )
                    MoreServicePillCard(
                        modifier = Modifier.weight(1f),
                        titleBn = "অভিযোগ",
                        icon = Icons.Default.Campaign,
                        tint = CrimsonExpress,
                        onClick = { onNavigate(AppScreen.COMPLAINT_SOS) },
                        tag = "service_complaint"
                    )
                    MoreServicePillCard(
                        modifier = Modifier.weight(1f),
                        titleBn = "পরবর্তী ট্রেন",
                        icon = Icons.Default.Schedule,
                        tint = EmeraldPrimary,
                        onClick = { onNavigate(AppScreen.NEXT_TRAIN) },
                        tag = "service_next_train"
                    )
                }
            }
        }
    }

    // Fleet Status Modal Sheet (কয়টা চলতেছে / কয়টা স্টেশনে / কয়টা আজ বন্ধ / সব ট্রেন)
    val modalFilter = activeFleetModalFilter
    if (modalFilter != null) {
        var modalSearchQuery by remember { mutableStateOf("") }
        val baseModalTrains = when (modalFilter) {
            FleetFilterMode.RUNNING_NOW -> allTelemetryList.filter { it.isRunningNow }
            FleetFilterMode.WAITING_OR_STATION -> allTelemetryList.filter { !it.isOffToday && !it.isRunningNow }
            FleetFilterMode.OFF_TODAY -> allTelemetryList.filter { it.isOffToday }
            FleetFilterMode.ALL -> allTelemetryList
        }
        val modalTrains = remember(baseModalTrains, modalSearchQuery) {
            val q = modalSearchQuery.trim()
            if (q.isEmpty()) {
                baseModalTrains
            } else {
                baseModalTrains.filter { tel ->
                    val tr = tel.train
                    tr.trainNumber.contains(q, ignoreCase = true) ||
                        tr.trainNumberBn.contains(q) ||
                        tr.nameBn.contains(q, ignoreCase = true) ||
                        tr.nameEn.contains(q, ignoreCase = true) ||
                        tr.corridorBn.contains(q, ignoreCase = true) ||
                        tr.stops.any { st ->
                            st.nameBn.contains(q, ignoreCase = true) ||
                                st.nameEn.contains(q, ignoreCase = true)
                        }
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { activeFleetModalFilter = null }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${modalFilter.labelBn} — মোট ${modalTrains.size}টি ট্রেন",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "যেকোনো ট্রেনে ট্যাপ করে তার বর্তমান অবস্থান ও 3D অ্যানিমেশন দেখুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = modalSearchQuery,
                    onValueChange = { modalSearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ট্রেনের নাম, নম্বর বা স্টেশন খুঁজুন (যেমন তিতাস, ৩৩, ৭০১)") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (modalSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { modalSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(modalTrains, key = { it.train.trainNumber }) { tel ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeFleetModalFilter = null
                                    onSelectTrainAndOpen3D(tel.train.trainNumber)
                                },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${tel.train.nameBn} (${tel.train.trainNumberBn})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = tel.statusBadgeBn,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            tel.isOffToday -> CrimsonExpress
                                            tel.isRunningNow -> EmeraldPrimary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    if (!tel.liveServerReportBn.isNullOrBlank()) {
                                        Text(
                                            text = "📡 ${tel.liveServerReportBn}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = EmeraldPrimary
                                        )
                                    }
                                }
                                Surface(
                                    color = when {
                                        tel.isOffToday -> CrimsonExpress
                                        tel.isRunningNow -> EmeraldPrimary
                                        else -> Color(0xFF455A64)
                                    },
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = when {
                                            tel.isOffToday -> "আজ বন্ধ"
                                            !tel.isRunningNow -> "স্টেশনে"
                                            tel.isAtStation -> "স্টেশনে হল্ট"
                                            else -> "${tel.currentSpeedKmh} km/h"
                                        },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Favorites Bottom Sheet ("প্রিয় ট্রেন")
    if (showFavoritesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFavoritesSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "★ আপনার প্রিয় ট্রেনসমূহ",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "যেকোনো ট্রেনে ট্যাপ করে সরাসরি লাইভ 3D লোকেশন দেখুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                val favTrains = BangladeshRailwayCatalog.allTrains.filter { tr ->
                    favorites.any { it.trainNumber == tr.trainNumber }
                }
                if (favTrains.isEmpty()) {
                    Text(
                        text = "এখনো কোনো প্রিয় ট্রেন যোগ করা হয়নি। যেকোনো ট্রেনের পাশে স্টার (★) চাপুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    favTrains.forEach { train ->
                        val tel = uiState.allTelemetries[train.trainNumber]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable {
                                    showFavoritesSheet = false
                                    onSelectTrainAndOpen3D(train.trainNumber)
                                },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
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
                                        text = "${train.corridorBn} • পরবর্তী: ${tel?.nextStation?.nameBn ?: ""}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { onToggleFavorite(train.trainNumber) }) {
                                    Icon(Icons.Default.Star, contentDescription = "রিমুভ", tint = SignalGold)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun FleetCounterPill(
    modifier: Modifier = Modifier,
    count: Int,
    labelBn: String,
    dotColor: Color,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag(tag),
        color = Color(0xFF073123),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "$count",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            Text(
                text = labelBn,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = Color(0xFFD1FAE5),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SearchModeTabButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: ImageVector,
    labelBn: String,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(tag),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = labelBn,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = labelBn,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RouteStationSelectorSection(
    originCode: String,
    destinationCode: String,
    onSetRoute: (String, String) -> Unit,
    onSwap: () -> Unit,
    onSelectTrain: (String) -> Unit
) {
    var showOriginMenu by remember { mutableStateOf(false) }
    var showDestMenu by remember { mutableStateOf(false) }

    val originStation = BangladeshRailwayCatalog.allStations.find { it.code == originCode }
        ?: BangladeshRailwayCatalog.allStations.first()
    val destStation = BangladeshRailwayCatalog.allStations.find { it.code == destinationCode }
        ?: BangladeshRailwayCatalog.allStations[13]

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showOriginMenu = true }
                        .testTag("select_origin_station")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("কোথা থেকে", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = originStation.nameBn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                DropdownMenu(expanded = showOriginMenu, onDismissRequest = { showOriginMenu = false }) {
                    BangladeshRailwayCatalog.allStations.forEach { st ->
                        DropdownMenuItem(
                            text = { Text(st.nameBn) },
                            onClick = {
                                showOriginMenu = false
                                onSetRoute(st.code, destinationCode)
                            }
                        )
                    }
                }
            }

            IconButton(
                onClick = onSwap,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .testTag("btn_swap_route_stations")
            ) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "স্টেশন অদলবদল",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDestMenu = true }
                        .testTag("select_dest_station")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("কোথায় যাবেন", style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = destStation.nameBn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                DropdownMenu(expanded = showDestMenu, onDismissRequest = { showDestMenu = false }) {
                    BangladeshRailwayCatalog.allStations.forEach { st ->
                        DropdownMenuItem(
                            text = { Text(st.nameBn) },
                            onClick = {
                                showDestMenu = false
                                onSetRoute(originCode, st.code)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val routeTrains = BangladeshRailwayCatalog.allTrains.filter { tr ->
            val origIdx = tr.stops.indexOfFirst { it.stationCode == originCode }
            val destIdx = tr.stops.indexOfFirst { it.stationCode == destinationCode }
            origIdx != -1 && destIdx != -1 && origIdx < destIdx
        }

        if (routeTrains.isEmpty()) {
            Text(
                text = "এই দুটি স্টেশনের মধ্যে সরাসরি ট্রেন পাওয়া যায়নি। অন্য স্টেশন নির্বাচন করুন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text(
                text = "${originStation.nameBn} ➔ ${destStation.nameBn} রুটে ${routeTrains.size}টি ট্রেন পাওয়া গেছে:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            routeTrains.take(8).forEach { tr ->
                val origStop = tr.stops.first { it.stationCode == originCode }
                val destStop = tr.stops.first { it.stationCode == destinationCode }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectTrain(tr.trainNumber) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${tr.nameBn} (${tr.trainNumberBn})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ছাড়বে: ${origStop.departureTime} • পৌঁছাবে: ${destStop.arrivalTime} • বন্ধ: ${tr.offDayBn}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            text = "3D ভিউ ➔",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceSearchSection(
    feedbackText: String?,
    onLaunchSystemSpeech: () -> Unit,
    onQuickVoiceCommand: (String, String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = EmeraldPrimary.copy(alpha = 0.14f),
            shape = CircleShape,
            modifier = Modifier
                .size(72.dp)
                .border(2.dp, EmeraldPrimary, CircleShape)
                .clickable { onLaunchSystemSpeech() }
                .testTag("btn_voice_mic_trigger")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "কথা বলুন",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = feedbackText ?: "মাইক্রোফোনে ট্যাপ করে ট্রেনের নাম বা নম্বর বলুন",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sampleCommands = listOf(
                "701" to "🎙️ \"৭০১ সুবর্ণ এক্সপ্রেস কোথায়?\"",
                "813" to "🎙️ \"কক্সবাজার এক্সপ্রেস কয়টায় আসবে?\"",
                "787" to "🎙️ \"সোনার বাংলা এক্সপ্রেস স্পিড কত?\"",
                "709" to "🎙️ \"পারাবত এক্সপ্রেস কোন স্টেশনে?\""
            )
            sampleCommands.forEach { (num, label) ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.clickable { onQuickVoiceCommand(num, label) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoServiceCard(
    item: BentoFeatureItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag(item.testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = item.containerColor,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.titleBn,
                            tint = item.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Surface(
                    color = item.accentColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = item.badgeBn,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = item.accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.titleBn,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.subtitleBn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun MoreServicePillCard(
    modifier: Modifier = Modifier,
    titleBn: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = tint.copy(alpha = 0.14f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = titleBn,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = titleBn,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
