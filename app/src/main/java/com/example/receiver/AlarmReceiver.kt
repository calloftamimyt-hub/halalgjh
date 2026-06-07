package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.database.NotificationEntity
import com.example.database.TrackerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmHelper.reschedule(context)
            return
        }

        val isUserAlarm = intent.getBooleanExtra("IS_USER_ALARM", false)
        if (isUserAlarm) {
            val alarmId = intent.getIntExtra("ALARM_ID", -1)
            val label = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
            
            // Launch Full Screen Alarm Activity
            val alarmIntent = Intent(context, com.example.AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("ALARM_ID", alarmId)
                putExtra("ALARM_LABEL", label)
            }
            context.startActivity(alarmIntent)
            return
        }

        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
        
        val prayerNameBen = when(prayerName) {
            "Fajr" -> "ফজর"
            "Sunrise" -> "সূর্যোদয়"
            "Dhuhr" -> "যোহর"
            "Asr" -> "আসর"
            "Maghrib" -> "মাগরিব"
            "Isha" -> "এশা"
            else -> prayerName
        }

        showNotification(context, prayerNameBen)
        saveNotificationToDb(context, prayerNameBen)
        
        // Reschedule for the next prayer
        AlarmHelper.reschedule(context)
    }

    private fun saveNotificationToDb(context: Context, prayerName: String) {
        val title = when(prayerName) {
            "সূর্যোদয়" -> "সূর্যোদয় হয়েছে"
            "মাগরিব" -> "সূর্যাস্ত হয়েছে (মাগরিব)"
            else -> "ওয়াক্ত পরিবর্তন হয়েছে: $prayerName"
        }
        val body = when(prayerName) {
            "সূর্যোদয়" -> "এখন সূর্যোদয় হয়েছে। ফজরের ওয়াক্ত শেষ।"
            "মাগরিব" -> "সূর্যাস্ত হয়েছে। এখন মাগরিবের ওয়াক্ত।"
            else -> "এখন $prayerName এর সময়। দয়া করে নামাজের জন্য প্রস্তুতি নিন।"
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TrackerDatabase.getDatabase(context)
                val notificationDao = db.notificationDao()
                val notification = NotificationEntity(
                    title = title,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    type = "GENERAL",
                    actorName = "সালাত রিমাইন্ডার",
                    itemType = "prayer"
                )
                notificationDao.insertNotification(notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showNotification(context: Context, prayerName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "prayer_times_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Prayer Times Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for prayer times"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val title = when(prayerName) {
            "সূর্যোদয়" -> "সূর্যোদয় হয়েছে"
            "মাগরিব" -> "সূর্যাস্ত হয়েছে (মাগরিব)"
            else -> "ওয়াক্ত পরিবর্তন হয়েছে: $prayerName"
        }
        val body = when(prayerName) {
            "সূর্যোদয়" -> "এখন সূর্যোদয় হয়েছে। ফজরের ওয়াক্ত শেষ।"
            "মাগরিব" -> "সূর্যাস্ত হয়েছে। এখন মাগরিবের ওয়াক্ত।"
            else -> "এখন $prayerName এর সময়। দয়া করে নামাজের জন্য প্রস্তুতি নিন।"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmSound)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(prayerName.hashCode(), notification)
    }
}
