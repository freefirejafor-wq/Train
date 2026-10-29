package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppScreen
import com.example.data.BangladeshRailwayCatalog
import com.example.data.Camera3DMode
import com.example.data.ChatMessageEntity
import com.example.data.ComplaintEntity
import com.example.data.Environment3DTheme
import com.example.data.FavoriteTrainEntity
import com.example.data.LiveTrainTelemetry
import com.example.data.LostFoundEntity
import com.example.data.RailPulseRepository
import com.example.data.SearchTabMode
import com.example.data.StationAlarmEntity
import com.example.data.StationDirectoryInfo
import com.example.data.TrainSchedule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.LiveRailNetworkSync
import com.example.data.LiveServerTrainReport
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RailPulseUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val screenHistory: List<AppScreen> = emptyList(),
    val isDarkTheme: Boolean = false,
    val searchTabMode: SearchTabMode = SearchTabMode.TRAIN,
    val trainSearchQuery: String = "",
    val originStationCode: String = "DA",
    val destinationStationCode: String = "CTG",
    val selectedTrainNumber: String = "701",
    val selectedStationCode: String = "DA",
    val camera3DMode: Camera3DMode = Camera3DMode.COCKPIT_3D,
    val environment3DTheme: Environment3DTheme = Environment3DTheme.AUTO,
    val showOppositeTrainIn3D: Boolean = true,
    val simulationShiftMinutes: Int = 0,
    val isOnBoardGpsActive: Boolean = false,
    val gpsSpeedKmh: Int? = null,
    val currentBstClock: String = BangladeshRailwayCatalog.currentBstTimeFormatted(),
    val customTrainDelays: Map<String, Int> = emptyMap(),
    val liveServerReports: Map<String, LiveServerTrainReport> = emptyMap(),
    val customStationCrowds: Map<String, Pair<String, Int>> = emptyMap(),
    val allTelemetries: Map<String, LiveTrainTelemetry> = emptyMap(),
    val statusBannerMessage: String? = null
)

