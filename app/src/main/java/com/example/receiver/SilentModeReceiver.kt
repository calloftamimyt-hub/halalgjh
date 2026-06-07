package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import android.app.PendingIntent
import com.example.MainActivity
import com.example.R
import com.example.database.NotificationEntity
import com.example.database.TrackerDatabase
import com.example.viewmodel.GlobalLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SilentModeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val silentOn = action == "com.example.ACTION_SILENT_ON"
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Check if we have permission to modify DND settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
            // We can't change ringer mode without this permission on newer Android
            return
        }

        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        if (silentOn) {
            // Save current ringer mode to restore later if needed, but for now just set to silent
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            } catch (e: Exception) {
                try {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
            
            val msg = if (prayerName == "Manual") {
                if (GlobalLanguage.isEnglish) 
                    "Phone silenced by manual silent mode." 
                else 
                    "ম্যানুয়াল সাইলেন্ট মোড অনুযায়ী ফোন সাইলেন্ট করা হয়েছে।"
            } else {
                if (GlobalLanguage.isEnglish) 
                    "Phone silenced for $prayerName prayer." 
                else 
                    "$prayerName নামাজের জন্য ফোন সাইলেন্ট করা হয়েছে।"
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            
            // Show push notification with beautiful motivating message
            showSilentNotification(context, notificationManager, prayerName, true)
        } else {
            // Turn silent off (restore to normal)
            try {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            val msg = if (prayerName == "Manual") {
                if (GlobalLanguage.isEnglish) 
                    "Manual silent mode turned off." 
                else 
                    "ম্যানুয়াল সাইলেন্ট মোড শেষ হওয়ায় সাইলেন্ট মোড বন্ধ করা হয়েছে।"
            } else {
                if (GlobalLanguage.isEnglish) 
                    "Silent mode turned off after $prayerName." 
                else 
                    "$prayerName নামাজ শেষে সাইলেন্ট মোড বন্ধ করা হয়েছে।"
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

            // Show push notification that phone is un-silenced
            showSilentNotification(context, notificationManager, prayerName, false)
        }
    }

    private fun showSilentNotification(context: Context, notificationManager: NotificationManager, prayerName: String, isSilencing: Boolean) {
        val channelId = "silent_mode_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = if (GlobalLanguage.isEnglish) "Silent Mode Manager" else "সাইলেন্ট মোড ম্যানেজার"
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifications for automatic and scheduled silent mode transitions"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val title = if (isSilencing) {
            if (GlobalLanguage.isEnglish) "Phone Silenced" else "ফোন সাইলেন্ট করা হয়েছে"
        } else {
            if (GlobalLanguage.isEnglish) "Silent Mode Ended" else "সাইলেন্ট মোড শেষ হয়েছে"
        }

        val body = if (isSilencing) {
            if (prayerName == "Manual") {
                if (GlobalLanguage.isEnglish) {
                    "Your phone is silenced for your custom time range. Cherish this serene distraction-free time!"
                } else {
                    "আপনার নির্ধারিত সময়ে ফোন সাইলেন্ট করা হয়েছে। এই শান্ত, নিরিবিলি সময়ে পুরোপুরি মনঃসংযোগ করুন!"
                }
            } else {
                if (GlobalLanguage.isEnglish) {
                    "Your phone is silenced for $prayerName prayer. Deepen your focus and pray with complete peace."
                } else {
                    "$prayerName নামাজের জন্য ফোন সাইলেন্ট করা হয়েছে। নামাজে স্থিরতা এবং মনঃসংযোগ বজায় রাখুন।"
                }
            }
        } else {
            if (prayerName == "Manual") {
                if (GlobalLanguage.isEnglish) {
                    "Silent mode has ended. Stay focused, productive, and filled with peace!"
                } else {
                    "নির্ধারিত সমাপ্তি সময়ে সাইলেন্ট মোড বন্ধ হয়েছে। আপনার দিনটি সফল ও সুন্দর হোক!"
                }
            } else {
                if (GlobalLanguage.isEnglish) {
                    "Silent period ended after $prayerName prayer. May your prayers and good deeds be accepted."
                } else {
                    "$prayerName নামাজের নীরবতার সময় শেষ হয়েছে। আল্লাহ আপনার ইবাদত কবুল করুন, আমিন।"
                }
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(777, notification)
        
        // Save to local database for the Notifications page list
        val db = TrackerDatabase.getDatabase(context)
        val dao = db.notificationDao()
        val entity = NotificationEntity(
            title = title,
            body = body,
            timestamp = System.currentTimeMillis(),
            type = "SILENT_MODE",
            actorName = "System",
            remoteId = "silent_${System.currentTimeMillis()}"
        )
        CoroutineScope(Dispatchers.IO).launch {
            dao.insertNotification(entity)
        }
    }
}
