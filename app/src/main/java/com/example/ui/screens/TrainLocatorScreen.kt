package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sms
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.BangladeshRailwayCatalog
import com.example.ui.RailPulseUiState
import com.example.ui.theme.CrimsonExpress
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.SignalGold

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TrainLocatorScreen(
    uiState: RailPulseUiState,
    onBack: () -> Unit,
    onSelectTrainFor3D: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    // 0 = Native BD Radar Map, 1..4 = White-labeled Encrypted Live GPS Channels
    var selectedTab by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoadingWeb by remember { mutableStateOf(true) }
    var locatorSearchQuery by remember { mutableStateOf("") }
    var locatorCategory by remember { mutableStateOf("ALL") }

    val activePortalUrl = when (selectedTab) {
        1 -> "https://www.trainlivelocation.com/"
        2 -> "https://www.trainkoi.com/"
        3 -> "https://trainkothai.com/"
        4 -> "https://railwayticket.bd/live-train-tracking/"
        else -> "https://www.trainlivelocation.com/"
    }

    val activeChannelName = when (selectedTab) {
        1 -> "জিপিএস সার্ভার ১"
        2 -> "জিপিএস সার্ভার ২"
        3 -> "জিপিএস সার্ভার ৩"
        4 -> "জিপিএস সার্ভার ৪"
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("train_locator_screen")
    ) {
        Surface(color = Color(0xFF04241A), modifier = Modifier.fillMaxWidth()) {
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
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ট্রেন লোকেটর (${BangladeshRailwayCatalog.allTrains.size}টি ট্রেন)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "রেলপালস বিডি স্যাটেলাইট ও মাল্টি-সার্ভার জিপিএস ট্র্যাকার",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }

                    if (selectedTab != 0) {
                        IconButton(onClick = { webViewRef?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "রিলোড", tint = SignalGold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5-Mode White-Labeled Multi-Server Switcher (No site or API names exposed)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, "রেল রাডার ম্যাপ", "tab_native_radar"),
                        Triple(1, "জিপিএস সার্ভার ১", "tab_gps_server_1"),
                        Triple(2, "জিপিএস সার্ভার ২", "tab_gps_server_2"),
                        Triple(3, "জিপিএস সার্ভার ৩", "tab_gps_server_3"),
                        Triple(4, "জিপিএস সার্ভার ৪", "tab_gps_server_4")
                    )

                    tabs.forEach { (idx, title, tag) ->
                        val isSel = selectedTab == idx
                        Surface(
                            modifier = Modifier
                                .clickable {
                                    if (idx != 0) {
                                        isLoadingWeb = true
                                        val targetUrl = when (idx) {
                                            1 -> "https://www.trainlivelocation.com/"
                                            2 -> "https://www.trainkoi.com/"
                                            3 -> "https://trainkothai.com/"
                                            else -> "https://railwayticket.bd/live-train-tracking/"
                                        }
                                        webViewRef?.loadUrl(targetUrl)
                                    }
                                    selectedTab = idx
                                }
                                .testTag(tag),
                            color = if (isSel) SignalGold else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (idx == 0) Icons.Default.Map else Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSel) Color(0xFF1F1300) else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSel) Color(0xFF1F1300) else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(270.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF051C15))
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            fun project(lat: Float, lng: Float): Offset {
                                val x = ((lng - 88.2f) / (92.4f - 88.2f)).coerceIn(0.06f, 0.94f) * w
                                val y = (1f - ((lat - 21.1f) / (26.6f - 21.1f)).coerceIn(0.06f, 0.94f)) * h
                                return Offset(x, y)
                            }

                            for (i in 1..6) {
                                drawLine(
                                    Color(0x1826D092),
                                    Offset(w * i / 7f, 0f),
                                    Offset(w * i / 7f, h),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    Color(0x1826D092),
                                    Offset(0f, h * i / 7f),
                                    Offset(w, h * i / 7f),
                                    strokeWidth = 1f
                                )
                            }

                            BangladeshRailwayCatalog.allTrains.forEach { tr ->
                                for (i in 0 until tr.stops.size - 1) {
                                    val p1 = project(tr.stops[i].lat, tr.stops[i].lng)
                                    val p2 = project(tr.stops[i + 1].lat, tr.stops[i + 1].lng)
                                    drawLine(
                                        color = Color(0xFF1F6E50),
                                        start = p1,
                                        end = p2,
                                        strokeWidth = 2.5f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                    )
                                }
                            }

                            BangladeshRailwayCatalog.allStations.forEach { st ->
                                val pt = project(st.lat, st.lng)
                                drawCircle(Color(0xFF80CBC4), radius = 4f, center = pt)
                            }

                            uiState.allTelemetries.values.forEach { tel ->
                                val pt = project(tel.currentLat, tel.currentLng)
                                val isSelected = tel.train.trainNumber == uiState.selectedTrainNumber
                                val dotColor = when {
                                    isSelected -> SignalGold
                                    tel.isOffToday -> CrimsonExpress
                                    else -> LiveGreen
                                }
                                drawCircle(
                                    color = dotColor.copy(alpha = 0.28f),
                                    radius = if (isSelected) 16f else 10f,
                                    center = pt
                                )
                                drawRoundRect(
                                    color = dotColor,
                                    topLeft = Offset(pt.x - 6f, pt.y - 4f),
                                    size = Size(12f, 8f),
                                    cornerRadius = CornerRadius(3f, 3f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color(0xCC03140E))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🟢 সক্রিয় ফ্লিট: ${BangladeshRailwayCatalog.allTrains.size}টি আন্তঃনগর, মেইল ও কমিউটার ট্রেন",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                            Text(
                                text = uiState.currentBstClock,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = SignalGold
                            )
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "সারা বাংলাদেশের সকল ট্রেনের লাইভ তালিকা (${BangladeshRailwayCatalog.allTrains.size}টি)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = locatorSearchQuery,
                            onValueChange = { locatorSearchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("ট্রেনের নাম, নম্বর বা স্টেশন খুঁজুন — যেমন তিতাস, ৩৩, ৭০১") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (locatorSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { locatorSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val cats = listOf(
                                "ALL" to "সব ট্রেন (${BangladeshRailwayCatalog.allTrains.size})",
                                "COMMUTER" to "তিতাস ও কমিউটার",
                                "INTERCITY" to "আন্তঃনগর",
                                "MAIL" to "মেইল ও শাটল"
                            )
                            cats.forEach { (key, label) ->
                                val isSel = locatorCategory == key
                                Surface(
                                    color = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.clickable { locatorCategory = key }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                val displayedTrains = BangladeshRailwayCatalog.allTrains.filter { tr ->
                    val matchesCategory = when (locatorCategory) {
                        "COMMUTER" -> tr.nameBn.contains("কমিউটার") || tr.nameBn.contains("তিতাস") || tr.nameEn.contains("Commuter", ignoreCase = true)
                        "INTERCITY" -> (tr.trainNumber.toIntOrNull() ?: 0) >= 700
                        "MAIL" -> {
                            val num = tr.trainNumber.toIntOrNull() ?: 0
                            num in 1..200 && (tr.nameBn.contains("মেইল") || tr.nameBn.contains("শাটল") || tr.nameBn.contains("এক্সপ্রেস"))
                        }
                        else -> true
                    }
                    val q = locatorSearchQuery.trim()
                    val matchesQuery = q.isEmpty() ||
                        tr.trainNumber.contains(q, ignoreCase = true) ||
                        tr.trainNumberBn.contains(q) ||
                        tr.nameBn.contains(q, ignoreCase = true) ||
                        tr.nameEn.contains(q, ignoreCase = true) ||
                        tr.corridorBn.contains(q, ignoreCase = true) ||
                        tr.stops.any { st ->
                            st.nameBn.contains(q, ignoreCase = true) ||
                                st.nameEn.contains(q, ignoreCase = true)
                        }
                    matchesCategory && matchesQuery
                }

                items(displayedTrains, key = { it.trainNumber }) { train ->
                    val tel = uiState.allTelemetries[train.trainNumber]
                        ?: BangladeshRailwayCatalog.calculateLiveTelemetry(train)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTrainFor3D(train.trainNumber) },
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
                                        text = tel.statusBadgeBn,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when {
                                            tel.isOffToday -> CrimsonExpress
                                            tel.isRunningNow -> EmeraldPrimary
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    if (!tel.liveServerReportBn.isNullOrBlank()) {
                                        Text(
                                            text = "📡 ${tel.liveServerReportBn}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EmeraldPrimary
                                        )
                                    } else {
                                        Text(
                                            text = if (tel.isRunningNow) {
                                                "পরবর্তী স্টেশন আসবে: ${tel.expectedNextArrivalFormatted} • ছাড়বে: ${tel.expectedNextDepartureFormatted}"
                                            } else {
                                                "নির্ধারিত ছাড়ে: ${tel.scheduledDepartureFormatted} • পৌঁছায়: ${tel.scheduledArrivalFormatted}"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        color = when {
                                            tel.isOffToday -> CrimsonExpress.copy(alpha = 0.14f)
                                            tel.isRunningNow -> EmeraldPrimary.copy(alpha = 0.14f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = when {
                                                tel.isOffToday -> "আজ বন্ধ"
                                                tel.isRunningNow -> if (tel.currentSpeedKmh > 0) "${tel.currentSpeedKmh} km/h" else "স্টেশনে থামা"
                                                else -> "0 km/h"
                                            },
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = when {
                                                tel.isOffToday -> CrimsonExpress
                                                tel.isRunningNow -> EmeraldPrimary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(50),
                                            modifier = Modifier.clickable {
                                                try {
                                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:16318")).apply {
                                                        putExtra("sms_body", "TR ${train.trainNumber}")
                                                    }
                                                    context.startActivity(smsIntent)
                                                } catch (_: Exception) {
                                                }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("16318 SMS", fontSize = 10.sp)
                                            }
                                        }
                                        Text(
                                            text = "3D ভিউ ➔",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // White-Labeled Live GPS Channel (No external site or API names shown)
            Column(modifier = Modifier.fillMaxSize()) {
                if (isLoadingWeb) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = SignalGold
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛰️ সরাসরি $activeChannelName রিয়েল-টাইম নেটওয়ার্ক সংযুক্ত",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Button(
                            onClick = { selectedTab = 0 },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("নেটিভ ম্যাপে ফিরুন", fontSize = 11.sp)
                        }
                    }
                }

                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            webViewClient = object : WebViewClient() {
                                private fun injectWhiteLabelMask(view: WebView?) {
                                    val js = """
                                        (function() {
                                            var style = document.getElementById('rp-whitelabel-style');
                                            if (!style) {
                                                style = document.createElement('style');
                                                style.id = 'rp-whitelabel-style';
                                                style.innerHTML = 'header, footer, nav, .navbar, .site-header, .site-footer, .brand, .logo, .ads, .adsbygoogle, [class*="cookie"], [class*="banner-ad"] { display: none !important; } body { padding-top: 0 !important; margin-top: 0 !important; }';
                                                document.head.appendChild(style);
                                            }
                                            document.title = 'RailPulse BD Live GPS';
                                            var regex = /(trainlivelocation\.com|trainlivelocation|trainkoi\.com|trainkoi|trainkothai\.com|trainkothai|railwayticket\.bd|railwayticket)/gi;
                                            var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, null, false);
                                            var node;
                                            while ((node = walker.nextNode())) {
                                                if (regex.test(node.nodeValue)) {
                                                    node.nodeValue = node.nodeValue.replace(regex, 'RailPulse BD');
                                                }
                                            }
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(js, null)
                                }

                                override fun onPageCommitVisible(view: WebView?, url: String?) {
                                    super.onPageCommitVisible(view, url)
                                    injectWhiteLabelMask(view)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    injectWhiteLabelMask(view)
                                    isLoadingWeb = false
                                }
                            }
                            webChromeClient = WebChromeClient()
                            loadUrl(activePortalUrl)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        if (view.url != activePortalUrl) {
                            view.loadUrl(activePortalUrl)
                        }
                    }
                )
            }
        }
    }
}
