package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.calculator.PrayerCalculator
import com.example.calculator.PrayerTimes
import java.util.Calendar

object AlarmHelper {

    fun scheduleNextPrayer(context: Context, lat: Double, lng: Double, timezoneOffsetHor: Double, alarms: Map<String, Boolean>? = null) {
        saveState(context, lat, lng, timezoneOffsetHor, alarms)
        val madhab = context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE).getInt("madhab", 2)
        val calendar = Calendar.getInstance()
        val times = PrayerCalculator.calculatePrayerTimes(lat, lng, timezoneOffsetHor, madhab, calendar)

        // Find the next prayer
        val currentHourDecimal = calendar.get(Calendar.HOUR_OF_DAY) + calendar.get(Calendar.MINUTE) / 60.0
        
        val allPrayers = listOf(
            Pair("Fajr", times.fajrHours),
            Pair("Sunrise", times.sunriseHours),
            Pair("Dhuhr", times.dhuhrHours),
            Pair("Asr", times.asrHours),
            Pair("Maghrib", times.maghribHours),
            Pair("Isha", times.ishaHours)
        )

        // Filter based on user preference but ALWAYS include Sunrise and Maghrib for notifications
        val activePrayers = if (alarms != null) {
            allPrayers.filter { 
                alarms[it.first] == true || it.first == "Sunrise" || it.first == "Maghrib"
            }
        } else allPrayers

        if (activePrayers.isEmpty()) {
            cancelAlarm(context)
            return
        }

        var nextPrayerTime = -1.0
        var nextPrayerName = ""
        var isTomorrow = false

        for (prayer in activePrayers) {
            if (prayer.second > currentHourDecimal) {
                nextPrayerTime = prayer.second
                nextPrayerName = prayer.first
                break
            }
        }

        // If no prayer is found today, the next prayer is the first active prayer tomorrow
        if (nextPrayerTime == -1.0) {
            val madhab = context.getSharedPreferences("prayer_prefs", Context.MODE_PRIVATE).getInt("madhab", 2)
            val tomorrow = Calendar.getInstance()
            tomorrow.add(Calendar.DAY_OF_YEAR, 1)
            val tomorrowTimes = PrayerCalculator.calculatePrayerTimes(lat, lng, timezoneOffsetHor, madhab, tomorrow)
            val tomorrowAllPrayers = listOf(
                Pair("Fajr", tomorrowTimes.fajrHours),
                Pair("Sunrise", tomorrowTimes.sunriseHours),
                Pair("Dhuhr", tomorrowTimes.dhuhrHours),
                Pair("Asr", tomorrowTimes.asrHours),
                Pair("Maghrib", tomorrowTimes.maghribHours),
                Pair("Isha", tomorrowTimes.ishaHours)
            )
            val tomorrowActivePrayers = if (alarms != null) {
                tomorrowAllPrayers.filter { 
                    alarms[it.first] == true || it.first == "Sunrise" || it.first == "Maghrib"
                }
            } else tomorrowAllPrayers
            nextPrayerTime = tomorrowActivePrayers.first().second
            nextPrayerName = tomorrowActivePrayers.first().first
            isTomorrow = true
        }

        scheduleAlarm(context, nextPrayerName, nextPrayerTime, isTomorrow)
        
        // Also schedule silent mode alarms
        SilentModeHelper.scheduleSilentAlarms(context, lat, lng, timezoneOffsetHor, madhab)
    }

    private fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_PRAYER_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleAlarm(context: Context, name: String, hourDecimal: Double, isTomorrow: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.ACTION_PRAYER_ALARM"
            putExtra("PRAYER_NAME", name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        if (isTomorrow) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        val hour = Math.floor(hourDecimal).toInt()
        val minute = Math.floor((hourDecimal - hour) * 60).toInt()
        
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        
        // Exact alarm
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun saveState(context: Context, lat: Double, lng: Double, offset: Double, alarms: Map<String, Boolean>?) {
        val prefs = context.getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("lat", lat.toFloat())
            putFloat("lng", lng.toFloat())
            putFloat("offset", offset.toFloat())
            alarms?.forEach { (name, enabled) ->
                putBoolean("alarm_$name", enabled)
            }
            apply()
        }
    }

    fun reschedule(context: Context) {
        val prefs = context.getSharedPreferences("prayer_alarm_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("lat", 23.8103f).toDouble()
        val lng = prefs.getFloat("lng", 90.4125f).toDouble()
        val offset = prefs.getFloat("offset", 6.0f).toDouble()
        
        val alarms = mutableMapOf<String, Boolean>()
        listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha").forEach { name ->
            alarms[name] = prefs.getBoolean("alarm_$name", name != "Sunrise")
        }
        
        scheduleNextPrayer(context, lat, lng, offset, alarms)
    }
}
