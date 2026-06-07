package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.calculator.PrayerCalculator
import java.util.*

object SilentModeHelper {

    fun scheduleSilentAlarms(context: Context, lat: Double? = null, lng: Double? = null, timezoneOffset: Double? = null, madhab: Int? = null) {
        val prefs = context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("auto_silent", true)
        
        val alarmPrefs = context.getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)
        val finalLat = lat ?: alarmPrefs.getFloat("lat", 23.8103f).toDouble()
        val finalLng = lng ?: alarmPrefs.getFloat("lng", 90.4125f).toDouble()
        val finalOffset = timezoneOffset ?: alarmPrefs.getFloat("offset", 6.0f).toDouble()
        val finalMadhab = madhab ?: context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE).getInt("madhab", 2)

        if (!isEnabled) {
            cancelAllAlarms(context)
            return
        }

        val calendar = Calendar.getInstance()
        val times = PrayerCalculator.calculatePrayerTimes(finalLat, finalLng, finalOffset, finalMadhab, calendar)
        
        val prayers = listOf(
            "Fajr" to times.fajrHours,
            "Dhuhr" to times.dhuhrHours,
            "Asr" to times.asrHours,
            "Maghrib" to times.maghribHours,
            "Isha" to times.ishaHours
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        prayers.forEach { (name, hour) ->
            val prayerEnabled = prefs.getBoolean("silent_${name}_enabled", true)
            if (prayerEnabled) {
                val startOffset = prefs.getInt("silent_${name}_start_offset", 0) // minutes relative to Azan
                val endOffset = prefs.getInt("silent_${name}_end_offset", 20) // minutes after Azan

                scheduleAlarm(context, alarmManager, name, hour, startOffset, true)
                scheduleAlarm(context, alarmManager, name, hour, endOffset, false)
            } else {
                cancelAlarm(context, alarmManager, name, true)
                cancelAlarm(context, alarmManager, name, false)
            }
        }

        val manualEnabled = prefs.getBoolean("manual_silent_enabled", false)
        if (manualEnabled) {
            val startHour = prefs.getInt("manual_silent_start_hour", 22)
            val startMinute = prefs.getInt("manual_silent_start_minute", 0)
            val endHour = prefs.getInt("manual_silent_end_hour", 6)
            val endMinute = prefs.getInt("manual_silent_end_minute", 0)

            val startPrayerHour = startHour.toDouble() + (startMinute.toDouble() / 60.0)
            val endPrayerHour = endHour.toDouble() + (endMinute.toDouble() / 60.0)

            scheduleAlarm(context, alarmManager, "Manual", startPrayerHour, 0, true)
            scheduleAlarm(context, alarmManager, "Manual", endPrayerHour, 0, false)
        } else {
            cancelAlarm(context, alarmManager, "Manual", true)
            cancelAlarm(context, alarmManager, "Manual", false)
        }
    }

    private fun scheduleAlarm(context: Context, alarmManager: AlarmManager, prayerName: String, prayerHour: Double, offsetMinutes: Int, isOn: Boolean) {
        val calendar = Calendar.getInstance()
        val totalMinutes = (prayerHour * 60).toInt() + offsetMinutes
        
        calendar.set(Calendar.HOUR_OF_DAY, (totalMinutes / 60) % 24)
        calendar.set(Calendar.MINUTE, totalMinutes % 60)
        calendar.set(Calendar.SECOND, 0)

        // If time is in the past, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, SilentModeReceiver::class.java).apply {
            action = if (isOn) "com.example.ACTION_SILENT_ON" else "com.example.ACTION_SILENT_OFF"
            putExtra("PRAYER_NAME", prayerName)
        }

        val requestCode = if (isOn) prayerName.hashCode() else prayerName.hashCode() + 1000
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, prayerName: String, isOn: Boolean) {
        val intent = Intent(context, SilentModeReceiver::class.java).apply {
            action = if (isOn) "com.example.ACTION_SILENT_ON" else "com.example.ACTION_SILENT_OFF"
        }
        val requestCode = if (isOn) prayerName.hashCode() else prayerName.hashCode() + 1000
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha", "Manual").forEach { name ->
            cancelAlarm(context, alarmManager, name, true)
            cancelAlarm(context, alarmManager, name, false)
        }
    }
}
