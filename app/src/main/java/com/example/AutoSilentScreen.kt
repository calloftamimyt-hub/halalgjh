package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.PrayerViewModel
import com.example.viewmodel.GlobalLanguage
import com.example.viewmodel.toBengali
import com.example.ui.theme.*
import com.example.receiver.SilentModeHelper

@Composable
fun AutoSilentScreen(
    prayerViewModel: PrayerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by prayerViewModel.state.collectAsState()
    val prefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    
    var isEnabled by remember { mutableStateOf(prefs.getBoolean("auto_silent", true)) }
    
    var isManualEnabled by remember { mutableStateOf(prefs.getBoolean("manual_silent_enabled", false)) }
    var manualStartHour by remember { mutableIntStateOf(prefs.getInt("manual_silent_start_hour", 22)) }
    var manualStartMinute by remember { mutableIntStateOf(prefs.getInt("manual_silent_start_minute", 0)) }
    var manualEndHour by remember { mutableIntStateOf(prefs.getInt("manual_silent_end_hour", 6)) }
    var manualEndMinute by remember { mutableIntStateOf(prefs.getInt("manual_silent_end_minute", 0)) }
    
    // Permission state
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    var hasPermission by remember { 
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.isNotificationPolicyAccessGranted
            } else true
        )
    }

    BackHandler(onBack = onBack)

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notificationManager.isNotificationPolicyAccessGranted
                } else true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
            }
            Text(
                text = "অটো সাইলেন্ট সেটিংস",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextDark
            )
        }

        if (!hasPermission) {
            PermissionRequiredCard {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Main Switch Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeOff, contentDescription = null, tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("অটো সাইলেন্ট মোড", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                            Text("নামাজের সময় ফোন সাইলেন্ট হবে", fontSize = 12.sp, color = TextGray)
                        }
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            isEnabled = it
                            prefs.edit().putBoolean("auto_silent", it).apply()
                            // Trigger rescheduling
                            SilentModeHelper.scheduleSilentAlarms(context)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Silent Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isManualEnabled) PrimaryGreen.copy(alpha = 0.5f) else Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isManualEnabled) PrimaryGreen.copy(alpha = 0.1f) else Color(0xFFF3F4F6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isManualEnabled) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (isManualEnabled) PrimaryGreen else TextGray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("ম্যানুয়াল সাইলেন্ট মোড", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                Text("আপনার নিজের নির্ধারিত সময়ে সাইলেন্ট করুন", fontSize = 12.sp, color = TextGray)
                            }
                        }
                        Switch(
                            checked = isManualEnabled,
                            onCheckedChange = {
                                isManualEnabled = it
                                prefs.edit().putBoolean("manual_silent_enabled", it).apply()
                                SilentModeHelper.scheduleSilentAlarms(context)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
                        )
                    }

                    if (isManualEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Color(0xFFF3F4F6))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Start Time Picker Button
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("সাইলেন্ট শুরুর সময়", fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val timePickerDialog = android.app.TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                manualStartHour = hour
                                                manualStartMinute = minute
                                                prefs.edit()
                                                    .putInt("manual_silent_start_hour", hour)
                                                    .putInt("manual_silent_start_minute", minute)
                                                    .apply()
                                                SilentModeHelper.scheduleSilentAlarms(context)
                                            },
                                            manualStartHour,
                                            manualStartMinute,
                                            false
                                        )
                                        timePickerDialog.show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(44.dp)
                                ) {
                                    val formattedTime = formatTime(manualStartHour, manualStartMinute)
                                    Text(formattedTime, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                }
                            }

                            // End Time Picker Button
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("সাইলেন্ট শেষের সময়", fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        val timePickerDialog = android.app.TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                manualEndHour = hour
                                                manualEndMinute = minute
                                                prefs.edit()
                                                    .putInt("manual_silent_end_hour", hour)
                                                    .putInt("manual_silent_end_minute", minute)
                                                    .apply()
                                                SilentModeHelper.scheduleSilentAlarms(context)
                                            },
                                            manualEndHour,
                                            manualEndMinute,
                                            false
                                        )
                                        timePickerDialog.show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(44.dp)
                                ) {
                                    val formattedTime = formatTime(manualEndHour, manualEndMinute)
                                    Text(formattedTime, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("নামাজের তালিকা ও সময় নির্ধারণ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
            Spacer(modifier = Modifier.height(12.dp))

            state.prayerTimes?.let { times ->
                val prayers = listOf(
                    Triple("Fajr", "ফজর", times.fajr),
                    Triple("Dhuhr", "যোহর", times.dhuhr),
                    Triple("Asr", "আসর", times.asr),
                    Triple("Maghrib", "মাগরিব", times.maghrib),
                    Triple("Isha", "এশা", times.isha)
                )

                prayers.forEach { (id, name, time) ->
                    SilentPrayerCard(
                        id = id,
                        name = name,
                        time = time,
                        context = context,
                        onScheduleNeeded = {
                            SilentModeHelper.scheduleSilentAlarms(context)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun SilentPrayerCard(
    id: String,
    name: String,
    time: String,
    context: Context,
    onScheduleNeeded: () -> Unit
) {
    val prefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    var isEnabled by remember { mutableStateOf(prefs.getBoolean("silent_${id}_enabled", true)) }
    var startOffset by remember { mutableIntStateOf(prefs.getInt("silent_${id}_start_offset", 0)) }
    var endOffset by remember { mutableIntStateOf(prefs.getInt("silent_${id}_end_offset", 20)) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isEnabled) PrimaryGreen.copy(alpha = 0.5f) else Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).background(if (isEnabled) PrimaryGreen.copy(alpha=0.1f) else Color(0xFFF3F4F6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isEnabled) PrimaryGreen else TextGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                        Text("${time.toBengali()} থেকে শুরু", fontSize = 12.sp, color = TextGray)
                    }
                }
                Checkbox(
                    checked = isEnabled,
                    onCheckedChange = {
                        isEnabled = it
                        prefs.edit().putBoolean("silent_${id}_enabled", it).apply()
                        onScheduleNeeded()
                    },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryGreen)
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFFF3F4F6))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OffsetPicker(
                        label = "সাইলেন্ট হবে",
                        value = startOffset,
                        onValueChange = {
                            startOffset = it
                            prefs.edit().putInt("silent_${id}_start_offset", it).apply()
                            onScheduleNeeded()
                        }
                    )
                    OffsetPicker(
                        label = "সাইলেন্ট অফ হবে",
                        value = endOffset,
                        onValueChange = {
                            endOffset = it
                            prefs.edit().putInt("silent_${id}_end_offset", it).apply()
                            onScheduleNeeded()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun OffsetPicker(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(140.dp)) {
        Text(label, fontSize = 12.sp, color = TextGray, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp)).padding(horizontal = 4.dp)
        ) {
            IconButton(onClick = { if (value > -60) onValueChange(value - 5) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(
                text = if (value == 0) "আজানের সময়".toBengali() else if (value > 0) "${value.toString().toBengali()} মিনিট পর" else "${(-value).toString().toBengali()} মিনিট আগে",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(horizontal = 4.dp),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { if (value < 60) onValueChange(value + 5) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun PermissionRequiredCard(onRequest: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        modifier = Modifier.fillMaxWidth().padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("পারমিশন প্রয়োজন", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF92400E))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "অটো সাইলেন্ট কাজ করার জন্য 'Do Not Disturb' এক্সেস পারমিশন দিতে হবে। নিচের বাটনে ক্লিক করে Halal Circle খুঁজে পারমিশনটি অন করুন।",
                fontSize = 12.sp,
                color = Color(0xFF92400E),
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(40.dp)
            ) {
                Text("পারমিশন দিন", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val isPm = hour >= 12
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val minStr = String.format("%02d", minute)
    val period = if (isPm) "PM" else "AM"
    
    // Convert to Bengali digits
    return "${displayHour.toString().toBengali()}:${minStr.toBengali()} $period"
}