class RailPulseViewModel(private val repository: RailPulseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RailPulseUiState())
    val uiState: StateFlow<RailPulseUiState> = _uiState.asStateFlow()

    val favoriteTrains: StateFlow<List<FavoriteTrainEntity>> = repository.favoriteTrains
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lostFoundPosts: StateFlow<List<LostFoundEntity>> = repository.lostFoundPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stationAlarms: StateFlow<List<StationAlarmEntity>> = repository.stationAlarms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val complaints: StateFlow<List<ComplaintEntity>> = repository.complaints
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialCommunityDataIfEmpty()
        }
        refreshAllTelemetries(autoSelectRunningTrainIfInitial = true)
        startLiveTelemetryTicker()
        startLiveNetworkSyncTicker()
    }

    private fun startLiveTelemetryTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                refreshAllTelemetries()
            }
        }
    }

    private fun startLiveNetworkSyncTicker() {
        viewModelScope.launch {
            while (isActive) {
                syncLiveServerNow()
                delay(30_000L)
            }
        }
    }

    fun syncLiveServerNow() {
        viewModelScope.launch {
            val selectedNum = _uiState.value.selectedTrainNumber
            val fetched = LiveRailNetworkSync.fetchLiveTrainReports(selectedNum)
            if (fetched.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(liveServerReports = fetched)
                refreshAllTelemetries()
            }
        }
    }

    private fun refreshAllTelemetries(autoSelectRunningTrainIfInitial: Boolean = false) {
        val state = _uiState.value
        val updatedMap = BangladeshRailwayCatalog.allTrains.associate { train ->
            val customDelay = state.customTrainDelays[train.trainNumber]
            val liveReport = state.liveServerReports[train.trainNumber]
            val gpsOverride = if (state.isOnBoardGpsActive && state.selectedTrainNumber == train.trainNumber) {
                state.gpsSpeedKmh
            } else null
            train.trainNumber to BangladeshRailwayCatalog.calculateLiveTelemetry(
                train = train,
                customDelayMinutes = customDelay,
                gpsSpeedOverrideKmh = gpsOverride,
                simulationTimeShiftMinutes = state.simulationShiftMinutes,
                liveServerReport = liveReport
            )
        }

        var nextSelected = state.selectedTrainNumber
        if (autoSelectRunningTrainIfInitial) {
            val currentTel = updatedMap[nextSelected]
            if (currentTel == null || !currentTel.isRunningNow) {
                val firstRunning = updatedMap.values.firstOrNull { it.isRunningNow && !it.isAtStation }
                    ?: updatedMap.values.firstOrNull { it.isRunningNow }
                if (firstRunning != null) {
                    nextSelected = firstRunning.train.trainNumber
                }
            }
        }

        _uiState.value = state.copy(
            selectedTrainNumber = nextSelected,
            currentBstClock = BangladeshRailwayCatalog.currentBstTimeFormatted(),
            allTelemetries = updatedMap
        )
    }

    fun navigateTo(screen: AppScreen) {
        val curr = _uiState.value.currentScreen
        if (curr == screen) return
        _uiState.value = _uiState.value.copy(
            currentScreen = screen,
            screenHistory = _uiState.value.screenHistory + curr
        )
    }

    fun navigateBack(): Boolean {
        val history = _uiState.value.screenHistory
        return if (history.isNotEmpty()) {
            val prev = history.last()
            _uiState.value = _uiState.value.copy(
                currentScreen = prev,
                screenHistory = history.dropLast(1)
            )
            true
        } else if (_uiState.value.currentScreen != AppScreen.HOME) {
            _uiState.value = _uiState.value.copy(currentScreen = AppScreen.HOME)
            true
        } else {
            false
        }
    }

    fun toggleDarkTheme() {
        _uiState.value = _uiState.value.copy(isDarkTheme = !_uiState.value.isDarkTheme)
    }

    fun setSearchTabMode(mode: SearchTabMode) {
        _uiState.value = _uiState.value.copy(searchTabMode = mode)
    }

    fun updateTrainSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(trainSearchQuery = query)
    }

    fun selectTrainAndOpen3D(trainNumber: String) {
        _uiState.value = _uiState.value.copy(selectedTrainNumber = trainNumber)
        refreshAllTelemetries()
        syncLiveServerNow()
        navigateTo(AppScreen.LIVE_3D_STATUS)
    }

    fun selectTrainOnly(trainNumber: String) {
        _uiState.value = _uiState.value.copy(selectedTrainNumber = trainNumber)
        refreshAllTelemetries()
        syncLiveServerNow()
    }

    fun setRouteStations(originCode: String, destinationCode: String) {
        _uiState.value = _uiState.value.copy(
            originStationCode = originCode,
            destinationStationCode = destinationCode
        )
    }

    fun swapRouteStations() {
        val current = _uiState.value
        _uiState.value = current.copy(
            originStationCode = current.destinationStationCode,
            destinationStationCode = current.originStationCode
        )
    }

    fun selectStation(stationCode: String) {
        _uiState.value = _uiState.value.copy(selectedStationCode = stationCode)
    }

    fun setCamera3DMode(mode: Camera3DMode) {
        _uiState.value = _uiState.value.copy(camera3DMode = mode)
    }

    fun setEnvironment3DTheme(theme: Environment3DTheme) {
        _uiState.value = _uiState.value.copy(environment3DTheme = theme)
    }

    fun toggleOppositeTrain3D() {
        _uiState.value = _uiState.value.copy(
            showOppositeTrainIn3D = !_uiState.value.showOppositeTrainIn3D
        )
    }

    fun adjustSimulationTimeShift(deltaMinutes: Int) {
        val nextShift = if (deltaMinutes == 0) 0 else (_uiState.value.simulationShiftMinutes + deltaMinutes)
        _uiState.value = _uiState.value.copy(simulationShiftMinutes = nextShift)
        refreshAllTelemetries()
    }

    fun setOnBoardGpsSpeed(active: Boolean, speedKmh: Int?) {
        _uiState.value = _uiState.value.copy(
            isOnBoardGpsActive = active,
            gpsSpeedKmh = speedKmh,
            statusBannerMessage = if (active) {
                "অন-বোর্ড লাইভ GPS স্পিড মিটার চালু হয়েছে (${speedKmh ?: 0} km/h)"
            } else null
        )
        refreshAllTelemetries()
    }

    fun reportTrainDelay(trainNumber: String, delayMinutes: Int, stationTag: String) {
        val updatedDelays = _uiState.value.customTrainDelays.toMutableMap()
        updatedDelays[trainNumber] = delayMinutes
        _uiState.value = _uiState.value.copy(
            customTrainDelays = updatedDelays,
            statusBannerMessage = "ট্রেন #$trainNumber-এর লাইভ ডিলে ($delayMinutes মিনিট) আপডেট হয়েছে!"
        )
        refreshAllTelemetries()

        val train = BangladeshRailwayCatalog.allTrains.find { it.trainNumber == trainNumber }
        if (train != null) {
            viewModelScope.launch {
                repository.postChatMessage(
                    ChatMessageEntity(
                        trainNumber = trainNumber,
                        trainNameBn = "${train.nameBn} (${train.trainNumberBn})",
                        senderName = "লাইভ যাত্রী আপডেট",
                        messageBn = "যাত্রী রিপোর্ট: ট্রেনটি বর্তমানে $stationTag এলাকায় আছে এবং আনুমানিক $delayMinutes মিনিট বিলম্বে চলছে।",
                        currentStationTag = stationTag,
                        reportedDelayMin = delayMinutes
                    )
                )
            }
        }
    }

    fun reportStationCrowd(stationCode: String, crowdLabelBn: String, crowdPercent: Int) {
        val updated = _uiState.value.customStationCrowds.toMutableMap()
        updated[stationCode] = Pair(crowdLabelBn, crowdPercent)
        _uiState.value = _uiState.value.copy(
            customStationCrowds = updated,
            statusBannerMessage = "স্টেশনের বর্তমান ভিড়ের অবস্থা ($crowdLabelBn) আপডেট করা হয়েছে!"
        )
    }

    fun clearBannerMessage() {
        _uiState.value = _uiState.value.copy(statusBannerMessage = null)
    }

    fun toggleFavoriteTrain(trainNumber: String) {
        val isFav = favoriteTrains.value.any { it.trainNumber == trainNumber }
        viewModelScope.launch {
            repository.toggleFavorite(trainNumber, isFav)
        }
    }

    fun sendChatMessage(trainNumber: String, senderName: String, messageBn: String, stationTag: String, delayMin: Int) {
        val train = BangladeshRailwayCatalog.allTrains.find { it.trainNumber == trainNumber }
            ?: BangladeshRailwayCatalog.allTrains.first()
        viewModelScope.launch {
            repository.postChatMessage(
                ChatMessageEntity(
                    trainNumber = train.trainNumber,
                    trainNameBn = "${train.nameBn} (${train.trainNumberBn})",
                    senderName = senderName.ifBlank { "যাত্রী #BD-${train.trainNumber}" },
                    messageBn = messageBn,
                    currentStationTag = stationTag,
                    reportedDelayMin = delayMin
                )
            )
        }
    }

    fun addLostFoundPost(
        postType: String,
        itemTitleBn: String,
        trainNameBn: String,
        stationNameBn: String,
        coachSerialBn: String,
        contactPhone: String,
        detailsBn: String
    ) {
        viewModelScope.launch {
            repository.postLostFound(
                LostFoundEntity(
                    postType = postType,
                    itemTitleBn = itemTitleBn,
                    trainNameBn = trainNameBn,
                    stationNameBn = stationNameBn,
                    coachSerialBn = coachSerialBn,
                    contactPhone = contactPhone,
                    detailsBn = detailsBn
                )
            )
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = "হারানো-পাওয়া বোর্ডে আপনার পোস্ট যুক্ত হয়েছে।"
            )
        }
    }

    fun deleteLostFoundPost(id: Int) {
        viewModelScope.launch {
            repository.deleteLostFound(id)
        }
    }

    fun addStationWakeupAlarm(train: TrainSchedule, station: StationDirectoryInfo, alertBeforeKm: Int) {
        viewModelScope.launch {
            repository.addStationAlarm(
                StationAlarmEntity(
                    trainNumber = train.trainNumber,
                    trainNameBn = "${train.nameBn} (${train.trainNumberBn})",
                    stationCode = station.code,
                    stationNameBn = station.nameBn,
                    alertBeforeKm = alertBeforeKm
                )
            )
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = "${station.nameBn} স্টেশনের $alertBeforeKm কিমি আগে ওয়েক-আপ অ্যালার্ম সেট করা হয়েছে!"
            )
        }
    }

    fun removeStationAlarm(id: Int) {
        viewModelScope.launch {
            repository.removeStationAlarm(id)
        }
    }

    fun submitPassengerComplaint(
        categoryBn: String,
        trainOrStationBn: String,
        coachNumberBn: String,
        descriptionBn: String
    ) {
        viewModelScope.launch {
            repository.submitComplaint(
                ComplaintEntity(
                    categoryBn = categoryBn,
                    trainOrStationBn = trainOrStationBn,
                    coachNumberBn = coachNumberBn,
                    descriptionBn = descriptionBn
                )
            )
            _uiState.value = _uiState.value.copy(
                statusBannerMessage = "আপনার অভিযোগটি সফলভাবে নথিভুক্ত হয়েছে।"
            )
        }
    }

    companion object {
        fun provideFactory(repository: RailPulseRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RailPulseViewModel(repository) as T
                }
            }
    }
}
