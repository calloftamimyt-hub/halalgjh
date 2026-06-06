package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.widget.Toast
import com.example.viewmodel.GlobalLanguage

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
            audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            
            val msg = if (GlobalLanguage.isEnglish) 
                "Phone silenced for $prayerName prayer." 
            else 
                "$prayerName নামাজের জন্য ফোন সাইলেন্ট করা হয়েছে।"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        } else {
            // Turn silent off (restore to normal)
            audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            
            val msg = if (GlobalLanguage.isEnglish) 
                "Silent mode turned off after $prayerName." 
            else 
                "$prayerName নামাজ শেষে সাইলেন্ট মোড বন্ধ করা হয়েছে।"
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
}
