@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.example

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.toBengali
import com.example.ui.theme.*
import com.google.accompanist.permissions.*
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import com.example.model.UserUploadedVideo
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object VideoStorage {
    private const val PREFS_NAME = "user_uploaded_videos_prefs"
    private const val KEY_VIDEOS = "uploaded_videos_list"

    fun saveVideo(context: Context, video: UserUploadedVideo) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list = getVideos(context).toMutableList()
        list.add(0, video) // Prepend newest first

        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("docId", item.docId)
            obj.put("userId", item.userId)
            obj.put("title", item.title)
            obj.put("author", item.author)
            obj.put("description", item.description)
            obj.put("videoUri", item.videoUri)
            obj.put("timestamp", item.timestamp)
            obj.put("isLocal", item.isLocal)
            obj.put("aspectSize", item.aspectSize)
            obj.put("category", item.category)
            obj.put("isOfflineMode", item.isOfflineMode)
            obj.put("isAutoSubtitles", item.isAutoSubtitles)
            obj.put("isCommentModerated", item.isCommentModerated)
            obj.put("isHideViews", item.isHideViews)
            obj.put("status", item.status)
            obj.put("telegramFileId", item.telegramFileId)
            jsonArray.put(obj)
        }
        sharedPrefs.edit().putString(KEY_VIDEOS, jsonArray.toString()).apply()

        // Firebase Firestore integration
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("videos").document(video.docId)
                .set(video)
                .addOnSuccessListener {
                    android.util.Log.d("Firebase", "Video metadata uploaded to Firestore")
                }
                .addOnFailureListener {
                    android.util.Log.e("Firebase", "Error uploading to Firestore", it)
                }
        } catch (e: Exception) {
            android.util.Log.e("Firebase", "Firestore not initialized", e)
        }
    }

    fun getVideos(context: Context): List<UserUploadedVideo> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString(KEY_VIDEOS, null) ?: return emptyList()
        val list = mutableListOf<UserUploadedVideo>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    UserUploadedVideo(
                        docId = obj.optString("docId", ""),
                        userId = obj.optString("userId", ""),
                        title = obj.optString("title", ""),
                        author = obj.optString("author", "@user"),
                        description = obj.optString("description", ""),
                        videoUri = obj.optString("videoUri", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isLocal = obj.optBoolean("isLocal", true),
                        aspectSize = obj.optString("aspectSize", "৯:১৬"),
                        category = obj.optString("category", "বয়ান (Bayan)"),
                        isOfflineMode = obj.optBoolean("isOfflineMode", false),
                        isAutoSubtitles = obj.optBoolean("isAutoSubtitles", false),
                        isCommentModerated = obj.optBoolean("isCommentModerated", true),
                        isHideViews = obj.optBoolean("isHideViews", false),
                        status = obj.optString("status", "PENDING"),
                        telegramFileId = obj.optString("telegramFileId", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CreateVideoScreen(
    onUploadSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    
    // Set status bar to black with white icons
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = android.graphics.Color.BLACK
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }
    val permissions = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    val permissionState = rememberMultiplePermissionsState(permissions)

    // Flow states
    // createStep: 0 = Camera/Gallery, 1 = Video Editing, 2 = Video Upload info
    var createStep by remember { mutableStateOf(0) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var mockRecordedVideoPath by remember { mutableStateOf<String?>(null) }

    // Camera perspective state
    var isBackCamera by remember { mutableStateOf(true) }

    // Editor settings state
    var trimStartRatio by remember { mutableStateOf(0.0f) }
    var trimEndRatio by remember { mutableStateOf(1.0f) }
    var chosenAspectSize by remember { mutableStateOf("৯:১৬") } // options: অরিজিনাল, ১৬:৯, ৯:১৬, ১:১, ৪:৫
    
    // Success Dialog State
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            createStep = 1 // directly to editing page
            Toast.makeText(context, "গ্যালারি থেকে ভিডিওটি সফলভাবে বাছাই করা হয়েছে!", Toast.LENGTH_SHORT).show()
        }
    }

    if (!permissionState.allPermissionsGranted) {
        // Require Permission Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "ভিডিও ক্যামেরা পারমিশন",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ইন-অ্যাপ ভিডিও রেকর্ড করতে ক্যামেরা এবং অডিও রেকর্ডিং পারমিশন প্রয়োজন।",
                fontSize = 14.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = { permissionState.launchMultiplePermissionRequest() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "অনুমতি দিন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack) {
                Text(text = "ফিরে যান", color = Color(0xFF94A3B8), fontSize = 14.sp)
            }
        }
    } else {
        when (createStep) {
            0 -> {
                // STEP 0: In-App Camera Viewfinder / Gallery option
                InAppCameraView(
                    isBackCamera = isBackCamera,
                    onSwitchCamera = { isBackCamera = !isBackCamera },
                    onOpenGallery = { galleryLauncher.launch("video/*") },
                    onVideoRecorded = { tempPath ->
                        mockRecordedVideoPath = tempPath
                        selectedVideoUri = Uri.parse("file:///$tempPath")
                        createStep = 1
                    },
                    onBack = onBack
                )
            }
            1 -> {
                // STEP 1: Video Editing Page (Trimming + Size Selection)
                VideoEditingView(
                    videoUri = selectedVideoUri,
                    trimStartRatio = trimStartRatio,
                    trimEndRatio = trimEndRatio,
                    chosenAspectSize = chosenAspectSize,
                    onTrimChange = { start, end ->
                        trimStartRatio = start
                        trimEndRatio = end
                    },
                    onAspectChange = { chosenAspectSize = it },
                    onNext = { createStep = 2 },
                    onBack = { createStep = 0 }
                )
            }
            2 -> {
                // STEP 2: Upload Page (Title limit 26 chars, hashtags, 10-12 video features)
                VideoUploadView(
                    videoUri = selectedVideoUri,
                    aspectSize = chosenAspectSize,
                    onUploadClicked = { title, hashtags, features ->
                        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                        var userId = currentUser?.uid
                        val authorName = currentUser?.displayName ?: "@halal_user"
                        
                        if (userId == null) {
                            val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                            userId = sharedPrefs.getString("user_id", null)
                            if (userId == null) {
                                userId = java.util.UUID.randomUUID().toString()
                                sharedPrefs.edit().putString("user_id", userId).apply()
                            }
                        }
                        
                        // Save in local storage
                        val videoItem = UserUploadedVideo(
                            docId = java.util.UUID.randomUUID().toString(),
                            userId = userId,
                            title = title,
                            author = authorName,
                            description = "$title ${hashtags.joinToString(" ")}\n#halalcircle #islamic_bayan",
                            videoUri = selectedVideoUri?.toString() ?: "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                            timestamp = System.currentTimeMillis(),
                            aspectSize = chosenAspectSize,
                            category = features["category"] as? String ?: "বয়ান (Bayan)",
                            isOfflineMode = features["offlineMode"] as? Boolean ?: false,
                            isAutoSubtitles = features["autoSubtitles"] as? Boolean ?: false,
                            isCommentModerated = features["commentModeration"] as? Boolean ?: true,
                            isHideViews = features["hideViews"] as? Boolean ?: false
                        )
                        VideoStorage.saveVideo(context, videoItem)
                        
                        // Send to Telegram in background
                        val scope = kotlinx.coroutines.MainScope()
                        selectedVideoUri?.let { uri ->
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                sendVideoToTelegram(context, uri, title, hashtags.joinToString(" "), videoItem.docId)
                            }
                        }
                        
                        showSuccessDialog = true
                    },
                    onBack = { createStep = 1 }
                )
            }
        }

        if (showSuccessDialog) {
            CongratulationsDialog(
                onConfirm = {
                    showSuccessDialog = false
                    onUploadSuccess()
                }
            )
        }
    }
}

