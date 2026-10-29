package com.example.data

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

data class LiveServerTrainReport(
    val trainNumber: String,
    val slug: String,
    val delayMinutes: Int,
    val delayLabelBn: String,
    val updatedAgoBn: String,
    val isRecentlyUpdated: Boolean,
    val liveStationBulletinBn: String? = null,
    val reportedCurrentStationBn: String? = null,
    val reportedNextStationBn: String? = null,
    val reportedEtaBn: String? = null
)

object LiveRailNetworkSync {

    private const val ENDPOINT_RUNNING_TRAINS = "https://www.trainlivelocation.com/running-trains-api"
    private const val ENDPOINT_HOMEPAGE = "https://www.trainlivelocation.com/"
    private const val ENDPOINT_TRAIN_DETAIL_PREFIX = "https://www.trainlivelocation.com/train-live-location/"

    private val slugCacheByTrainNumber = ConcurrentHashMap<String, String>()
    private val detailBulletinCache = ConcurrentHashMap<String, Pair<Long, Triple<String?, String?, String?>>>()

    private fun banglaToAsciiDigits(input: String): String {
        val map = mapOf(
            '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
            '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
        )
        return input.map { map[it] ?: it }.joinToString("")
    }

    private fun asciiToBanglaDigits(input: String): String {
        val map = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )
        return input.map { map[it] ?: it }.joinToString("")
    }

    fun parseDelayMinutes(lateTimeStr: String?): Int {
        if (lateTimeStr.isNullOrBlank() || lateTimeStr == "null" || lateTimeStr == "None") return 0
        val normalized = banglaToAsciiDigits(lateTimeStr)
        var hours = 0
        var minutes = 0

        val hourMatch = Regex("""(\d+)\s*(?:ঘণ্টা|ঘন্টা|hour|hr)""", RegexOption.IGNORE_CASE).find(normalized)
        if (hourMatch != null) {
            hours = hourMatch.groupValues[1].toIntOrNull() ?: 0
        }

        val minMatch = Regex("""(\d+)\s*(?:মিনিট|min)""", RegexOption.IGNORE_CASE).find(normalized)
        if (minMatch != null) {
            minutes = minMatch.groupValues[1].toIntOrNull() ?: 0
        } else if (hourMatch == null) {
            val anyNum = Regex("""(\d+)""").find(normalized)
            minutes = anyNum?.groupValues?.get(1)?.toIntOrNull() ?: 0
        }

        return (hours * 60 + minutes).coerceIn(0, 720)
    }

    fun translateUpdateDiffToBn(diffRaw: String?): Pair<String, Boolean> {
        if (diffRaw.isNullOrBlank() || diffRaw == "null") return Pair("এইমাত্র আপডেট", true)
        val lower = diffRaw.lowercase().trim()
        val numMatch = Regex("""(\d+)""").find(lower)
        val num = numMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val numBn = asciiToBanglaDigits(num.toString())

        return when {
            lower.contains("second") || lower.contains("just") ->
                Pair("এইমাত্র আপডেট", true)
            lower.contains("minute") || lower.contains("min") ->
                Pair("$numBn মিনিট আগে আপডেট", num <= 180)
            lower.contains("hour") ->
                Pair("$numBn ঘণ্টা আগে আপডেট", num <= 4)
            lower.contains("day") ->
                Pair("$numBn দিন আগে", false)
            lower.contains("week") ->
                Pair("$numBn সপ্তাহ আগে", false)
            else ->
                Pair("লাইভ আপডেট", true)
        }
    }

    private fun fetchUrlText(urlStr: String, timeoutMs: Int = 8000): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                setRequestProperty("Accept", "application/json, text/html, */*")
            }
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun extractMatchedTrainNumbers(nameRaw: String, slug: String): List<String> {
        val asciiName = banglaToAsciiDigits(nameRaw)
        val explicitNumMatch = Regex("""\((\d{1,4})\)""").find(asciiName)
        if (explicitNumMatch != null) {
            val num = explicitNumMatch.groupValues[1]
            if (BangladeshRailwayCatalog.allTrains.any { it.trainNumber == num }) {
                return listOf(num)
            }
        }

        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+6"))
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        // Special multi-trip handling for Titas Commuter (33, 34, 35, 36)
        if (nameRaw.contains("তিতাস") || slug.contains("titas")) {
            val activeTitas = when {
                nowMin in 270..565 -> "33"   // 05:00 AM - 09:25 AM (Akhaura -> Dhaka)
                nowMin in 566..770 -> "34"   // 09:45 AM - 12:50 PM (Dhaka -> Brahmanbaria)
                nowMin in 771..1020 -> "35"  // 01:10 PM - 05:00 PM (Brahmanbaria -> Dhaka)
                else -> "36"                 // 05:45 PM - Night (Dhaka -> Akhaura)
            }
            return listOf(activeTitas, "33", "34", "35", "36").distinct()
        }

        val keywordPairs: List<Pair<List<String>, List<String>>> = listOf(
            listOf("তিস্তা", "tista") to listOf("707", "708"),
            listOf("ব্রহ্মপুত্র", "brahmaputra") to listOf("743", "744"),
            listOf("এগারোসিন্ধুর প্রভাতী", "এগারোসিন্দুর প্রভাতী", "egarosindhur-provati") to listOf("737", "738"),
            listOf("এগারোসিন্ধুর গোধূলি", "এগারোসিন্দুর গোধূলী", "egarosindhur-godhuli") to listOf("749", "750"),
            listOf("হাওর", "haor") to listOf("777", "778"),
            listOf("মোহনগঞ্জ", "mohanganj") to listOf("789", "790"),
            listOf("রংপুর এক্সপ্রেস", "rangpur-express") to listOf("771", "772"),
            listOf("লালমনি", "লালমণি", "lalmoni") to listOf("751", "752"),
            listOf("কুড়িগ্রাম", "কুড়িগ্রাম", "kurigram") to listOf("797", "798"),
            listOf("একতা", "ekota") to listOf("705", "706"),
            listOf("দ্রুতযান", "drutojan") to listOf("757", "758"),
            listOf("পঞ্চগড়", "পঞ্চগড়", "panchagarh") to listOf("793", "794"),
            listOf("দেওয়ানগঞ্জ কমিউটার", "দেওয়ানগঞ্জ কমিউটার", "dewgonj") to listOf("47", "48"),
            listOf("বিজয়", "বিজয়", "bijoy") to listOf("785", "786"),
            listOf("কিশোরগঞ্জ", "kishoreganj") to listOf("781", "782"),
            listOf("বুড়িমারী", "বুড়িমারী", "burimari") to listOf("809", "810"),
            listOf("নীলসাগর", "nilsagar") to listOf("765", "766"),
            listOf("চিলাহাটি", "chilahati") to listOf("805", "806"),
            listOf("করতোয়া", "করতোয়া", "korotoa") to listOf("713", "714"),
            listOf("রুপসা", "রূপসা", "rupsa") to listOf("727", "728"),
            listOf("সীমান্ত", "simanta") to listOf("747", "748"),
            listOf("সৈকত", "shaikat") to listOf("821", "822"),
            listOf("দোলনচাঁপা", "dolonchapa") to listOf("767", "768"),
            listOf("মেঘনা", "meghna") to listOf("729", "730"),
            listOf("তিতুমীর", "titumir") to listOf("733", "734"),
            listOf("বরেন্দ্র", "barendra") to listOf("731", "732"),
            listOf("রকেট", "rocket") to listOf("23", "24"),
            listOf("রূপসী বাংলা", "ruposhi-bangla") to listOf("827", "828"),
            listOf("বেনাপোল এক্সপ্রেস", "benapole-express") to listOf("795", "796"),
            listOf("বাংলাবান্ধা", "banglabandha") to listOf("803", "804"),
            listOf("পাহাড়িকা", "পাহাড়িকা", "paharika") to listOf("719", "720"),
            listOf("উদয়ন", "উদয়ন", "udayan") to listOf("723", "724"),
            listOf("নরসিংদী কমিউটার", "narshindi-commuter") to listOf("101", "102"),
            listOf("তুরাগ কমিউটার", "turag-commuter") to listOf("69", "70", "71", "72"),
            listOf("নারায়ণগঞ্জ কমিউটার", "নারায়ণগঞ্জ কমিউটার", "narayanganj-commuter") to listOf("121", "122", "123", "124", "125", "126", "127", "128"),
            listOf("মহুয়া কমিউটার", "মহুয়া কমিউটার", "mohua-commuter") to listOf("43", "44"),
            listOf("জামালপুর কমিউটার", "jamalpur-commuter") to listOf("51", "52"),
            listOf("কর্ণফুলী কমিউটার", "karnaphuli-commuter") to listOf("3", "4"),
            listOf("বলাকা কমিউটার", "balaka-commuter") to listOf("49", "50"),
            listOf("সাগরিকা", "sagorika") to listOf("29", "30"),
            listOf("কাঞ্চন কমিউটার", "kanchan-commuter") to listOf("41", "42"),
            listOf("নকশিকাঁথা", "নকশীকাঁথা", "nokshikanta") to listOf("25", "26"),
            listOf("ঢাকা/চট্টগ্রাম মেইল", "dhaka-chattagram-mail") to listOf("1", "2"),
            listOf("ময়মনসিংহ/নাসিরাবাদ", "mymensingh-express-mail") to listOf("37", "38", "75", "76"),
            listOf("সুবর্ণ", "suborno") to listOf("701", "702"),
            listOf("সোনার বাংলা", "sonar-bangla") to listOf("787", "788"),
            listOf("কক্সবাজার এক্সপ্রেস", "coxs-bazar") to listOf("813", "814"),
            listOf("পর্যটক", "parjatak") to listOf("815", "816")
        )

        for ((keys, trainNums) in keywordPairs) {
            if (keys.any { k -> nameRaw.contains(k, ignoreCase = true) || slug.contains(k, ignoreCase = true) }) {
                return trainNums
            }
        }
        return emptyList()
    }

    private fun parseDetailBulletinFromHtml(html: String): Triple<String?, String?, String?>? {
        val postRegex = Regex("""<p id="post-content-\d+">(.*?)</p>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val match = postRegex.find(html) ?: return null
        val rawHtml = match.groupValues[1]
            .replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), " • ")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace("&nbsp;", " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (rawHtml.isBlank()) return null

        // Scrub any domain or third-party site references if ever present
        val cleanedBulletin = rawHtml
            .replace(Regex("""https?://\S+"""), "")
            .replace("trainlivelocation", "", ignoreCase = true)
            .replace("trainkoi", "", ignoreCase = true)
            .trim()

        var currentSt: String? = null
        var nextSt: String? = null
        var etaText: String? = null

        val nextMatch = Regex("""সামনের স্টেশন\s*[-–:]\s*([^•⏰📍\r\n]+)""").find(cleanedBulletin)
        if (nextMatch != null) {
            nextSt = nextMatch.groupValues[1].trim()
        }

        val etaMatch = Regex("""আনুমানিক পৌঁছাবে\s*[:\-–]\s*([^•📍\r\n]+)""").find(cleanedBulletin)
        if (etaMatch != null) {
            etaText = etaMatch.groupValues[1].trim()
        }

        val currMatch = Regex("""(?:রাত|সকাল|দুপুর|বিকাল|সন্ধ্যা|ভোর)?\s*[০-৯0-9:]+\s*([^\s:•📍]+(?:\s+[^\s:•📍]+)?)\s*(?:অবস্থান করছে|অতিক্রম করছে|স্টেশনে)""").find(cleanedBulletin)
        if (currMatch != null) {
            currentSt = currMatch.groupValues[1].trim()
        }

        return Triple(cleanedBulletin, currentSt ?: nextSt, etaText)
    }

    suspend fun fetchLiveTrainReports(selectedTrainNumber: String? = null): Map<String, LiveServerTrainReport> =
        withContext(Dispatchers.IO) {
            val result = mutableMapOf<String, LiveServerTrainReport>()
            try {
                val runningJsonStr = fetchUrlText(ENDPOINT_RUNNING_TRAINS, 8000)
                val itemsToProcess = mutableListOf<org.json.JSONObject>()

                if (!runningJsonStr.isNullOrBlank() && runningJsonStr.trim().startsWith("[")) {
                    val arr = JSONArray(runningJsonStr)
                    for (i in 0 until arr.length()) {
                        arr.optJSONObject(i)?.let { itemsToProcess.add(it) }
                    }
                }

                // Also parse allTrains from homepage if running-trains-api missed any train
                val homeHtml = fetchUrlText(ENDPOINT_HOMEPAGE, 8000)
                if (!homeHtml.isNullOrBlank()) {
                    val m = Regex("""allTrains:\s*(\[.*?\])\s*,\r?\n""").find(homeHtml)
                    if (m != null) {
                        val unescaped = Html.fromHtml(m.groupValues[1], Html.FROM_HTML_MODE_LEGACY).toString()
                        if (unescaped.trim().startsWith("[")) {
                            val allArr = JSONArray(unescaped)
                            for (i in 0 until allArr.length()) {
                                allArr.optJSONObject(i)?.let { itemsToProcess.add(it) }
                            }
                        }
                    }
                }

                for (obj in itemsToProcess) {
                    val name = obj.optString("name", "")
                    val slug = obj.optString("slug", "")
                    val lateStr = obj.optString("latest_late_time", "")
                    val diffStr = obj.optString("last_update_diff", "")
                    if (name.isBlank() || slug.isBlank()) continue

                    val delayMin = parseDelayMinutes(lateStr)
                    val (updatedAgoBn, isRecent) = translateUpdateDiffToBn(diffStr)
                    val matchedNums = extractMatchedTrainNumbers(name, slug)

                    for (tNum in matchedNums) {
                        slugCacheByTrainNumber[tNum] = slug
                        if (!result.containsKey(tNum)) {
                            result[tNum] = LiveServerTrainReport(
                                trainNumber = tNum,
                                slug = slug,
                                delayMinutes = delayMin,
                                delayLabelBn = if (lateStr.isNotBlank() && lateStr != "null" && lateStr != "None") lateStr else "নির্ধারিত সময়ে",
                                updatedAgoBn = updatedAgoBn,
                                isRecentlyUpdated = isRecent
                            )
                        }
                    }
                }

                // Fetch detailed station bulletin for the selected train (and Titas / top active trains)
                val priorityNums = buildList {
                    if (!selectedTrainNumber.isNullOrBlank()) add(selectedTrainNumber)
                    addAll(listOf("33", "34", "35", "36", "802", "703", "760", "707", "708", "749", "750"))
                }.distinct()

                val nowMs = System.currentTimeMillis()
                for (tNum in priorityNums) {
                    val slug = result[tNum]?.slug ?: slugCacheByTrainNumber[tNum] ?: continue
                    val cached = detailBulletinCache[slug]
                    val parsedTriple = if (cached != null && (nowMs - cached.first) < 90_000L) {
                        cached.second
                    } else {
                        val detailHtml = fetchUrlText("$ENDPOINT_TRAIN_DETAIL_PREFIX$slug", 6000)
                        val extracted = if (!detailHtml.isNullOrBlank()) parseDetailBulletinFromHtml(detailHtml) else null
                        if (extracted != null) {
                            detailBulletinCache[slug] = Pair(nowMs, extracted)
                        }
                        extracted
                    }

                    if (parsedTriple != null) {
                        val (bulletin, stationHint, etaHint) = parsedTriple
                        val nextHint = bulletin?.let {
                            Regex("""সামনের স্টেশন\s*[-–:]\s*([^•⏰📍\r\n]+)""").find(it)?.groupValues?.get(1)?.trim()
                        }
                        val currHint = bulletin?.let {
                            Regex("""(?:রাত|সকাল|দুপুর|বিকাল|সন্ধ্যা|ভোর)?\s*[০-৯0-9:]+\s*([^\s:•📍]+(?:\s+[^\s:•📍]+)?)\s*(?:অবস্থান করছে|অতিক্রম করছে|স্টেশনে)""").find(it)?.groupValues?.get(1)?.trim()
                        }
                        // Update all train numbers sharing this slug
                        result.keys.filter { result[it]?.slug == slug }.forEach { keyNum ->
                            val existing = result[keyNum] ?: return@forEach
                            result[keyNum] = existing.copy(
                                liveStationBulletinBn = bulletin,
                                reportedCurrentStationBn = currHint ?: stationHint,
                                reportedNextStationBn = nextHint,
                                reportedEtaBn = etaHint
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Fail silently if offline; schedule telemetry still operates accurately
            }
            result
        }
}
