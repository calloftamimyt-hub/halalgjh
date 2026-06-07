package com.example

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.UserAlarm
import com.example.ui.theme.BgLight
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmScreen(
    onBack: () -> Unit,
    onSave: (UserAlarm) -> Unit
) {
    var selectedHour by remember { mutableIntStateOf(12) }
    var selectedMinute by remember { mutableIntStateOf(30) }
    var selectedAmPm by remember { mutableStateOf("AM") }
    
    var selectedDays by remember { mutableStateOf(setOf<String>()) }
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    
    var deleteAfterRinging by remember { mutableStateOf(false) }
    var alarmSound by remember { mutableStateOf("Default") }
    var selectedRingtoneUri by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    val context = LocalContext.current
    val ringtonePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<android.net.Uri>(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                selectedRingtoneUri = uri.toString()
                val ringtone = android.media.RingtoneManager.getRingtone(context, uri)
                alarmSound = ringtone.getTitle(context)
            }
        }
    }
    var snooze by remember { mutableStateOf("10 min, 3 times") }
    var vibrate by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Alarm", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val daysString = if (selectedDays.isEmpty()) "Once" else selectedDays.joinToString(",")
                        onSave(
                            UserAlarm(
                                hour = selectedHour,
                                minute = selectedMinute,
                                amPm = selectedAmPm,
                                days = daysString,
                                deleteAfterRinging = deleteAfterRinging,
                                sound = alarmSound,
                                ringtoneUri = selectedRingtoneUri,
                                label = label.ifEmpty { "Alarm" },
                                snooze = snooze,
                                vibrate = vibrate
                            )
                        )
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = PrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BgLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Time Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    TimePickerColumn(
                        options = (1..12).toList(),
                        selected = selectedHour,
                        onSelected = { selectedHour = it }
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                    TimePickerColumn(
                        options = (0..59).toList(),
                        selected = selectedMinute,
                        onSelected = { selectedMinute = it },
                        format = { it.toString().padStart(2, '0') }
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "AM",
                            color = if (selectedAmPm == "AM") PrimaryGreen else TextGray,
                            fontWeight = if (selectedAmPm == "AM") FontWeight.Bold else FontWeight.Normal,
                            fontSize = 24.sp,
                            modifier = Modifier.clickable { selectedAmPm = "AM" }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "PM",
                            color = if (selectedAmPm == "PM") PrimaryGreen else TextGray,
                            fontWeight = if (selectedAmPm == "PM") FontWeight.Bold else FontWeight.Normal,
                            fontSize = 24.sp,
                            modifier = Modifier.clickable { selectedAmPm = "PM" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day selection circles
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysOfWeek.forEach { day ->
                    val isSelected = selectedDays.contains(day)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) PrimaryGreen else Color.White)
                            .border(1.dp, if (isSelected) PrimaryGreen else BgLight, CircleShape)
                            .clickable {
                                selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.take(1),
                            color = if (isSelected) Color.White else TextDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Settings Card
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    AlarmSettingToggle(
                        label = "Ring only once",
                        checked = deleteAfterRinging,
                        onCheckedChange = { deleteAfterRinging = it }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = BgLight, thickness = 1.dp)
                    AlarmSettingItem(
                        label = "Alarm Sound",
                        value = alarmSound,
                        onClick = { 
                            val intent = android.content.Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE, android.media.RingtoneManager.TYPE_ALARM)
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, if (selectedRingtoneUri.isNotEmpty()) android.net.Uri.parse(selectedRingtoneUri) else null as android.net.Uri?)
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            }
                            ringtonePickerLauncher.launch(intent)
                        }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = BgLight, thickness = 1.dp)
                    AlarmSettingItem(
                        label = "Label",
                        value = label.ifEmpty { "Default" },
                        onClick = { /* Label dialog */ }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = BgLight, thickness = 1.dp)
                    AlarmSettingItem(
                        label = "Snooze",
                        value = snooze,
                        onClick = { /* Snooze dialog */ }
                    )
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = BgLight, thickness = 1.dp)
                    AlarmSettingToggle(
                        label = "Vibrate while Ringing",
                        checked = vibrate,
                        onCheckedChange = { vibrate = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TimePickerColumn(
    options: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    format: (Int) -> String = { it.toString() }
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val index = options.indexOf(selected)
        val prev = if (index > 0) options[index - 1] else options.last()
        val next = if (index < options.size - 1) options[index + 1] else options.first()
        val prevPrev = if (options.indexOf(prev) > 0) options[options.indexOf(prev) - 1] else options.last()
        val nextNext = if (options.indexOf(next) < options.size - 1) options[options.indexOf(next) + 1] else options.first()

        Text(format(prevPrev), color = TextGray.copy(alpha = 0.3f), fontSize = 18.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(format(prev), color = TextGray, fontSize = 22.sp, modifier = Modifier.clickable { onSelected(prev) })
        Spacer(modifier = Modifier.height(12.dp))
        Text(format(selected), color = TextDark, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(format(next), color = TextGray, fontSize = 22.sp, modifier = Modifier.clickable { onSelected(next) })
        Spacer(modifier = Modifier.height(12.dp))
        Text(format(nextNext), color = TextGray.copy(alpha = 0.3f), fontSize = 18.sp)
    }
}

@Composable
fun AlarmSettingItem(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 16.sp, color = TextDark)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontSize = 14.sp, color = TextGray)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun AlarmSettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 16.sp, color = TextDark, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = BgLight
            )
        )
    }
}
