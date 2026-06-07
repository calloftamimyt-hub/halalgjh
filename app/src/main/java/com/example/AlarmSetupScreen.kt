package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BgLight
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import com.example.database.UserAlarm
import com.example.ui.theme.BgLight
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSetupScreen(
    onBack: () -> Unit,
    onAddAlarmClick: () -> Unit,
    alarms: List<UserAlarm>,
    onToggleAlarm: (UserAlarm) -> Unit,
    onDeleteAlarm: (UserAlarm) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alarms", fontWeight = FontWeight.SemiBold, color = TextDark) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
                    }
                },
                actions = {
                    IconButton(onClick = onAddAlarmClick) {
                        Icon(Icons.Default.Add, contentDescription = "Add Alarm", tint = TextDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgLight)
            )
        },
        containerColor = BgLight
    ) { padding ->
        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No Alarms Set", color = TextGray, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap + to create a new alarm", color = TextGray, fontSize = 14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
            ) {
                items(alarms) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { onToggleAlarm(alarm) },
                        onDelete = { onDeleteAlarm(alarm) }
                    )
                }
            }
        }
    }
}

@Composable
fun AlarmCard(
    alarm: UserAlarm,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f), // Square-ish
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (alarm.isEnabled) Color.White else Color.White.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "${alarm.hour}:${alarm.minute.toString().padStart(2, '0')}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (alarm.isEnabled) TextDark else TextGray
                    )
                    Text(
                        text = alarm.amPm,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (alarm.isEnabled) TextDark else TextGray
                    )
                }
                
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    scale = 0.8f,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = BgLight
                    )
                )
            }

            Column {
                if (alarm.label.isNotEmpty()) {
                    Text(
                        text = alarm.label,
                        fontSize = 13.sp,
                        color = if (alarm.isEnabled) TextDark else TextGray,
                        maxLines = 1
                    )
                }
                Text(
                    text = alarm.days,
                    fontSize = 11.sp,
                    color = TextGray,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    scale: Float = 1f,
    colors: SwitchColors = SwitchDefaults.colors()
) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.scale(scale),
        colors = colors
    )
}
