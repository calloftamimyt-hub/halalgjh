package com.example

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.model.UserUploadedVideo
import com.example.viewmodel.GlobalLanguage
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import com.example.R

class VideoUploadService : Service() {

    private val CHANNEL_ID = "video_upload_channel"
    private val NOTIFICATION_ID = 1001
    private val SUCCESS_NOTIFICATION_ID = 1002

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoUriStr = intent?.getStringExtra("videoUri")
        val title = intent?.getStringExtra("title") ?: ""
        val description = intent?.getStringExtra("description") ?: ""
        val docId = intent?.getStringExtra("docId") ?: ""

        if (videoUriStr != null) {
            val videoUri = Uri.parse(videoUriStr)
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createProgressNotification(0, 0))
            uploadVideo(videoUri, title, description, docId)
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Video Upload Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createProgressNotification(progress: Int, totalSize: Long): Notification {
        val isEnglish = GlobalLanguage.isEnglish
        val titleText = if (isEnglish) "Uploading Video..." else "ভিডিও আপলোড হচ্ছে..."
        
        val progressText = if (totalSize > 0) {
            val sizeMb = String.format("%.2f", totalSize / (1024f * 1024f))
            if (isEnglish) "$progress% ($sizeMb MB)" else "$progress% ($sizeMb MB)"
        } else {
            "$progress%"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun uploadVideo(videoUri: Uri, title: String, description: String, docId: String) {
        val chatId = "-1002647379129"
        val botToken = "8968904429:AAE3Ce849ysMuaxQhdMebsBwyB_nlIPQ1Os"
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS) // Longer timeout for large videos
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)

        Thread {
            try {
                val inputStream = contentResolver.openInputStream(videoUri)
                val totalSize = inputStream?.available()?.toLong() ?: 0L

                val videoBody = object : RequestBody() {
                    override fun contentType(): MediaType? = "video/mp4".toMediaTypeOrNull()
                    override fun contentLength(): Long = totalSize
                    override fun writeTo(sink: BufferedSink) {
                        inputStream?.use { input ->
                            val buffer = ByteArray(8192)
                            var read: Int
                            var uploaded = 0L
                            var lastUpdate = 0L
                            while (input.read(buffer).also { read = it } != -1) {
                                sink.write(buffer, 0, read)
                                uploaded += read
                                val currentProgress = ((uploaded * 100) / totalSize).toInt()
                                if (currentProgress > lastUpdate) {
                                    notificationManager.notify(NOTIFICATION_ID, createProgressNotification(currentProgress, totalSize))
                                    lastUpdate = currentProgress.toLong()
                                }
                            }
                        }
                    }
                }

                val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", chatId)
                    .addFormDataPart("caption", "🎬 **নতুন ভিডিও আপলোড হয়েছে!**\n\n**ID:** $docId\n**শিরোনাম:** $title\n**বিবরণ:** $description\n\nএডমিন, দয়া করে ভিডিওটি পর্যালোচনা করে অ্যাপ্রুভ বা রিজেক্ট করুন।")
                    .addFormDataPart("reply_markup", """
                        {
                            "inline_keyboard": [
                                [
                                    {"text": "Approve ✅", "callback_data": "approve_$docId"},
                                    {"text": "Reject ❌", "callback_data": "reject_$docId"}
                                ],
                                [
                                    {"text": "Permanently Delete 🗑️", "callback_data": "delete_$docId"}
                                ]
                            ]
                        }
                    """.trimIndent())
                    .addFormDataPart("video", "upload.mp4", videoBody)

                val request = Request.Builder()
                    .url("https://api.telegram.org/bot$botToken/sendVideo")
                    .post(bodyBuilder.build())
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseStr = response.body?.string()
                    handleSuccess(responseStr, docId)
                } else {
                    handleError("Server error: ${response.code}")
                }
            } catch (e: Exception) {
                handleError(e.message ?: "Unknown error")
                e.printStackTrace()
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }.start()
    }

    private fun handleSuccess(responseStr: String?, docId: String) {
        if (responseStr != null) {
            try {
                val jsonObj = org.json.JSONObject(responseStr)
                if (jsonObj.optBoolean("ok")) {
                    val result = jsonObj.optJSONObject("result")
                    val videoObj = result?.optJSONObject("video")
                    val fileId = videoObj?.optString("file_id")
                    if (!fileId.isNullOrEmpty()) {
                        FirebaseFirestore.getInstance().collection("videos").document(docId)
                            .update("telegramFileId", fileId)
                    }
                    showCompletionNotification()
                } else {
                    handleError("Telegram API error")
                }
            } catch (e: Exception) {
                handleError("Parsing error")
            }
        }
    }

    private fun showCompletionNotification() {
        val isEnglish = GlobalLanguage.isEnglish
        val title = if (isEnglish) "Video upload successful" else "ভিডিও আপলোড সফল হয়েছে"
        val body = if (isEnglish) "Await admin approval." else "এডমিন অ্যাপ্রুভালের জন্য অপেক্ষা করুন।"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_notifications", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(SUCCESS_NOTIFICATION_ID, notification)
        
        // Add to local notifications database for the list
        addToLocalNotifications(title, body)
    }

    private fun addToLocalNotifications(title: String, body: String) {
        val db = com.example.database.TrackerDatabase.getDatabase(this)
        val dao = db.notificationDao()
        val entity = com.example.database.NotificationEntity(
            title = title,
            body = body,
            timestamp = System.currentTimeMillis(),
            type = "UPLOAD_SUCCESS",
            actorName = "System",
            remoteId = "upload_${System.currentTimeMillis()}"
        )
        CoroutineScope(Dispatchers.IO).launch {
            dao.insertNotification(entity)
        }
    }

    private fun handleError(error: String) {
        val isEnglish = GlobalLanguage.isEnglish
        val title = if (isEnglish) "Upload failed" else "আপলোড ব্যর্থ হয়েছে"
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(error)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(SUCCESS_NOTIFICATION_ID, notification)
    }
}
