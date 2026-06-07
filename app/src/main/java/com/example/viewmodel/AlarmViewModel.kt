package com.example.viewmodel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.TrackerDatabase
import com.example.database.UserAlarm
import com.example.receiver.AlarmReceiver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.*

class AlarmViewModel(private val context: Context) : ViewModel() {
    private val db = TrackerDatabase.getDatabase(context)
    private val alarmDao = db.alarmDao()

    val alarms = alarmDao.getAllAlarms().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addAlarm(alarm: UserAlarm) {
        viewModelScope.launch {
            val id = alarmDao.insertAlarm(alarm)
            val insertedAlarm = alarm.copy(id = id.toInt())
            scheduleAlarm(insertedAlarm)
        }
    }

    fun toggleAlarm(alarm: UserAlarm) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            alarmDao.updateAlarm(updated)
            if (updated.isEnabled) {
                scheduleAlarm(updated)
            } else {
                cancelAlarm(updated)
            }
        }
    }

    fun deleteAlarm(alarm: UserAlarm) {
        viewModelScope.launch {
            alarmDao.deleteAlarm(alarm)
            cancelAlarm(alarm)
        }
    }

    private fun scheduleAlarm(alarm: UserAlarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                }
                context.startActivity(intent)
                return
            }
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, if (alarm.amPm == "PM" && alarm.hour != 12) alarm.hour + 12 else if (alarm.amPm == "AM" && alarm.hour == 12) 0 else alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarm.id)
            putExtra("ALARM_LABEL", alarm.label)
            putExtra("IS_USER_ALARM", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id + 1000, // Use offset to avoid conflict with prayer alarms
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun cancelAlarm(alarm: UserAlarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
