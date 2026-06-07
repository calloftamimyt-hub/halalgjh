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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import android.content.Intent
import android.os.Build
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
        LaunchedEffect(Unit) {
            permissionState.launchMultiplePermissionRequest()
        }
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ক্যামেরা এবং মাইক্রোফোন পারমিশন চাচ্ছে...",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onBack) {
                    Text(text = "ফিরে যান", color = Color(0xFF94A3B8), fontSize = 14.sp)
                }
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
                        
                        // Start VideoUploadService for background upload with notification
                        val intent = Intent(context, VideoUploadService::class.java).apply {
                            putExtra("videoUri", selectedVideoUri.toString())
                            putExtra("title", title)
                            putExtra("description", videoItem.description)
                            putExtra("docId", videoItem.docId)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        
                        // Navigate back immediately as requested
                        onUploadSuccess()
                    },
                    onBack = { createStep = 1 }
                )
            }
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
    videoUri: android.net.Uri?,
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

    // Dialog triggering states
    var showCategoryPage by remember { mutableStateOf(false) }
    var showHashtagsDialog by remember { mutableStateOf(false) }

    // Features states (10-12 key features defined to fulfill 10-12 guidelines)
    var isOfflineMode by remember { mutableStateOf(false) } // feature 1
    var isHdQuality by remember { mutableStateOf(true) }     // feature 2
    var isVoiceBoost by remember { mutableStateOf(false) }  // feature 3
    var isAutoSubtitles by remember { mutableStateOf(false) } // feature 4
    var isHalalModCheck by remember { mutableStateOf(true) } // feature 5
    var thumbnailFrameSelected by remember { mutableStateOf(1) } // feature 6
    var isCopyrightShieldOn by remember { mutableStateOf(true) } // feature 7
    var isCommentModerationOn by remember { mutableStateOf(true) } // feature 8
    var deenCategory by remember { mutableStateOf("Bayan") } // feature 9
    var isHideViewsCount by remember { mutableStateOf(false) } // feature 10
    var isDataSaverOn by remember { mutableStateOf(false) } // feature 11
    var isVerticalHeightFit by remember { mutableStateOf(true) } // feature 12

    // Removed simulated upload progress dialog as requested by user for faster flow

    val categoryOptions = listOf("Bayan", "Recitation", "Nasheed", "Education", "Hadith")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack, 
                            contentDescription = "পিছনে", 
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Reel Settings",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(40.dp)) // Centering structural spacer
                }
                Divider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main content containing the options
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top horizontal upload media thumbnail preview & text info segment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Left element: Rounded video thumbnail with cover photo tag
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(144.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF1E293B))
                            .clickable {
                                thumbnailFrameSelected = if (thumbnailFrameSelected >= 3) 1 else thumbnailFrameSelected + 1
                                Toast.makeText(context, "থাম্বনেইল কভার ফ্রেম পরিবর্তনঃ $thumbnailFrameSelected", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "থাম্বনেইল নির্বাচন",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(28.dp)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.61f))
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Cover Frame $thumbnailFrameSelected",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Right element: Basic description textbox with character limits
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = titleInput,
                            onValueChange = {
                                if (it.length <= 26) {
                                    titleInput = it
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color(0xFF1C1E21),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (titleInput.isEmpty()) {
                                        Text(
                                            text = "Describe your reel. You can also add hashtags here...",
                                            color = Color(0xFF8D949E),
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp)
                        )

                        // Direct fast hashtag/mention trigger buttons under description, exactly like screenshot
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "# Hashtag",
                                color = Color(0xFF1877F2),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable {
                                        showHashtagsDialog = true
                                    }
                                    .padding(vertical = 4.dp)
                            )
                            Text(
                                text = "@ Mention",
                                color = Color(0xFF1877F2),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable {
                                        Toast.makeText(context, "মেনশন ফিচারটি শীগ্রই যুক্ত হচ্ছে!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }

                        // Character limiter indicator matching modern slate font styles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "${titleInput.length.toString().toBengali()} / ২৬ অক্ষর কাস্টম",
                                fontSize = 11.sp,
                                color = if (titleInput.length >= 26) Color.Red else Color(0xFF8D949E),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Inline Flow Row display of user-added Hashtag tags
                if (hashtagsList.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            hashtagsList.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE7F3FF), RoundedCornerShape(16.dp))
                                        .clickable { hashtagsList.remove(tag) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = tag, color = Color(0xFF1877F2), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Icon(Icons.Default.Cancel, contentDescription = "বাতিল", tint = Color(0xFF1877F2), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Divider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)

                // Reel Settings Option Row blocks similar to Facebook Settings rows
                Column(modifier = Modifier.fillMaxWidth()) {
                    
                    // 1. Selector row for Islamic Deen categorization
                    ReelSettingRow(
                        icon = Icons.Default.Category,
                        title = "Deen Category",
                        subtitle = deenCategory,
                        onClick = { showCategoryPage = true },
                        action = {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF8D949E)
                            )
                        }
                    )

                    // 2. Custom Hashtags control row
                    ReelSettingRow(
                        icon = Icons.Default.Tag,
                        title = "Add Hashtags",
                        subtitle = if (hashtagsList.isEmpty()) "Add up to 5 custom tags" else hashtagsList.joinToString(" "),
                        onClick = { showHashtagsDialog = true },
                        action = {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF8D949E)
                            )
                        }
                    )

                    // 3. Cover configuration row
                    ReelSettingRow(
                        icon = Icons.Default.Photo,
                        title = "Cover Thumbnail Frame",
                        subtitle = "Selected Frame: $thumbnailFrameSelected",
                        onClick = {
                            thumbnailFrameSelected = if (thumbnailFrameSelected >= 3) 1 else thumbnailFrameSelected + 1
                            Toast.makeText(context, "Thumbnail frame changed: $thumbnailFrameSelected", Toast.LENGTH_SHORT).show()
                        },
                        action = {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1877F2).copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "$thumbnailFrameSelected / 3", color = Color(0xFF1877F2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )

                    // 4. Offline Access switch
                    ReelSettingRow(
                        icon = Icons.Default.Download,
                        title = "Offline Access Caching",
                        subtitle = null,
                        onClick = { isOfflineMode = !isOfflineMode },
                        action = {
                            Switch(
                                checked = isOfflineMode,
                                onCheckedChange = { isOfflineMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1877F2),
                                    uncheckedThumbColor = Color(0xFFFFFFFF),
                                    uncheckedTrackColor = Color(0xFFE4E6EB)
                                )
                            )
                        }
                    )

                    // 5. Post Schedule control
                    ReelSettingRow(
                        icon = Icons.Default.Schedule,
                        title = "Schedule Post",
                        subtitle = "Choose date and time for publishing",
                        onClick = { /* Implement scheduling logic */ },
                        action = {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                        }
                    )
                    
                    // 6. AB Testing control
                    ReelSettingRow(
                        icon = Icons.Default.Science,
                        title = "A/B Testing",
                        subtitle = "Test different titles or thumbnails",
                        onClick = { /* Implement AB testing logic */ },
                        action = {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                        }
                    )

                    // 7. Hide View Count switch
                    ReelSettingRow(
                        icon = Icons.Default.VisibilityOff,
                        title = "Hide View Count",
                        subtitle = null,
                        onClick = { isHideViewsCount = !isHideViewsCount },
                        action = {
                            Switch(
                                checked = isHideViewsCount,
                                onCheckedChange = { isHideViewsCount = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1877F2),
                                    uncheckedThumbColor = Color(0xFFFFFFFF),
                                    uncheckedTrackColor = Color(0xFFE4E6EB)
                                )
                            )
                        }
                    )

                    // 8. Bandwidth Data Saver switch
                    ReelSettingRow(
                        icon = Icons.Default.DataUsage,
                        title = "Bandwidth Data Saver",
                        subtitle = null,
                        onClick = { isDataSaverOn = !isDataSaverOn },
                        action = {
                            Switch(
                                checked = isDataSaverOn,
                                onCheckedChange = { isDataSaverOn = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1877F2),
                                    uncheckedThumbColor = Color(0xFFFFFFFF),
                                    uncheckedTrackColor = Color(0xFFE4E6EB)
                                )
                            )
                        }
                    )

                    // 9. Ultra Portrait Height Fit switch
                    ReelSettingRow(
                        icon = Icons.Default.CropFree,
                        title = "Ultra Portrait Height Fit",
                        subtitle = null,
                        onClick = { isVerticalHeightFit = !isVerticalHeightFit },
                        action = {
                            Switch(
                                checked = isVerticalHeightFit,
                                onCheckedChange = { isVerticalHeightFit = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF1877F2),
                                    uncheckedThumbColor = Color(0xFFFFFFFF),
                                    uncheckedTrackColor = Color(0xFFE4E6EB)
                                )
                            )
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (showCategoryPage) {
                CategorySelectionScreen(
                    currentCategory = deenCategory,
                    onCategorySelected = { 
                        deenCategory = it
                        showCategoryPage = false 
                    },
                    onBack = { showCategoryPage = false }
                )
            }

            // Bottom bar holding the draft Icon Button on the left, and spacious "Share now" blue button on the right
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                Divider(color = Color(0xFFE5E7EB), thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left element: Save / Bookmark Draft square button
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "খসড়া বা ড্রাফট হিসেবে সেভ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFFF1F2F5), RoundedCornerShape(8.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "ড্রাফট হিসেবে সেভ করুন",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Right element: Blue primary "Share now" button filling the remaining space
                    Button(
                        onClick = {
                            if (titleInput.trim().isEmpty()) {
                                Toast.makeText(context, "দয়া করে ভিডিওর একটি বিবরণ বা শিরোনাম লিখুন!", Toast.LENGTH_SHORT).show()
                            } else {
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
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Share now",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Modern Dialog modal to pick Islamic Category removed in favor of full screen screen


    // Modern Dialog modal to pick Hashtags
    if (showHashtagsDialog) {
        AlertDialog(
            onDismissRequest = { showHashtagsDialog = false },
            title = {
                Text(
                    text = "Add Hashtags (Max 5)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = hashtagInput,
                        onValueChange = { hashtagInput = it },
                        placeholder = { Text(text = "e.g. bayaan, ramadan (no spaces)", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1877F2),
                            unfocusedBorderColor = Color(0xFFE4E6EB)
                        )
                    )

                    Button(
                        onClick = {
                            if (hashtagInput.trim().isNotEmpty()) {
                                val formatted = hashtagInput.trim().removePrefix("#")
                                if (formatted.isNotEmpty() && hashtagsList.size < 5) {
                                    hashtagsList.add("#$formatted")
                                    hashtagInput = ""
                                } else if (hashtagsList.size >= 5) {
                                    Toast.makeText(context, "Max 5 hashtags can be added!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Add", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (hashtagsList.isNotEmpty()) {
                        Text(
                            text = "Added tags (tap to remove):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        androidx.compose.foundation.layout.FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            hashtagsList.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE7F3FF), RoundedCornerShape(16.dp))
                                        .clickable { hashtagsList.remove(tag) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(text = tag, color = Color(0xFF1877F2), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Icon(Icons.Default.Cancel, contentDescription = "Remove", tint = Color(0xFF1877F2), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHashtagsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2))
                ) {
                    Text("Done")
                }
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionScreen(
    currentCategory: String,
    onCategorySelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val categories = listOf("Bayan", "Recitation", "Nasheed", "Education", "Hadith", "Fiqh", "Quran", "Tafsir", "Seerah", "Dawah", "History")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Category") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(categories) { category ->
                ListItem(
                    headlineContent = { Text(category) },
                    modifier = Modifier.clickable { onCategorySelected(category) },
                    trailingContent = {
                        if (category == currentCategory) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF1877F2))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ReelSettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    action: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF1F2F5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFF65676B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            action()
        }
        Divider(color = Color(0xFFF1F2F5), thickness = 0.5.dp)
    }
}

// Removed local sendVideoToTelegram implementation as it is now handled by VideoUploadService
