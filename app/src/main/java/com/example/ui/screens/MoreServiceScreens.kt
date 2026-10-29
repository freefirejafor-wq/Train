package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BangladeshRailwayCatalog
import com.example.data.ComplaintEntity
import com.example.ui.RailPulseUiState
import com.example.ui.theme.CrimsonExpress
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.SignalGold
import java.util.Calendar
import java.util.TimeZone

/**
 * 1. রেল নিরাপত্তা (Rail Safety Interactive Guide & Emergency Alerts)
 */
@Composable
fun RailSafetyScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("rail_safety_screen")
    ) {
        ScreenTopHeader(
            titleBn = "রেল নিরাপত্তা ও জীবনরক্ষা বিধি",
            subtitleBn = "ট্রেন, প্ল্যাটফর্ম ও লেভেল ক্রসিংয়ে নিরাপদ থাকার জরুরি নির্দেশনা",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CrimsonExpress)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "জরুরি রেল দুর্ঘটনা বা বিপদে কল করুন",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "জাতীয় জরুরি সেবা: 999 • রেলওয়ে হেল্পলাইন: 131",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:999")))
                                } catch (_: Exception) {
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = CrimsonExpress),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("999 কল", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(BangladeshRailwayCatalog.safetyRules) { rule ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (rule.isCritical) Icons.Default.Warning else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (rule.isCritical) CrimsonExpress else EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = rule.titleBn,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = rule.subtitleBn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "💡 করণীয়: ${rule.emergencyTipBn}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. নির্দেশিকা (Travel & E-Ticket Rules Guide)
 */
@Composable
fun TravelGuideScreen(onBack: () -> Unit) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val guides = listOf(
        "১. অনলাইন ই-টিকিট কাটার নিয়ম" to "বাংলাদেশ রেলওয়ের অফিশিয়াল ই-টিকিট সিস্টেম বা Rail Sheba অ্যাপে জাতীয় পরিচয়পত্র (NID) দিয়ে নিবন্ধন করে ১০ দিন আগে থেকে টিকিট কাটা যায়। একজন যাত্রী সর্বোচ্চ ৪টি টিকিট কাটতে পারেন।",
        "২. টিকিট রিফান্ড ও বাতিল চার্জ" to "ট্রেন ছাড়ার ৪৮ ঘণ্টা আগে বাতিল করলে ১০% (সর্বনিম্ন ৪০ টাকা), ২৪ থেকে ৪৮ ঘণ্টা আগে ২৫%, ১২ থেকে ২৪ ঘণ্টা আগে ৫০% এবং ৬ থেকে ১২ ঘণ্টা আগে ৭৫% চার্জ কর্তন করে বাকি টাকা ফেরত দেওয়া হয়। ৬ ঘণ্টার কম সময়ে রিফান্ড প্রযোজ্য নয়।",
        "৩. শিশু ও প্রবীণ যাত্রীদের নিয়ম" to "৩ বছর বয়স পর্যন্ত শিশুদের কোনো টিকিট লাগে না। ৩ থেকে ১২ বছর বয়সী শিশুদের জন্য অপ্রাপ্তবয়স্ক (Minor) টিকিট প্রযোজ্য। ভ্রমণের সময় মূল এনআইডি বা জন্মসনদের ফটোকপি সাথে রাখা বাধ্যতামূলক।",
        "৪. বিনা মাশুলে লাগেজ বহন সীমা" to "এসি বার্থে সর্বোচ্চ ৫৬ কেজি, স্নিগ্ধা/এসি সিটে ৩৭.৫ কেজি এবং শোভন চেয়ার শ্রেণিতে ২৮ কেজি পর্যন্ত ব্যক্তিগত ব্যাগ সাথে নেওয়া যাবে। অতিরিক্ত মালের জন্য লাগেজ ভ্যানে বুকিং দিতে হবে।"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("travel_guide_screen")
    ) {
        ScreenTopHeader(
            titleBn = "যাত্রী নির্দেশিকা ও টিকিট নিয়মাবলি",
            subtitleBn = "ই-টিকিট, রিফান্ড চার্জ এবং লাগেজ বহনের অফিশিয়াল নির্দেশনা",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF063928))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎫 বাংলাদেশ রেলওয়ে অফিশিয়াল ই-টিকিট সার্ভিস",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SignalGold
                        )
                        Text(
                            text = "আসন খালি আছে কি না দেখতে বা অনলাইনে টিকিট বুকিং করতে নিচের বাটনে ট্যাপ করুন:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://eticket.railway.gov.bd/"))
                                    )
                                } catch (_: Exception) {
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SignalGold, contentColor = Color(0xFF1F1300)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("অফিশিয়াল ই-টিকিট পোর্টাল খুলুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(guides) { (title, body) ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/**
 * 3. প্রশ্নোত্তর (Expandable Interactive FAQ)
 */
@Composable
fun FaqScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("faq_screen")
    ) {
        ScreenTopHeader(
            titleBn = "সচরাচর জিজ্ঞাসিত প্রশ্নোত্তর (FAQ)",
            subtitleBn = "ট্রেন ট্র্যাকিং, 3D ভিউ ও টিকিট সংক্রান্ত সকল প্রশ্নের উত্তর",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(BangladeshRailwayCatalog.faqList) { faq ->
                var expanded by remember { mutableStateOf(true) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(18.dp),
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
                                    text = faq.categoryBn,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = faq.questionBn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                        AnimatedVisibility(visible = expanded) {
                            Text(
                                text = faq.answerBn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 4. অভিযোগ ও জরুরি হেল্পলাইন (Complaints & SOS Helpline)
 */
@Composable
fun ComplaintSosScreen(
    complaints: List<ComplaintEntity>,
    onBack: () -> Unit,
    onSubmitComplaint: (String, String, String, String) -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var category by remember { mutableStateOf("এসি বা ফ্যান কাজ করছে না") }
    var trainOrStation by remember { mutableStateOf("সুবর্ণ এক্সপ্রেস (৭০১)") }
    var coachNum by remember { mutableStateOf("বগি - গ") }
    var description by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("complaint_sos_screen")
    ) {
        ScreenTopHeader(
            titleBn = "অভিযোগ ও জরুরি রেলওয়ে সহায়তা",
            subtitleBn = "রেলওয়ে হেল্পলাইন (131), পুলিশ (999) ও যাত্রী অভিযোগ বক্স",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:131")))
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("রেল সেবা: 131")
                    }
                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:999")))
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonExpress)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("জরুরি পুলিশ: 999")
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "নতুন অভিযোগ দাখিল করুন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("অভিযোগের বিষয়") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = trainOrStation,
                            onValueChange = { trainOrStation = it },
                            label = { Text("ট্রেন বা স্টেশনের নাম") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = coachNum,
                            onValueChange = { coachNum = it },
                            label = { Text("বগি ও সিট নম্বর") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("বিস্তারিত বিবরণ লিখুন...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_complaint_desc")
                        )
                        Button(
                            onClick = {
                                if (description.isNotBlank()) {
                                    onSubmitComplaint(category, trainOrStation, coachNum, description.trim())
                                    description = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_submit_complaint"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("অভিযোগ জমা দিন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (complaints.isNotEmpty()) {
                item {
                    Text(
                        text = "আপনার দাখিলকৃত অভিযোগসমূহ (${complaints.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(complaints, key = { it.id }) { comp ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(comp.categoryBn, fontWeight = FontWeight.Bold)
                                Text(comp.statusBn, color = EmeraldPrimary, fontSize = 12.sp)
                            }
                            Text(
                                text = "${comp.trainOrStationBn} • ${comp.coachNumberBn}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(comp.descriptionBn, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. পরবর্তী ট্রেন (Next Departing Train Live Countdown Finder)
 */
@Composable
fun NextTrainScreen(
    uiState: RailPulseUiState,
    onBack: () -> Unit,
    onSelectStation: (String) -> Unit,
    onOpenTrain3D: (String) -> Unit
) {
    BackHandler { onBack() }

    val selectedStation = BangladeshRailwayCatalog.allStations.find { it.code == uiState.selectedStationCode }
        ?: BangladeshRailwayCatalog.allStations.first()

    val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+6"))
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    val upcomingTrains = BangladeshRailwayCatalog.allTrains.mapNotNull { tr ->
        val stop = tr.stops.find { it.stationCode == selectedStation.code } ?: return@mapNotNull null
        val parts = stop.departureTime.split(":")
        val depMin = (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
        val diffMin = if (depMin >= nowMin) depMin - nowMin else (1440 - nowMin) + depMin
        Triple(tr, stop, diffMin)
    }.sortedBy { it.third }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("next_train_screen")
    ) {
        ScreenTopHeader(
            titleBn = "পরবর্তী ট্রেন কাউন্টডাউন",
            subtitleBn = "${selectedStation.nameBn} থেকে পরবর্তী কোন ট্রেন কতক্ষণ পর ছাড়বে",
            onBack = onBack
        )

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(upcomingTrains) { (train, stop, diffMin) ->
                val hours = diffMin / 60
                val mins = diffMin % 60
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTrain3D(train.trainNumber) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                                text = "${train.corridorBn} • প্ল্যাটফর্ম ${stop.platformNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ছাড়ার সময়: ${stop.departureTime} • বন্ধ: ${train.offDayBn}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            color = Color(0xFF062C21),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = SignalGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${hours}ঘ ${mins}মি",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SignalGold
                                    )
                                }
                                Text(
                                    text = "বাকি আছে",
                                    fontSize = 10.sp,
                                    color = Color(0xFFA7F3D0)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
