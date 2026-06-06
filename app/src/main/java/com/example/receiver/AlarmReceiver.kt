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

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Need to retrieve last known location to reschedule? 
            // In a real app we'd save location to DataStore and restore.
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
        
        // Reschedule for the next prayer
        AlarmHelper.reschedule(context)
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

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("ওয়াক্ত পরিবর্তন হয়েছে: $prayerName")
            .setContentText("এখন $prayerName এর সময়। দয়া করে নামাজের জন্য প্রস্তুতি নিন।")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(alarmSound)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(prayerName.hashCode(), notification)
    }
}
