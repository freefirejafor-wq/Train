package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRailway
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.AppScreen
import com.example.data.BangladeshRailwayCatalog
import com.example.data.Camera3DMode
import com.example.data.Environment3DTheme
import com.example.data.RailPulseRepository
import com.example.ui.RailPulseViewModel
import com.example.ui.screens.CoachPositionScreen
import com.example.ui.screens.ComplaintSosScreen
import com.example.ui.screens.FaqScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveChatScreen
import com.example.ui.screens.LiveTrain3DScreen
import com.example.ui.screens.LostFoundScreen
import com.example.ui.screens.NextTrainScreen
import com.example.ui.screens.RailSafetyScreen
import com.example.ui.screens.RouteAnalysisScreen
import com.example.ui.screens.StationBoardScreen
import com.example.ui.screens.StationStatusScreen
import com.example.ui.screens.TrainLocatorScreen
import com.example.ui.screens.TravelGuideScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SignalGold

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repository = remember {
                val db = AppDatabase.getDatabase(context)
                RailPulseRepository(db.railPulseDao())
            }
            val viewModel: RailPulseViewModel = viewModel(
                factory = RailPulseViewModel.provideFactory(repository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
                RailPulseAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun RailPulseAppRoot(viewModel: RailPulseViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteTrains.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val lostFoundPosts by viewModel.lostFoundPosts.collectAsStateWithLifecycle()
    val stationAlarms by viewModel.stationAlarms.collectAsStateWithLifecycle()
    val complaints by viewModel.complaints.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBannerMessage()
        }
    }

    val cycleCamera: () -> Unit = {
        val next = when (uiState.camera3DMode) {
            Camera3DMode.COCKPIT_3D -> Camera3DMode.ISOMETRIC_DRONE
            Camera3DMode.ISOMETRIC_DRONE -> Camera3DMode.TOP_RADAR
            Camera3DMode.TOP_RADAR -> Camera3DMode.COCKPIT_3D
        }
        viewModel.setCamera3DMode(next)
    }

    val cycleEnvironment: () -> Unit = {
        val next = when (uiState.environment3DTheme) {
            Environment3DTheme.AUTO -> Environment3DTheme.DAYLIGHT
            Environment3DTheme.DAYLIGHT -> Environment3DTheme.NIGHT_VISION
            Environment3DTheme.NIGHT_VISION -> Environment3DTheme.MONSOON_RAIN
            Environment3DTheme.MONSOON_RAIN -> Environment3DTheme.AUTO
        }
        viewModel.setEnvironment3DTheme(next)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation_bar")
            ) {
                val navItems = listOf(
                    Triple(AppScreen.HOME, "হোম হাব", Icons.Default.Home),
                    Triple(AppScreen.LIVE_3D_STATUS, "3D লাইভ", Icons.Default.DirectionsRailway),
                    Triple(AppScreen.TRAIN_LOCATOR, "লোকেটর", Icons.Default.LocationOn),
                    Triple(AppScreen.STATION_BOARD, "স্টেশন বোর্ড", Icons.Default.Tv),
                    Triple(AppScreen.COACH_POSITION, "বগি ও ভাড়া", Icons.Default.ViewCarousel)
                )
                navItems.forEach { (screen, label, icon) ->
                    val selected = uiState.currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { viewModel.navigateTo(screen) },
                        icon = { Icon(icon, contentDescription = label) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = SignalGold.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.currentScreen) {
                AppScreen.HOME -> HomeScreen(
                    uiState = uiState,
                    favorites = favorites,
                    onNavigate = { viewModel.navigateTo(it) },
                    onSelectSearchTab = { viewModel.setSearchTabMode(it) },
                    onUpdateQuery = { viewModel.updateTrainSearchQuery(it) },
                    onSelectTrainAndOpen3D = { viewModel.selectTrainAndOpen3D(it) },
                    onSelectTrainPreview = { viewModel.selectTrainOnly(it) },
                    onSetRouteStations = { orig, dest -> viewModel.setRouteStations(orig, dest) },
                    onSwapRouteStations = { viewModel.swapRouteStations() },
                    onToggleDarkTheme = { viewModel.toggleDarkTheme() },
                    onCycleCamera3D = cycleCamera,
                    onCycleEnvironment3D = cycleEnvironment,
                    onToggleOppositeTrain3D = { viewModel.toggleOppositeTrain3D() },
                    onToggleFavorite = { viewModel.toggleFavoriteTrain(it) }
                )

                AppScreen.LIVE_3D_STATUS -> LiveTrain3DScreen(
                    uiState = uiState,
                    favorites = favorites,
                    alarms = stationAlarms,
                    onBack = { viewModel.navigateBack() },
                    onSelectTrain = { viewModel.selectTrainOnly(it) },
                    onCycleCamera = cycleCamera,
                    onCycleEnvironment = cycleEnvironment,
                    onToggleOppositeTrain = { viewModel.toggleOppositeTrain3D() },
                    onAdjustTimeShift = { viewModel.adjustSimulationTimeShift(it) },
                    onSetOnBoardGps = { active, spd -> viewModel.setOnBoardGpsSpeed(active, spd) },
                    onReportDelay = { trNum, delayMin, stTag -> viewModel.reportTrainDelay(trNum, delayMin, stTag) },
                    onToggleFavorite = { viewModel.toggleFavoriteTrain(it) },
                    onAddStationAlarm = { st, km ->
                        val activeTrain = BangladeshRailwayCatalog.allTrains.find { it.trainNumber == uiState.selectedTrainNumber }
                            ?: BangladeshRailwayCatalog.allTrains.first()
                        viewModel.addStationWakeupAlarm(activeTrain, st, km)
                    },
                    onOpenCoachPosition = { viewModel.navigateTo(AppScreen.COACH_POSITION) }
                )

                AppScreen.TRAIN_LOCATOR -> TrainLocatorScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateBack() },
                    onSelectTrainFor3D = { viewModel.selectTrainAndOpen3D(it) }
                )

                AppScreen.LIVE_CHAT -> LiveChatScreen(
                    uiState = uiState,
                    messages = chatMessages,
                    onBack = { viewModel.navigateBack() },
                    onSendMessage = { trNum, sender, msg, stTag, delayMin ->
                        viewModel.sendChatMessage(trNum, sender, msg, stTag, delayMin)
                    }
                )

                AppScreen.STATION_BOARD -> StationBoardScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateBack() },
                    onSelectStation = { viewModel.selectStation(it) },
                    onOpenTrain3D = { viewModel.selectTrainAndOpen3D(it) }
                )

                AppScreen.STATION_STATUS -> StationStatusScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateBack() },
                    onSelectStation = { viewModel.selectStation(it) },
                    onReportCrowd = { code, label, pct -> viewModel.reportStationCrowd(code, label, pct) }
                )

                AppScreen.LOST_AND_FOUND -> LostFoundScreen(
                    posts = lostFoundPosts,
                    onBack = { viewModel.navigateBack() },
                    onAddPost = { type, title, train, st, coach, phone, details ->
                        viewModel.addLostFoundPost(type, title, train, st, coach, phone, details)
                    },
                    onDeletePost = { viewModel.deleteLostFoundPost(it) }
                )

                AppScreen.COACH_POSITION -> CoachPositionScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateBack() },
                    onSelectTrain = { viewModel.selectTrainOnly(it) }
                )

                AppScreen.ROUTE_ANALYSIS -> RouteAnalysisScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateBack() },
                    onOpenTrain3D = { viewModel.selectTrainAndOpen3D(it) }
                )

                AppScreen.RAIL_SAFETY -> RailSafetyScreen(
                    onBack = { viewModel.navigateBack() }
                )

                AppScreen.TRAVEL_GUIDE -> TravelGuideScreen(
                    onBack = { viewModel.navigateBack() }
                )

                AppScreen.FAQ -> FaqScreen(
                    onBack = { viewModel.navigateBack() }
                )

                AppScreen.COMPLAINT_SOS -> ComplaintSosScreen(
                    complaints = complaints,
                    onBack = { viewModel.navigateBack() },
                    onSubmitComplaint = { cat, trOrSt, coach, desc ->
                        viewModel.submitPassengerComplaint(cat, trOrSt, coach, desc)
                    }
                )

                AppScreen.NEXT_TRAIN -> NextTrainScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateBack() },
                    onSelectStation = { viewModel.selectStation(it) },
                    onOpenTrain3D = { viewModel.selectTrainAndOpen3D(it) }
                )
            }
        }
    }
}
