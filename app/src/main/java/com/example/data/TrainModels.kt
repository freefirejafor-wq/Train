package com.example.data

enum class AppScreen {
    HOME,
    LIVE_3D_STATUS,
    TRAIN_LOCATOR,
    LIVE_CHAT,
    STATION_BOARD,
    STATION_STATUS,
    LOST_AND_FOUND,
    COACH_POSITION,
    ROUTE_ANALYSIS,
    RAIL_SAFETY,
    TRAVEL_GUIDE,
    FAQ,
    COMPLAINT_SOS,
    NEXT_TRAIN
}

enum class SearchTabMode {
    TRAIN,
    ROUTE,
    VOICE
}

enum class FleetFilterMode(val labelBn: String) {
    ALL("সব ট্রেন"),
    RUNNING_NOW("চলমান"),
    WAITING_OR_STATION("স্টেশনে/অপেক্ষমাণ"),
    OFF_TODAY("আজ বন্ধ")
}

enum class Camera3DMode(val labelBn: String) {
    COCKPIT_3D("সামনের 3D ভিউ"),
    ISOMETRIC_DRONE("পাশের রাস্তা ও 3D ট্রেন"),
    TOP_RADAR("ডাবল-লাইন রাডার")
}

enum class Environment3DTheme(val labelBn: String) {
    AUTO("অটো (দিন/রাত)"),
    DAYLIGHT("দিনের রোদ"),
    NIGHT_VISION("রাতের হেডলাইট"),
    MONSOON_RAIN("বর্ষা ও মেঘ")
}

data class StationStop(
    val stationCode: String,
    val nameBn: String,
    val nameEn: String,
    val arrivalTime: String, // "HH:mm" in 24h BST
    val departureTime: String, // "HH:mm" in 24h BST
    val haltMinutes: Int,
    val distanceKm: Int,
    val platformNumber: Int,
    val lat: Float,
    val lng: Float
)

data class CoachInfo(
    val serialBn: String,
    val serialEn: String,
    val classTypeBn: String,
    val seatCount: Int,
    val baseFareBdt: Int,
    val isAc: Boolean
)

data class TrainSchedule(
    val trainNumber: String,
    val trainNumberBn: String,
    val nameBn: String,
    val nameEn: String,
    val corridorBn: String,
    val offDayBn: String,
    val maxSpeedKmh: Int,
    val averageDelayMinutes: Int,
    val punctualityPercent: Int,
    val liveryType: String,
    val oppositeTrainNumber: String,
    val oppositeTrainNameBn: String,
    val stops: List<StationStop>,
    val coaches: List<CoachInfo>
)

data class LiveTrainTelemetry(
    val train: TrainSchedule,
    val isOffToday: Boolean,
    val todayNameBn: String,
    val isRunningNow: Boolean,
    val isAtStation: Boolean,
    val statusBadgeBn: String,
    val currentSpeedKmh: Int,
    val lastStation: StationStop,
    val nextStation: StationStop,
    val currentStationIndex: Int,
    val segmentProgress: Float,
    val overallProgress: Float,
    val distanceCoveredKm: Float,
    val distanceRemainingToNextKm: Float,
    val totalDistanceKm: Int,
    val etaNextStationMinutes: Int,
    val expectedNextArrivalFormatted: String,
    val expectedNextDepartureFormatted: String,
    val expectedFinalArrivalFormatted: String,
    val delayMinutes: Int,
    val currentLat: Float,
    val currentLng: Float,
    val oppositeTrainDistanceKm: Float,
    val isNearBridge: Boolean,
    val bridgeNameBn: String,
    val isGpsOverrideActive: Boolean,
    val isLiveServerSynced: Boolean = false,
    val liveServerReportBn: String? = null,
    val liveServerUpdatedAgoBn: String? = null,
    val scheduledDepartureFormatted: String = "",
    val scheduledArrivalFormatted: String = ""
)

data class StationDirectoryInfo(
    val code: String,
    val nameBn: String,
    val nameEn: String,
    val divisionBn: String,
    val totalPlatforms: Int,
    val defaultCrowdLevelBn: String,
    val crowdPercentage: Int,
    val hasWifi: Boolean,
    val hasAcWaitingRoom: Boolean,
    val hasFoodCourt: Boolean,
    val hasParking: Boolean,
    val grpPhone: String,
    val stationMasterPhone: String,
    val lat: Float,
    val lng: Float
)

data class FaqItem(
    val categoryBn: String,
    val questionBn: String,
    val answerBn: String
)

data class SafetyRuleItem(
    val titleBn: String,
    val subtitleBn: String,
    val emergencyTipBn: String,
    val isCritical: Boolean
)
