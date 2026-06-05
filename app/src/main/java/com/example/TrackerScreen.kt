package com.example

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.database.DailyTracker
import com.example.viewmodel.TrackerViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun Int.toBengaliDigits(): String {
    if (com.example.viewmodel.GlobalLanguage.isEnglish) return this.toString()
    val eng = this.toString()
    val ben = listOf("০", "১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯")
    var res = ""
    for (char in eng) {
        val digit = char.toString().toIntOrNull()
        if (digit != null) {
            res += ben[digit]
        } else {
            res += char
        }
    }
    return res
}

@Composable
fun TrackerScreen() {
    val trackerViewModel: TrackerViewModel = viewModel()
    val state by trackerViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF9FAFB))
            .padding(bottom = 32.dp)
    ) {
        // Tracker Screen Title Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(PrimaryGreen.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Amal Tracker" else "আমল ট্র্যাকার",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Track your daily prayers, Quran reading and Dhikr to build habits." else "আপনার প্রতিদিনের সালাত, কুরআন ও জিকির ট্র্যাক করে অভ্যাস গড়ে তুলুন।",
                    fontSize = 13.sp,
                    color = TextGray,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Date Picker & Today Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { trackerViewModel.changeDate(-1) },
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day", tint = TextDark)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.dateStringFormatted,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        fontSize = 14.sp
                    )
                }
            }

            IconButton(
                onClick = { trackerViewModel.changeDate(1) },
                modifier = Modifier
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Day", tint = TextDark)
            }
        }

        // Jump to Today Shortcut
        val isToday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(state.selectedDate) ==
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        if (!isToday) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { trackerViewModel.jumpToToday() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    elevation = null,
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Back to Today" else "আজকের দিনে ফিরে যান", color = PrimaryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Prayer Tracker Completion Card
        PrayerProgressCard(completedCount = state.totalPrayersCompleted)

        Spacer(modifier = Modifier.height(24.dp))

        // Obligatory Prayers Checklist (৫ ওয়াক্ত ফরজ সালাত)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "5 Daily Fard Salah" else "৫ ওয়াক্ত ফরজ সালাত",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark
                )
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Check off the prayers you have performed today" else "আজ আপনার পড়া আদায়কৃত সালাতগুলো চেক করুন",
                    fontSize = 12.sp,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val prayers = listOf(
                    Triple("Fajr", "ফজর সালাত", "Fajr Prayer"),
                    Triple("Dhuhr", "যোহর সালাত", "Dhuhr Prayer"),
                    Triple("Asr", "আসর সালাত", "Asr Prayer"),
                    Triple("Maghrib", "মাগরিব সালাত", "Maghrib Prayer"),
                    Triple("Isha", "এশা সালাত", "Isha Prayer")
                )

                prayers.forEachIndexed { index, prayer ->
                    val isChecked = when (prayer.first) {
                        "Fajr" -> state.currentTracker.fajr
                        "Dhuhr" -> state.currentTracker.dhuhr
                        "Asr" -> state.currentTracker.asr
                        "Maghrib" -> state.currentTracker.maghrib
                        "Isha" -> state.currentTracker.isha
                        else -> false
                    }

                    PrayerTrackerRow(
                        titleBen = prayer.second,
                        titleEng = prayer.third,
                        isChecked = isChecked,
                        onToggle = { trackerViewModel.togglePrayer(prayer.first) }
                    )

                    if (index < prayers.size - 1) {
                        Divider(
                            color = Color(0xFFF3F4F6),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Digital Live Tasbih Counter Card (লাইভ জিকির ও তসবীহ)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Live Digital Tasbih" else "লাইভ ডিজিটাল তসবীহ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark
                        )
                        Text(
                            text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Count Sunnah Dhikr like Subhanallah, Alhamdulillah" else "সুন্নাহ জিকির যেমন সুবহানাল্লাহ, আলহামদুলিল্লাহ কাউন্ট করুন",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                    IconButton(
                        onClick = { trackerViewModel.resetTasbih() },
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset Tasbih",
                            tint = TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tasbih Dial count display
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(130.dp)
                        .background(PrimaryGreen.copy(alpha = 0.05f), CircleShape)
                        .border(4.dp, PrimaryGreen.copy(alpha = 0.15f), CircleShape)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.currentTracker.tasbihCount.toBengaliDigits(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = PrimaryGreen
                        )
                        Text(
                            text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Times" else "বার জপ",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Massive Increment Tally Button
                Button(
                    onClick = { trackerViewModel.incrementTasbih() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Tap to Dhikr" else "জিকির করুন (টিপুন)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Daily Good Deed Habits (নেক আমল ট্র্যাকার)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Daily Good Deeds" else "দৈনন্দিন নেক আমল",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark
                )
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Keep track of other good activities daily" else "প্রতিদিনের অন্যান্য শুভ ও নেক কার্যক্রম সংরক্ষণ করুন",
                    fontSize = 12.sp,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val habits = listOf(
                    Triple("quran", "আজকে কুরআন তিলাওয়াত করেছি", Icons.Outlined.MenuBook),
                    Triple("charity", "আজকে দান-সদকাহ বা সাহায্য করেছি", Icons.Outlined.Handshake),
                    Triple("reading", "দ্বীনি বই পড়ে জ্ঞান লাভ করেছি", Icons.Outlined.AutoStories),
                    Triple("istighfar", "আজকে শতবার ইস্তিগফার করেছি", Icons.Outlined.Favorite),
                    Triple("parents", "বাবা-মায়ের খোঁজ নিয়েছি বা সেবা করেছি", Icons.Outlined.People)
                )

                habits.forEachIndexed { index, habit ->
                    val isChecked = when (habit.first) {
                        "quran" -> state.currentTracker.quran
                        "charity" -> state.currentTracker.charity
                        "reading" -> state.currentTracker.reading
                        "istighfar" -> state.currentTracker.istighfar
                        "parents" -> state.currentTracker.parents
                        else -> false
                    }

                    HabitTrackerRow(
                        title = habit.second,
                        icon = habit.third,
                        isChecked = isChecked,
                        onToggle = { trackerViewModel.toggleHabit(habit.first) }
                    )

                    if (index < habits.size - 1) {
                        Divider(
                            color = Color(0xFFF3F4F6),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Awesome Weekly Insights / Streak Matrix
        Text(
            text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Recent History (Last 7 Days)" else "বিগত দিনের রিপোর্ট (সর্বশেষ ৭ দিন)",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextDark,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (state.history.isEmpty()) {
                    Text(
                        text = "পর্যাপ্ত ট্র্যাকিং ডাটা উপলব্ধ নয়। প্রতিদিন আমল পূরণ করুন!",
                        fontSize = 12.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.history.take(7).reversed()) { historyItem ->
                            HistoryDayItem(historyItem)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerProgressCard(completedCount: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Salah Progress" else "সালাত অগ্রগতি",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Your performed prayers out of 5 daily fard prayers." else "৫ ওয়াক্ত ফরজ নামাজের মধ্যে আপনার আদায়কৃত নামাজ।",
                    fontSize = 12.sp,
                    color = TextGray,
                    modifier = Modifier.padding(end = 12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Completed ${completedCount} of 5" else "৫ এর মধ্যে ${completedCount.toBengaliDigits()} টি সম্পন্ন",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(76.dp),
                    color = Color(0xFFF3F4F6),
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                CircularProgressIndicator(
                    progress = { completedCount / 5f },
                    modifier = Modifier.size(76.dp),
                    color = PrimaryGreen,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${((completedCount / 5f) * 100).toInt().toBengaliDigits()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }
        }
    }
}

@Composable
fun PrayerTrackerRow(
    titleBen: String,
    titleEng: String,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onToggle,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (titleBen.contains("ফজর") || titleBen.contains("মাগরিব") || titleBen.contains("এশা")) 
                    Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                contentDescription = null,
                tint = if (isChecked) PrimaryGreen else TextGray,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = titleBen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = titleEng,
                    fontSize = 11.sp,
                    color = TextGray
                )
            }
        }

        Icon(
            imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = "Toggle Status",
            tint = if (isChecked) PrimaryGreen else Color(0xFFD1D5DB),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun HabitTrackerRow(
    title: String,
    icon: ImageVector,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onToggle,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isChecked) PrimaryGreen else TextGray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Icon(
            imageVector = if (isChecked) Icons.Filled.CheckCircle else Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = "Toggle Habit",
            tint = if (isChecked) PrimaryGreen else Color(0xFFD1D5DB),
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun HistoryDayItem(history: DailyTracker) {
    val dateParts = history.date.split("-")
    val displayDay = if (dateParts.size == 3) {
        dateParts[2].toIntOrNull()?.toBengaliDigits() ?: history.date
    } else {
        history.date
    }

    var checkCount = 0
    if (history.fajr) checkCount++
    if (history.dhuhr) checkCount++
    if (history.asr) checkCount++
    if (history.maghrib) checkCount++
    if (history.isha) checkCount++

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.width(68.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "$displayDay Date" else "$displayDay তারিখ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // Check list dots representing Fajr Dhuhr Asr Maghrib Isha
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                historyDot(history.fajr)
                historyDot(history.dhuhr)
                historyDot(history.asr)
                historyDot(history.maghrib)
                historyDot(history.isha)
            }
            
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "$checkCount/5 Times" else "$checkCount/৫ ওয়াক্ত",
                fontSize = 9.sp,
                color = if (checkCount == 5) PrimaryGreen else TextGray,
                fontWeight = if (checkCount == 5) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun historyDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .background(
                color = if (active) PrimaryGreen else Color(0xFFD1D5DB),
                shape = CircleShape
            )
    )
}