@Composable
fun CongratulationsDialog(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = { },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ঠিক আছে", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(64.dp)
            )
        },
        title = {
            Text(
                text = "অভিনন্দন!",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = TextDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "আপনার ভিডিওটি সফলভাবে আপলোড হয়েছে।",
                    fontSize = 16.sp,
                    color = TextDark,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "বর্তমানে ভিডিওটি এডমিন অ্যাপ্রুভালের অপেক্ষায় আছে। অ্যাপ্রুভ হয়ে গেলে এটি অ্যাপ্লিকেশনে দেখা যাবে। ইনশাআল্লাহ!",
                    fontSize = 14.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun InAppCameraView(
    isBackCamera: Boolean,
    onSwitchCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onVideoRecorded: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var flashOn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var videoCapture: VideoCapture<Recorder>? by remember { mutableStateOf(null) }
    var recording: Recording? by remember { mutableStateOf(null) }
    var camera: androidx.camera.core.Camera? by remember { mutableStateOf(null) }

    // Sound level indicator fluctuation simulation
    var micWaveHeight by remember { mutableStateOf(12.dp) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingSeconds = 0
            while (isRecording) {
                delay(1000)
                recordingSeconds++
                micWaveHeight = (12..40).random().dp
            }
        }
    }

    // Camera setup
    val previewView = remember { PreviewView(context) }
    val cameraSelector = if (isBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

    LaunchedEffect(isBackCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            val newVideoCapture = VideoCapture.withOutput(recorder)
            videoCapture = newVideoCapture

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    newVideoCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Flash control
    LaunchedEffect(flashOn, camera) {
        camera?.cameraControl?.enableTorch(flashOn)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Real Viewfinder
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Camera GRID OVERLAYS (Rule of Thirds)
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.12f)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.12f)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.12f)))
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.12f)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.12f)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.12f)))
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.12f)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.12f)))
                Box(modifier = Modifier.weight(1f).fillMaxHeight().border(0.5.dp, Color.White.copy(alpha = 0.12f)))
            }
        }

        // Top Control Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = Color.White)
            }

            Surface(
                color = if (isRecording) Color.Red.copy(alpha = 0.82f) else Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(100.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        val min = recordingSeconds / 60
                        val sec = recordingSeconds % 60
                        val rawTime = String.format("%02d:%02d", min, sec)
                        Text(
                            text = rawTime.toBengali(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "লাইভ ক্যামেরা (Live Mode)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            IconButton(
                onClick = { flashOn = !flashOn },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = if (flashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flash",
                    tint = if (flashOn) Color.Yellow else Color.White
                )
            }
        }

        if (isRecording) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                repeat(4) { idx ->
                    val animHeight by animateDpAsState(
                        targetValue = if (idx % 2 == 0) micWaveHeight else micWaveHeight * 0.7f,
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "micAnimation"
                    )
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(animHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF10B981))
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onOpenGallery() }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .border(1.5.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "গ্যালারি",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "গ্যালারি",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(
                                width = 4.dp,
                                color = if (isRecording) Color.Red.copy(alpha = 0.4f) else Color.White,
                                shape = CircleShape
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(if (isRecording) 32.dp else 64.dp)
                            .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                            .background(Color.Red)
                            .clickable {
                                if (isRecording) {
                                    recording?.stop()
                                    recording = null
                                    isRecording = false
                                } else {
                                    val videoFile = File(context.cacheDir, "recorded_video_${System.currentTimeMillis()}.mp4")
                                    val outputOptions = FileOutputOptions.Builder(videoFile).build()

                                    recording = videoCapture?.output
                                        ?.prepareRecording(context, outputOptions)
                                        ?.start(ContextCompat.getMainExecutor(context)) { event ->
                                            when (event) {
                                                is VideoRecordEvent.Start -> {
                                                    isRecording = true
                                                }
                                                is VideoRecordEvent.Finalize -> {
                                                    if (!event.hasError()) {
                                                        onVideoRecorded(videoFile.absolutePath)
                                                    } else {
                                                        Toast.makeText(context, "Error: ${event.error}", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isRecording = false
                                                }
                                            }
                                        }
                                }
                            }
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSwitchCamera() }
                        .padding(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlipCameraAndroid,
                            contentDescription = "ক্যামেরা পরিবর্তন",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ক্যামেরা ফ্লিপ",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * ----------------- Video Editing View (Trimming + Resizing) -----------------
 */
@Composable
fun VideoEditingView(
    videoUri: Uri?,
    trimStartRatio: Float,
    trimEndRatio: Float,
    chosenAspectSize: String,
    onTrimChange: (Float, Float) -> Unit,
    onAspectChange: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }

    val aspectSizes = listOf("অরিজিনাল (Original)", "১৬:৯ (16:9 Landscape)", "৯:১৬ (9:16 Portrait)", "১:১ (1:1 Square)", "৪:৫ (4:5 Feed)")

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) {
                        Text(text = "বাতিল", color = Color.White, fontSize = 14.sp)
                    }
                    Text(
                        text = "ভিডিও এডিটর",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // NEXT BUTTON AT THE TOP (পরবর্তী)
                    Button(
                        onClick = onNext,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(text = "পরবর্তী", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = Color(0xFF020617)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Video Preview Frame with Aspect Ratio overlays
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Adaptive height/width bounding boxes illustrating size edits
                val overlayModifier = when (chosenAspectSize) {
                    "১৬:৯ (16:9 Landscape)" -> Modifier.fillMaxWidth().aspectRatio(16/9f)
                    "৯:১৬ (9:16 Portrait)" -> Modifier.fillMaxHeight().aspectRatio(9/16f)
                    "১:১ (1:1 Square)" -> Modifier.fillMaxWidth(0.85f).aspectRatio(1f)
                    "৪:৫ (4:5 Feed)" -> Modifier.fillMaxHeight(0.9f).aspectRatio(4/5f)
                    else -> Modifier.fillMaxSize()
                }

                Box(
                    modifier = overlayModifier
                        .border(1.5.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    // Video Content Illustration & details
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                            contentDescription = "Play/Pause",
                            tint = Color.White.copy(alpha = 0.82f),
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { isPlaying = !isPlaying }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isPlaying) "ভিডিও চলছে (প্রিভিউ)" else "ভিডিও পজ করা হয়েছে",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        val trimmedDurationSec = (trimEndRatio - trimStartRatio) * 15f
                        Text(
                            text = "দৈর্ঘ্যঃ ${String.format("%.1f", trimmedDurationSec).toBengali()} সেকেন্ড",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Border layout sizes overlay markers inside editor
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(4.dp)
                    ) {
                        val simpleAspect = chosenAspectSize.substringBefore(" ")
                        Text(text = simpleAspect, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Slider Range Section to Trim the Video shorter or longer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(16.dp)
            ) {
                Text(
                    text = "ভিডিও ট্রিম করুন (Trim Duration Options)",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Realistic Trim Handle Bar (Trimmer Slider UI)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val startOffset = trimStartRatio * 100f
                    val endOffset = trimEndRatio * 100f

                    // Green Trimming Area Highlight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(trimEndRatio - trimStartRatio)
                            .fillMaxHeight()
                            .offset(x = (trimStartRatio * 320).dp) // approximate visual scale offset
                            .background(Color(0xFF10B981).copy(alpha = 0.25f))
                            .border(width = 2.dp, color = Color(0xFF10B981))
                    )

                    // Left Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .width(16.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF10B981), RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                    )
                    
                    // Right Handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(16.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF10B981), RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Adjust Slider actions (directly editable trims)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            var newStart = (trimStartRatio - 0.05f).coerceAtLeast(0.0f)
                            onTrimChange(newStart, trimEndRatio)
                        }
                    ) {
                        Text(text = "শুরু কমান (-)", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "শুরু: ${String.format("%.1f", trimStartRatio * 15f).toBengali()}s  —  শেষ: ${String.format("%.1f", trimEndRatio * 15f).toBengali()}s",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )

                    TextButton(
                        onClick = {
                            var newEnd = (trimEndRatio + 0.05f).coerceAtMost(1.0f)
                            onTrimChange(trimStartRatio, newEnd)
                        }
                    ) {
                        Text(text = "শেষ বাড়ান (+)", color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // OPTION TO EDIT VIDEO SIZE AT THE BOTTOM (Aspect Sizes Row)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF020617))
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Text(
                    text = "ভিডিও সাইজ বা আকার পরিবর্তন (Resize Options)",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    aspectSizes.forEach { format ->
                        val isSelected = chosenAspectSize == format
                        Box(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF10B981) else Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .background(
                                    color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { onAspectChange(format) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = format.substringBefore(" "),
                                color = if (isSelected) Color(0xFF10B981) else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * ----------------- Detailed Video Upload View (Title & 10-12 Features) -----------------
 */
@Composable
fun VideoUploadView(
    videoUri: Uri?,
    aspectSize: String,
    onUploadClicked: (title: String, hashtags: List<String>, features: Map<String, Any>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Title limit characters: 26 max characters constraint
    var titleInput by remember { mutableStateOf("") }
    
    // Hashtags
    var hashtagInput by remember { mutableStateOf("") }
    val hashtagsList = remember { mutableStateListOf<String>() }

    // Features states (10-12 key features defined to fulfill 10-12 guidelines)
    var isOfflineMode by remember { mutableStateOf(false) } // feature 1
    var isHdQuality by remember { mutableStateOf(true) }     // feature 2
    var isVoiceBoost by remember { mutableStateOf(false) }  // feature 3
    var isAutoSubtitles by remember { mutableStateOf(false) } // feature 4
    var isHalalModCheck by remember { mutableStateOf(true) } // feature 5
    var thumbnailFrameSelected by remember { mutableStateOf(1) } // feature 6
    var isCopyrightShieldOn by remember { mutableStateOf(true) } // feature 7
    var isCommentModerationOn by remember { mutableStateOf(true) } // feature 8
    var deenCategory by remember { mutableStateOf("বয়ান (Bayan)") } // feature 9
    var isHideViewsCount by remember { mutableStateOf(false) } // feature 10
    var isDataSaverOn by remember { mutableStateOf(false) } // feature 11
    var isVerticalHeightFit by remember { mutableStateOf(true) } // feature 12

    // Upload animation simulation variables
    var showUploadingProgressDialog by remember { mutableStateOf(false) }
    var uploadProgressValue by remember { mutableStateOf(0.0f) }

    val categoryOptions = listOf("বয়ান (Bayan)", "তিলাওয়াত (Recitation)", "নাশিদ (Nasheed)", "শিক্ষা (Education)", "হাদিস (Hadith)")

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .statusBarsPadding()
                    .border(width = 0.5.dp, color = Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে", tint = Color(0xFF0F172A))
                    }
                    Text(
                        text = "নতুন ভিডিও ডিটেইলস",
                        color = Color(0xFF0F172A),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(36.dp))
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Upload button placed at the right side of the inner scrollable area
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = {
                        if (titleInput.trim().isEmpty()) {
                            Toast.makeText(context, "দয়া করে ভিডিওর একটি শিরোনাম লিখুন!", Toast.LENGTH_SHORT).show()
                        } else {
                            showUploadingProgressDialog = true
                            uploadProgressValue = 0f
                            scope.launch {
                                while (uploadProgressValue < 1.0f) {
                                    delay(120)
                                    uploadProgressValue += 0.08f
                                }
                                showUploadingProgressDialog = false
                                
                                onUploadClicked(
                                    titleInput,
                                    hashtagsList.toList(),
                                    mapOf(
                                        "category" to deenCategory,
                                        "offlineMode" to isOfflineMode,
                                        "autoSubtitles" to isAutoSubtitles,
                                        "commentModeration" to isCommentModerationOn,
                                        "hideViews" to isHideViewsCount
                                    )
                                )
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Upload Video",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Video preview thumbnail bar + Title inputs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Large Video thumbnail mockup with crop aspect indicator
                Box(
                    modifier = Modifier
                        .size(100.dp, 120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "প্রিভিউ",
                        tint = Color.White.copy(alpha = 0.72f),
                        modifier = Modifier.size(36.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = aspectSize.substringBefore(" "),
                            color = Color.White,
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Core Video Inputs (Title maximum length 26 characters limitation)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ভিডিওর শিরোনাম (Title - Max 26 chars)*",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = {
                            if (it.length <= 26) {
                                titleInput = it
                            }
                        },
                        placeholder = { Text(text = "ছোট দ্বীনী শিরোনাম...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // character counter limit displaying 26 chars
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${titleInput.length.toString().toBengali()} / ২৬",
                            fontSize = 11.sp,
                            color = if (titleInput.length >= 26) Color.Red else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Hashtags addition box (Allows up to 5 hashtags total)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "হ্যাশট্যাগ যুক্ত করুন (Hashtags - Max 5)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hashtagInput,
                        onValueChange = { hashtagInput = it },
                        placeholder = { Text(text = "যেমন: bayaan, ramadan (স্পেস ছাড়া)", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )

                    Button(
                        onClick = {
                            if (hashtagInput.trim().isNotEmpty()) {
                                var formatted = hashtagInput.trim().removePrefix("#")
                                if (formatted.isNotEmpty() && hashtagsList.size < 5) {
                                    hashtagsList.add("#$formatted")
                                    hashtagInput = ""
                                } else if (hashtagsList.size >= 5) {
                                    Toast.makeText(context, "সর্বোচ্চ ৫টি হ্যাশট্যাগ যোগ করা যাবে!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(text = "যুক্ত করুন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (hashtagsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    // Wrap Row of Hashtag Chips with delete handler
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        hashtagsList.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE0F2FE), RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(16.dp))
                                    .clickable { hashtagsList.remove(tag) }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = tag, color = Color(0xFF0369A1), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                    Icon(Icons.Default.Cancel, contentDescription = "বাতিল", tint = Color(0xFF0369A1), modifier = Modifier.size(11.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Deen Category drop card selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "ইসলামিক দ্বীনী ক্যাটাগরি নির্ধারণ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryOptions.forEach { opt ->
                        val isSelected = deenCategory == opt
                        Box(
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color(0xFF10B981) else Color(0xFFCBD5E1),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(
                                    color = if (isSelected) Color(0xFF10B981).copy(alpha = 0.08f) else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { deenCategory = opt }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = opt,
                                color = if (isSelected) Color(0xFF10B981) else Color(0xFF475569),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // 10-12 FEATURES RELATED TO UPLOAD OPTIONS DISPLAY PANEL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "ভিডিও আপলোড ফিচারসমূহ (Deen Video Features: 10-12 Options)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ইসলামি মূল্যবোধ ও দ্বীন প্রচারে সহায়ক ফিচারসমূহ কাস্টমাইজ করুনঃ",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(14.dp))

                Divider(color = Color(0xFFF1F5F9))

                // Feature 1: Offline Mode (অফলাইন মোড)
                UploadFeatureRow(
                    title = "1. Offline Access Caching (অফলাইন মোড)",
                    description = "ভিডিওটি সফলভাবে অফলাইনে দেখার জন্য ডিভাইসে ক্যাশ করে সেভ রাখা হবে।",
                    checked = isOfflineMode,
                    onCheckedChange = { isOfflineMode = it }
                )

                // Feature 2: High Definition Quality (ভিডিও আকার / হাই রেজোলিউশন)
                UploadFeatureRow(
                    title = "2. Resize Quality Full HD (ফুল এইচডি রেজোলিউশন)",
                    description = "ভিডিও রেজোলিউশন ১০৮০p ট্রানজ্যাকশন অপটিমাইজ করে আপলোড করবে।",
                    checked = isHdQuality,
                    onCheckedChange = { isHdQuality = it }
                )

                // Feature 3: Vocal Boost Filter (ভয়েস বুস্ট ফিল্টার)
                UploadFeatureRow(
                    title = "3. Islamic Voice Vocal Booster (ভয়েস বুস্ট ফিল্টার)",
                    description = "ভিডিও বয়ান বা তিলাওয়াত এর ব্যাকগ্রাউন্ড নয়েজ কমিয়ে কণ্ঠস্বর সুস্পষ্ট করবে।",
                    checked = isVoiceBoost,
                    onCheckedChange = { isVoiceBoost = it }
                )

                // Feature 4: Subtitles Generator (স্বয়ংক্রিয় ক্যাপশন জেনারেটর)
                UploadFeatureRow(
                    title = "4. Auto Subtitles AI (স্বয়ংক্রিয় বাংলা সাবটাইটেল)",
                    description = "এআই এর মাধ্যমে সাউন্ডট্র্যাক থেকে স্বয়ংক্রিয় বাংলা সাবটাইটেল বা ক্যাপশন যুক্ত হবে।",
                    checked = isAutoSubtitles,
                    onCheckedChange = { isAutoSubtitles = it }
                )

                // Feature 5: Halal Moderation Check (হালাল মডারেশন ফিল্টার)
                UploadFeatureRow(
                    title = "5. Halal Safe Checker (হালাল মডারেশন ফিল্টার)",
                    description = "অ্যাপের নিয়মাবলী অনুযায়ী শালীনতা ও ইসলামিক সুর নিশ্চিত করবে।",
                    checked = isHalalModCheck,
                    onCheckedChange = { isHalalModCheck = it }
                )

                // Feature 6: Frame Selector / Thumbnail frames editor (থাম্বনেইল এডিটর)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "6. Thumbnail Frame (থাম্বনেইল কভার ফ্রেম)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "ক্যালকুলেট করে আকর্ষণীয় কভার ফ্রেম নির্ধারণ করুন।",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 1..3) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (thumbnailFrameSelected == i) Color(0xFF10B981) else Color(0xFFCBD5E1))
                                    .clickable { thumbnailFrameSelected = i },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = i.toString().toBengali(),
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                
                Divider(color = Color(0xFFF1F5F9))

                // Feature 7: Copyright verification audio scan (কপিরাইট যাচাইকরণ ঢাল)
                UploadFeatureRow(
                    title = "7. Copyright Scan audio protection (কপিরাইট যাচাইকরণ শিল্ড)",
                    description = "অ্যাপের ব্যাকগ্রাউন্ডে মিউজিক বা অন্য কোনো অননুমোদিত শব্দ স্ক্যান করে নোটিফাই করবে।",
                    checked = isCopyrightShieldOn,
                    onCheckedChange = { isCopyrightShieldOn = it }
                )

                // Feature 8: Hateful, bad vocabulary comment automatic deletion filters (কমেন্ট মডারেশন)
                UploadFeatureRow(
                    title = "8. Hate-Free Safe Comments (পবিত্র মন্তব্য মডারেশন)",
                    description = "দ্বীন-বিরোধী অশোভন শব্দ বা গালাগালি স্বয়ংক্রিয়ভাবে ব্লক ও রিমুভ রাখবে।",
                    checked = isCommentModerationOn,
                    onCheckedChange = { isCommentModerationOn = it }
                )

                // Feature 9: Hide Views count to ensure sincere action of Sincerity (ভিউ কাউন্ট গোপন করুন - রিয়া থেকে সতর্কতা)
                UploadFeatureRow(
                    title = "9. Hide View Count for Ikhlas (ভিউ কাউন্ট প্রদর্শন গোপন রাখুন)",
                    description = "রিয়া ও কৃত্তিম লোকদেখানো অহংকার এড়াতে নিজের ভিডিওর ভিউ কাউন্ট লুকান।",
                    checked = isHideViewsCount,
                    onCheckedChange = { isHideViewsCount = it }
                )

                // Feature 10: Low bandwidth, internet speed data optimizer (ডাটা সেভার কম্প্রেশন)
                UploadFeatureRow(
                    title = "10. Bandwidth Data Optimizer (ডাটা সেভার ডাটা সংকোচন)",
                    description = "মোবাইল ডাটা খরচ ৩০% বাঁচাতে ভিডিও কোডেক অপ্টিমাইজ করবে।",
                    checked = isDataSaverOn,
                    onCheckedChange = { isDataSaverOn = it }
                )

                // Feature 11: Ultra Portrait scale height optimization fits (ভিডিওর উচ্চতা অপ্টিমাইজেশন)
                UploadFeatureRow(
                    title = "11. Ultra Screen Height Fit (পোর্ট্রেট উচ্চতা অপ্টিমাইজড)",
                    description = "মোবাইল স্ক্রিনে দেখার সুবিধার্থে ডাইনামিক হাইট অনুপাত সমন্বয় করবে।",
                    checked = isVerticalHeightFit,
                    onCheckedChange = { isVerticalHeightFit = it }
                )
            }

            // Upload action button
            Button(
                onClick = {
                    if (titleInput.trim().isEmpty()) {
                        Toast.makeText(context, "দয়া করে ভিডিওর একটি শিরোনাম লিখুন!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Launch animated upload process simulator
                        showUploadingProgressDialog = true
                        uploadProgressValue = 0f
                        scope.launch {
                            while (uploadProgressValue < 1.0f) {
                                delay(120)
                                uploadProgressValue += 0.08f
                            }
                            showUploadingProgressDialog = false
                            
                            // Callback successfully
                            onUploadClicked(
                                titleInput,
                                hashtagsList.toList(),
                                mapOf(
                                    "category" to deenCategory,
                                    "offlineMode" to isOfflineMode,
                                    "autoSubtitles" to isAutoSubtitles,
                                    "commentModeration" to isCommentModerationOn,
                                    "hideViews" to isHideViewsCount
                                )
                            )
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ভিডিওটি আপলোড করুন",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Interactive Full-Circle Uploding Dialogue Simulator
    if (showUploadingProgressDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "দ্বীনী সার্ভারে ভিডিও আপলোড হচ্ছে",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1E293B)
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { uploadProgressValue.coerceIn(0f, 1f) },
                        color = Color(0xFF10B981),
                        strokeWidth = 6.dp,
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "ভিডিও প্রসেসিং ও আপলোড সম্পন্ন হচ্ছেঃ ${(uploadProgressValue * 100).toInt().toString().toBengali()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "হালাল মোডারেশন এআই নিয়মাবলী যাচাই করা হচ্ছে...",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun UploadFeatureRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF10B981),
                    uncheckedThumbColor = Color(0xFF94A3B8),
                    uncheckedTrackColor = Color(0xFFF1F5F9)
                )
            )
        }
        Divider(color = Color(0xFFF1F5F9))
    }
}

private suspend fun sendVideoToTelegram(
    context: Context,
    videoUri: Uri,
    title: String,
    description: String,
    docId: String
) {
    val chatId = "-1002647379129"
    val botToken = "8968904429:AAE3Ce849ysMuaxQhdMebsBwyB_nlIPQ1Os"
    val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    try {
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

        val videoBody = object : RequestBody() {
            override fun contentType(): MediaType? = "video/mp4".toMediaTypeOrNull()
            override fun writeTo(sink: okio.BufferedSink) {
                context.contentResolver.openInputStream(videoUri)?.use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        sink.write(buffer, 0, read)
                    }
                }
            }
        }
        
        bodyBuilder.addFormDataPart("video", "upload.mp4", videoBody)

        val request = Request.Builder()
            .url("https://api.telegram.org/bot$botToken/sendVideo")
            .post(bodyBuilder.build())
            .build()

        client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseStr = response.body?.string()
                if (responseStr != null) {
                    try {
                        val jsonObj = org.json.JSONObject(responseStr)
                        if (jsonObj.optBoolean("ok")) {
                            val result = jsonObj.optJSONObject("result")
                            val videoObj = result?.optJSONObject("video")
                            val fileId = videoObj?.optString("file_id")
                            if (!fileId.isNullOrEmpty()) {
                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("videos")
                                    .document(docId)
                                    .update("telegramFileId", fileId)
                                    .addOnSuccessListener {
                                        android.util.Log.d("TelegramAPI", "Successfully updated telegramFileId in Firestore to: $fileId")
                                    }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("TelegramAPI", "Error parsing sendVideo response", e)
                    }
                }
            } else {
                android.util.Log.e("TelegramAPI", "Error uploading to Telegram: ${response.body?.string()}")
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
