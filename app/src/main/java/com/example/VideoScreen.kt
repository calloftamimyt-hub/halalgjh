package com.example

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.model.UserUploadedVideo
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class VideoItem(
    val id: Int, 
    val url: String, 
    val author: String, 
    val description: String,
    val category: String = "সাধারণ",
    val status: String = "APPROVED",
    val docId: String = "",
    val userId: String = "",
    val isOfflineMode: Boolean = false,
    val telegramFileId: String = "",
    val viewsCount: Long = 0L,
    val likedBy: List<String> = emptyList(),
    val sharesCount: Long = 0L,
    val title: String = "",
    val videoUri: String = ""
)

// dummyVideos removed to only show user-uploaded videos

// Change this to your deployed Google Apps Script URL!
const val TELEGRAM_PROXY_URL = "https://script.google.com/macros/s/AKfycbyse-oVHrCgGjsCtN7q_TaCEf6YIGKxWkpjL9ILq_Uems0odlikDcO9dAIUMWTlWQ4B8Q/exec"

@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoScreen(
    activeSubTab: String = "home",
    onActiveSubTabChange: (String) -> Unit = {},
    onRequireLogin: () -> Unit = {},
    onNavigateToCreatorProfile: (String, String) -> Unit = { _, _ -> },
    onNavigateToSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current

    val userVideosList = remember { mutableStateListOf<UserUploadedVideo>() }
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val currentUserId = remember { currentUser?.uid ?: "" }
    
    // Track viewed non-offline unique videos count
    val viewedNonOfflineVideos = remember { mutableStateListOf<String>() }
    val dismissedPendingBanners = remember { mutableStateMapOf<String, Boolean>() }
    
    // Real-time Firestore Listener
    LaunchedEffect(Unit) {
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection("videos")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        android.util.Log.w("Firebase", "Listen failed.", e)
                        return@addSnapshotListener
                    }
                    
                    if (snapshots != null) {
                        userVideosList.clear()
                        for (doc in snapshots) {
                            try {
                                val video = doc.toObject(UserUploadedVideo::class.java)
                                userVideosList.add(video)
                            } catch (e: Exception) {
                                android.util.Log.e("Firebase", "Error parsing doc: ${doc.id}", e)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("Firebase", "Firestore not initialized", e)
        }
    }

    val allCategories = listOf("All", "Bayan", "Recitation", "Nasheed", "Education", "Hadith")
    var selectedCategory by remember { mutableStateOf("All") }

    val combinedVideos = remember(userVideosList.toList(), selectedCategory) {
        val mapped = userVideosList.mapIndexed { idx, uv ->
            val playUrl = if (uv.telegramFileId.isNotEmpty() && !TELEGRAM_PROXY_URL.contains("YOUR_SCRIPT_ID")) {
                "$TELEGRAM_PROXY_URL?action=stream&file_id=${uv.telegramFileId}"
            } else if (uv.videoUri.startsWith("http")) {
                uv.videoUri
            } else if (uv.videoUri.startsWith("content") && uv.userId == currentUserId) {
                uv.videoUri
            } else {
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
            }
            VideoItem(
                id = 1000 + idx,
                url = playUrl,
                author = uv.author,
                description = uv.description,
                category = uv.category,
                status = uv.status,
                docId = uv.docId,
                userId = uv.userId,
                isOfflineMode = uv.isOfflineMode,
                telegramFileId = uv.telegramFileId,
                viewsCount = uv.viewsCount,
                likedBy = uv.likedBy,
                sharesCount = uv.sharesCount,
                title = uv.title,
                videoUri = uv.videoUri
            )
        }.filter { 
            it.status.trim().equals("APPROVED", ignoreCase = true)
        } // Only show APPROVED videos in the video category list

        val total = mapped
        if (selectedCategory == "All") total 
        else total.filter { it.category == selectedCategory }
    }

    var playingVideoId by remember(combinedVideos) { 
        mutableStateOf(combinedVideos.firstOrNull()?.id) 
    }

    // Track seen unique non-offline videos matching Facebook active play behavior
    LaunchedEffect(playingVideoId, combinedVideos) {
        val activeVideo = combinedVideos.find { it.id == playingVideoId }
        if (activeVideo != null && !activeVideo.isOfflineMode && activeVideo.docId.isNotEmpty()) {
            if (!viewedNonOfflineVideos.contains(activeVideo.docId)) {
                viewedNonOfflineVideos.add(activeVideo.docId)
            }
        }
    }
    
    val mainBgColor = Color(0xFFF0F2F5) // Beautiful Facebook app light grey background 
    Box(modifier = Modifier.fillMaxSize().background(mainBgColor)) {
        when (activeSubTab) {
            "home" -> {
                if (combinedVideos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 135.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "No videos found in this category" else "এই ক্যাটাগরিতে কোনো ভিডিও পাওয়া যায়নি", 
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 135.dp, bottom = 110.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            VideoCategoryList(
                                selectedCategory = selectedCategory,
                                allCategories = allCategories,
                                onCategoryChange = { selectedCategory = it },
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        items(combinedVideos) { video ->
                            val isLimitExceeded = currentUser == null && 
                                    viewedNonOfflineVideos.size > 5 && 
                                    !video.isOfflineMode

                            FacebookVideoPostCard(
                                video = video,
                                isPlaying = (playingVideoId == video.id),
                                onPlayClick = { 
                                    playingVideoId = if (playingVideoId == video.id) null else video.id 
                                },
                                isLimitExceeded = isLimitExceeded,
                                onRequireLogin = onRequireLogin,
                                onNavigateToCreatorProfile = onNavigateToCreatorProfile,
                                onNavigateToSaved = onNavigateToSaved,
                                dismissedPendingBanners = dismissedPendingBanners
                            )
                        }
                    }
                }
            }
            "analytics" -> {
                VideoAnalyticsScreen(
                    userVideos = userVideosList,
                    onRequireLogin = onRequireLogin
                )
            }
            "tools" -> {
                VideoToolsScreen()
            }
            "profile" -> {
                VideoProfileScreen(
                    userVideos = userVideosList,
                    onNavigateToCreate = onRequireLogin,
                    onNavigateToSaved = onNavigateToSaved,
                    onRequireLogin = onRequireLogin
                )
            }
        }

        // Global FIXED Top Icons
        VideoTopIcons(
            activeSubTab = activeSubTab,
            onActiveSubTabChange = onActiveSubTabChange
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoItem: VideoItem, 
    isSelected: Boolean, 
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isUserPaused by remember(videoItem.id, isSelected) { mutableStateOf(false) }
    
    LaunchedEffect(isSelected) {
        if (isSelected && videoItem.docId.isNotEmpty() && !videoItem.isOfflineMode) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("videos").document(videoItem.docId)
                    .update("viewsCount", com.google.firebase.firestore.FieldValue.increment(1))
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayer", "Error incrementing viewsCount", e)
            }
        }
    }
    
    val needsResolution = videoItem.telegramFileId.isNotEmpty() || 
                          videoItem.url.contains("file_id=") || 
                          videoItem.url.startsWith("https://script.google.com")

    var resolvedUrl by remember(videoItem.url, videoItem.telegramFileId) { 
        mutableStateOf(if (needsResolution) "" else videoItem.url) 
    }

    // Resolve URL first if it is a Telegram video or Google Apps Script Proxy URL or local content Uri
    LaunchedEffect(videoItem.url, videoItem.telegramFileId) {
        val fileId = if (videoItem.telegramFileId.isNotEmpty()) {
            videoItem.telegramFileId
        } else if (videoItem.url.contains("file_id=")) {
            videoItem.url.substringAfter("file_id=").substringBefore("&")
        } else {
            ""
        }

        if (fileId.isNotEmpty()) {
            val resolved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    val botToken = "8968904429:AAE3Ce849ysMuaxQhdMebsBwyB_nlIPQ1Os"
                    val request = okhttp3.Request.Builder()
                        .url("https://api.telegram.org/bot$botToken/getFile?file_id=$fileId")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val responseStr = response.body?.string()
                            if (!responseStr.isNullOrEmpty()) {
                                val jsonObj = org.json.JSONObject(responseStr)
                                if (jsonObj.optBoolean("ok")) {
                                    val result = jsonObj.optJSONObject("result")
                                    val filePath = result?.optString("file_path")
                                    if (!filePath.isNullOrEmpty()) {
                                        return@withContext "https://api.telegram.org/file/bot$botToken/$filePath"
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayer", "Error loading direct Telegram URL", e)
                }
                null
            }
            resolvedUrl = resolved ?: videoItem.url
            android.util.Log.d("VideoPlayer", "Resolved Telegram URL: $resolvedUrl")
        } else if (videoItem.url.startsWith("https://script.google.com")) {
            val resolved = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val client = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build()
                    val request = okhttp3.Request.Builder().url(videoItem.url).build()
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val directUrl = response.body?.string()?.trim()
                            if (!directUrl.isNullOrEmpty() && (directUrl.startsWith("http://") || directUrl.startsWith("https://"))) {
                                return@withContext directUrl
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayer", "Error loading direct video URL", e)
                }
                null
            }
            resolvedUrl = resolved ?: videoItem.url
            android.util.Log.d("VideoPlayer", "Resolved Apps Script URL: $resolvedUrl")
        }
    }

    DisposableEffect(resolvedUrl) {
        if (resolvedUrl.isEmpty()) {
            return@DisposableEffect onDispose {}
        }
        
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(resolvedUrl)))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = isSelected
            prepare()
        }
        exoPlayer = player

        onDispose {
            player.release()
            exoPlayer = null
        }
    }

    val shouldPlay = isSelected && !isUserPaused

    LaunchedEffect(shouldPlay) {
        exoPlayer?.let { player ->
            if (shouldPlay) {
                player.play()
            } else {
                player.pause()
            }
        }
    }
    
    Box(
        modifier = modifier
            .background(Color.Black)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) {
                isUserPaused = !isUserPaused
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { playerView ->
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // TikTok-style watermark (App Logo)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.2f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Halal Circle",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        if (isUserPaused) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun VideoOverlay(
    videoItem: VideoItem,
    onNavigateToCreatorProfile: (String, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
    val currentUserId = remember(currentUser) { currentUser?.uid ?: "" }

    var isLiked by remember(videoItem.likedBy, currentUserId) {
        mutableStateOf(currentUserId.isNotEmpty() && videoItem.likedBy.contains(currentUserId))
    }

    var isFollowed by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId, videoItem.userId) {
        if (currentUserId.isNotEmpty() && videoItem.userId.isNotEmpty()) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("follows")
                    .document("${currentUserId}_${videoItem.userId}")
                    .get()
                    .addOnSuccessListener { doc ->
                        isFollowed = doc.exists()
                    }
            } catch (e: Exception) {
                android.util.Log.e("VideoOverlay", "Error checking follow state", e)
            }
        }
    }

    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("ভুল তথ্য") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    // Loading user's customized profile logo selection
    val sharedPrefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    val selectedLogoIndex = sharedPrefs.getInt("selected_logo_index", 0)
    
    val logoIcons = listOf(
        Icons.Default.Person to Color(0xFF10B981),
        Icons.Default.Star to Color(0xFF3B82F6),
        Icons.Default.Favorite to Color(0xFFEC4899),
        Icons.Default.MenuBook to Color(0xFFD97706),
        Icons.Default.Face to Color(0xFF8B5CF6),
        Icons.Default.AccountCircle to Color(0xFF14B8A6)
    )
    val chosenLogo = logoIcons.getOrNull(selectedLogoIndex) ?: logoIcons[0]

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            if (videoItem.status == "PENDING") {
                Surface(
                    color = Color(0xFFF59E0B).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Pending Approval (Only visible to you)" else "অনুমোদন অপেক্ষমাণ (শুধু আপনি দেখছেন)",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = videoItem.author, 
                color = Color.White, 
                fontWeight = FontWeight.Bold, 
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = videoItem.description, 
                color = Color.White.copy(alpha = 0.9f), 
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
        
        // Right Action buttons (TikTok-style Vertical Panel)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(64.dp)
                .padding(bottom = 8.dp)
        ) {
            
            // 1. Profile Avatar at the Top with Overlaid Follow Icon
            Box(
                modifier = Modifier.padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Profile Circular Avatar displaying the user's custom chosen logo
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable {
                            if (videoItem.userId.isNotEmpty()) {
                                onNavigateToCreatorProfile(videoItem.userId, videoItem.author)
                            } else {
                                Toast.makeText(context, "এই ক্রিয়েটরের প্রোফাইল আইডি পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "প্রোফাইল লোগো",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Follow Plus/Check Icon overlapping at bottom-center of avatar
                Box(
                    modifier = Modifier
                        .offset(y = 10.dp)
                        .size(20.dp)
                        .background(if (isFollowed) PrimaryGreen else Color.Red, CircleShape)
                        .border(1.dp, Color.White, CircleShape)
                        .clickable {
                            if (currentUserId.isEmpty()) {
                                Toast.makeText(context, 
                                    if (com.example.viewmodel.GlobalLanguage.isEnglish) "Please log in to follow creators!" else "ফলো করতে দয়া করে লগইন করুন!", 
                                    Toast.LENGTH_SHORT).show()
                            } else if (videoItem.userId.isNotEmpty()) {
                                val nextFollowed = !isFollowed
                                isFollowed = nextFollowed
                                
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val followDoc = db.collection("follows").document("${currentUserId}_${videoItem.userId}")
                                if (nextFollowed) {
                                    val data = mapOf(
                                        "followerId" to currentUserId,
                                        "creatorId" to videoItem.userId,
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                    followDoc.set(data)
                                        .addOnSuccessListener {
                                            Toast.makeText(context, 
                                                if (com.example.viewmodel.GlobalLanguage.isEnglish) "You are now following ${videoItem.author}!" else "আপনি এখন ${videoItem.author} কে ফলো করছেন!", 
                                                Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    followDoc.delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(context, 
                                                if (com.example.viewmodel.GlobalLanguage.isEnglish) "Unfollowed ${videoItem.author}." else "ফলো করা বাতিল করা হয়েছে।", 
                                                Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFollowed) Icons.Default.Check else Icons.Default.Add,
                        contentDescription = "Follow Button",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 2. Like Button
            IconButton(
                onClick = {
                    if (currentUserId.isEmpty()) {
                        Toast.makeText(context, 
                            if (com.example.viewmodel.GlobalLanguage.isEnglish) "Please log in to like this video!" else "ভিডিও লাইক করতে দয়া করে লগইন করুন!", 
                            Toast.LENGTH_SHORT).show()
                    } else if (videoItem.docId.isNotEmpty()) {
                        val nextLikedState = !isLiked
                        isLiked = nextLikedState
                        
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val videoRef = db.collection("videos").document(videoItem.docId)
                        if (nextLikedState) {
                            videoRef.update("likedBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
                                .addOnFailureListener {
                                    android.util.Log.e("VideoOverlay", "Error liking video", it)
                                }
                        } else {
                            videoRef.update("likedBy", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                                .addOnFailureListener {
                                    android.util.Log.e("VideoOverlay", "Error unliking video", it)
                                }
                        }
                    }
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            val rawLikes = videoItem.likedBy.size + (if (isLiked && !videoItem.likedBy.contains(currentUserId)) 1 else if (!isLiked && videoItem.likedBy.contains(currentUserId)) -1 else 0)
            val likesCount = maxOf(0, rawLikes)
            val likesText = remember(likesCount) {
                if (likesCount >= 1000) {
                    val thousands = likesCount / 1000.0
                    val formatted = String.format(java.util.Locale.US, "%.1f", thousands)
                    "${formatted}K"
                } else {
                    likesCount.toString()
                }
            }
            Text(
                text = likesText, 
                color = Color.White, 
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // 3. Share Button
            IconButton(
                onClick = { 
                    Toast.makeText(context, "ভিডিওর লিংকটি ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                    if (videoItem.docId.isNotEmpty()) {
                        try {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            db.collection("videos").document(videoItem.docId)
                                .update("sharesCount", com.google.firebase.firestore.FieldValue.increment(1))
                        } catch (e: Exception) {
                            android.util.Log.e("VideoOverlay", "Error incrementing sharesCount", e)
                        }
                    }
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Text(
                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Share" else "শেয়ার", 
                color = Color.White, 
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // 4. Save Post Button (New)
            val trackerDb = remember { com.example.database.TrackerDatabase.getDatabase(context) }
            val savedPostDao = remember { trackerDb.savedPostDao() }
            var isSaved by remember { mutableStateOf(false) }
            LaunchedEffect(videoItem.docId) {
                isSaved = savedPostDao.getPostById(videoItem.docId) != null
            }

            IconButton(
                onClick = {
                    scope.launch {
                        val nextSavedState = !isSaved
                        isSaved = nextSavedState
                        
                        if (nextSavedState) {
                            savedPostDao.savePost(
                                com.example.database.SavedPost(
                                    docId = videoItem.docId,
                                    author = videoItem.author,
                                    description = videoItem.description,
                                    category = videoItem.category,
                                    status = videoItem.status,
                                    userId = videoItem.userId,
                                    telegramFileId = videoItem.telegramFileId,
                                    viewsCount = videoItem.viewsCount,
                                    sharesCount = videoItem.sharesCount,
                                    title = videoItem.title,
                                    videoUri = videoItem.videoUri,
                                    url = videoItem.url
                                )
                            )
                            Toast.makeText(context, if (com.example.viewmodel.GlobalLanguage.isEnglish) "Post saved locally!" else "পোস্টটি লোকালি সেভ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        } else {
                            val post = savedPostDao.getPostById(videoItem.docId)
                            if (post != null) {
                                savedPostDao.deletePost(post)
                                Toast.makeText(context, if (com.example.viewmodel.GlobalLanguage.isEnglish) "Removed from saved." else "সেভ থেকে মুছে ফেলা হয়েছে।", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save",
                    tint = if (isSaved) Color.Yellow else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Save" else "সেভ", 
                color = Color.White, 
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(14.dp))

            // 5. More Options Button (Three Dots)
            var showMoreOptions by remember { mutableStateOf(false) }
            
            Box {
                IconButton(
                    onClick = { showMoreOptions = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showMoreOptions,
                    onDismissRequest = { showMoreOptions = false },
                    modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Report Video" else "ভিডিও রিপোর্ট করুন",
                                    color = Color.Black
                                )
                            }
                        },
                        onClick = {
                            showMoreOptions = false
                            showReportDialog = true
                        }
                    )
                }
            }
            Text(
                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "More" else "আরও", 
                color = Color.White, 
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(14.dp))

            // 6. Download Button under More
            IconButton(
                onClick = { 
                    if (!isDownloading) {
                        isDownloading = true
                        downloadProgress = 0f
                        scope.launch {
                            while (downloadProgress < 1f) {
                                delay(150)
                                downloadProgress += 0.15f
                            }
                            isDownloading = false
                            val msg = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Video successfully saved to gallery!" else "ভিডিওটি সফলভাবে গ্যালারিতে সংরক্ষিত হয়েছে!"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = if (isDownloading) PrimaryGreen else Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Download" else "ডাউনলোড", 
                color = Color.White, 
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Dynamic Report Dialog
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Report Video" else "ভিডিও রিপোর্ট করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Identify the issue with this video. Help us maintain a safe environment." else "এই ভিডিওটির সমস্যা চিহ্নিত করুন। Halal Circle প্ল্যাটফর্মে নিরাপদ পরিবেশ বজায় রাখতে সাহায্য করুন।",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                    
                    val options = if (com.example.viewmodel.GlobalLanguage.isEnglish) {
                        listOf("Misinformation", "Inappropriate content", "Spam or scams", "Other")
                    } else {
                        listOf("ভুল তথ্য / অসত্য তথ্য", "অশালীন বিষয়বস্তু বা ভাষা", "স্প্যাম বা প্রতারণা", "অন্যান্য")
                    }
                    options.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReportReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedReportReason == reason,
                                onClick = { selectedReportReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = reason, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReportDialog = false
                        val msg = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Report submitted! We will review it." else "রিপোর্ট জমা নেওয়া হয়েছে! পর্যালোচনা করে ব্যবস্থা নেওয়া হবে।"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Submit Report" else "রিপোর্ট জমা দিন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Cancel" else "বাতিল", color = Color.Gray)
                }
            }
        )
    }

    // Download Simulation Dialog
    if (isDownloading) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    text = "ভিডিও ডাউনলোড হচ্ছে",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        color = PrimaryGreen,
                        strokeWidth = 4.dp,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "গ্যালারিতে সেভ করার জন্য প্রসেস করা হচ্ছে: ${(downloadProgress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun IconSubTabButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val activeColor = if (isDarkTheme) Color.White else PrimaryGreen
    val inactiveColor = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color(0xFF6B7280)
    val contentColor = if (isSelected) activeColor else inactiveColor

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun VideoCategoryList(
    selectedCategory: String,
    allCategories: List<String>,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allCategories) { category ->
                val isSelected = selectedCategory == category
                Surface(
                    onClick = { onCategoryChange(category) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) PrimaryGreen else Color(0xFFDCDFE4)
                    )
                ) {
                    Text(
                        text = category,
                        color = if (isSelected) PrimaryGreen else Color(0xFF050505),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun VideoTopIcons(
    activeSubTab: String,
    onActiveSubTabChange: (String) -> Unit
) {
    val isDarkTheme = false
    val backgroundModifier = Modifier.background(Color.White)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = androidx.compose.ui.graphics.RectangleShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(backgroundModifier)
                .statusBarsPadding()
        ) {
            // First Row: Halal Circle Brand Heading in English
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Halal Circle",
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.weight(1f))
            }

            HorizontalDivider(
                color = Color(0xFFF0F2F5),
                thickness = 1.dp
            )

            // Second Row: Navigation Icons Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconSubTabButton(
                    icon = Icons.Default.Home,
                    label = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Home Feed" else "হোম ফিড",
                    isSelected = activeSubTab == "home",
                    isDarkTheme = isDarkTheme,
                    onClick = { onActiveSubTabChange("home") }
                )
                Spacer(modifier = Modifier.weight(1f))
                IconSubTabButton(
                    icon = Icons.Default.TrendingUp,
                    label = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Analytics" else "অ্যানালিটিক্স",
                    isSelected = activeSubTab == "analytics",
                    isDarkTheme = isDarkTheme,
                    onClick = { onActiveSubTabChange("analytics") }
                )
                Spacer(modifier = Modifier.weight(1.8f)) // Gap between Analytics and Tools!
                IconSubTabButton(
                    icon = Icons.Default.Build,
                    label = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Tools" else "টুলস",
                    isSelected = activeSubTab == "tools",
                    isDarkTheme = isDarkTheme,
                    onClick = { onActiveSubTabChange("tools") }
                )
                Spacer(modifier = Modifier.weight(1f))
                IconSubTabButton(
                    icon = Icons.Default.Person,
                    label = if (com.example.viewmodel.GlobalLanguage.isEnglish) "My Profile" else "আমার প্রোফাইল",
                    isSelected = activeSubTab == "profile",
                    isDarkTheme = isDarkTheme,
                    onClick = { onActiveSubTabChange("profile") }
                )
            }
        }
    }
}

@Composable
fun FacebookVideoPostCard(
    video: VideoItem,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    isLimitExceeded: Boolean,
    onRequireLogin: () -> Unit,
    onNavigateToCreatorProfile: (String, String) -> Unit,
    onNavigateToSaved: () -> Unit,
    dismissedPendingBanners: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
    val currentUserId = remember(currentUser) { currentUser?.uid ?: "" }

    var isLiked by remember(video.likedBy, currentUserId) {
        mutableStateOf(currentUserId.isNotEmpty() && video.likedBy.contains(currentUserId))
    }

    var isFollowed by remember { mutableStateOf(false) }

    LaunchedEffect(currentUserId, video.userId) {
        if (currentUserId.isNotEmpty() && video.userId.isNotEmpty()) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("follows")
                    .document("${currentUserId}_${video.userId}")
                    .get()
                    .addOnSuccessListener { doc ->
                        isFollowed = doc.exists()
                    }
            } catch (e: Exception) {
                android.util.Log.e("FacebookVideoPostCard", "Error checking follow state", e)
            }
        }
    }

    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("ভুল তথ্য") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDescExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = androidx.compose.ui.graphics.RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryGreen.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, PrimaryGreen.copy(alpha = 0.2f), CircleShape)
                        .clickable {
                            if (video.userId.isNotEmpty()) {
                                onNavigateToCreatorProfile(video.userId, video.author)
                            } else {
                                Toast.makeText(context, "এই ক্রিয়েটরের প্রোফাইল আইডি পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "প্রোফাইল",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = video.author,
                            color = Color(0xFF050505),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.clickable {
                                if (video.userId.isNotEmpty()) {
                                    onNavigateToCreatorProfile(video.userId, video.author)
                                }
                            }
                        )
                        
                        if (video.userId.isNotEmpty() && video.userId != currentUserId) {
                            Text(
                                text = if (isFollowed) " • Following" else " • Follow",
                                color = if (isFollowed) Color(0xFF65676B) else Color(0xFF1877F2),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .clickable {
                                        if (currentUserId.isEmpty()) {
                                            Toast.makeText(context, "ফলো করতে দয়া করে লগইন করুন!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val nextFollowed = !isFollowed
                                            isFollowed = nextFollowed
                                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            val followDoc = db.collection("follows").document("${currentUserId}_${video.userId}")
                                            if (nextFollowed) {
                                                followDoc.set(mapOf(
                                                    "followerId" to currentUserId,
                                                    "creatorId" to video.userId,
                                                    "timestamp" to System.currentTimeMillis()
                                                )).addOnSuccessListener {
                                                    Toast.makeText(context, "ফলো করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                followDoc.delete().addOnSuccessListener {
                                                    Toast.makeText(context, "ফলো বাতিল করা হয়েছে।", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ক্যাটাগরি: ${video.category}",
                        color = Color(0xFF65676B),
                        fontSize = 12.sp
                    )
                }

                var showMoreDropdown by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMoreDropdown = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreHoriz,
                            contentDescription = "মোর অপশন",
                            tint = Color(0xFF65676B)
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreDropdown,
                        onDismissRequest = { showMoreDropdown = false },
                        modifier = Modifier.background(Color.White, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Flag, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("ভিডিও রিপোর্ট করুন", color = Color.Black)
                                }
                            },
                            onClick = {
                                showMoreDropdown = false
                                showReportDialog = true
                            }
                        )
                    }
                }
            }

            if (video.description.isNotEmpty()) {
                val isLong = video.description.length > 120
                val descText = if (isLong && !isDescExpanded) {
                    video.description.take(120) + "..."
                } else {
                    video.description
                }
                
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                    Text(
                        text = descText,
                        color = Color(0xFF050505),
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    if (isLong) {
                        Text(
                            text = if (isDescExpanded) "সংক্ষিপ্ত করুন" else "আরও দেখুন (Read More)",
                            color = Color(0xFF1877F2),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { isDescExpanded = !isDescExpanded }
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isLimitExceeded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.95f))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Sign In Required" else "লগইন করা আবশ্যক",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) 
                                    "Log in to watch more videos." 
                                    else "পরবর্তী ভিডিওগুলো দেখতে দয়া করে লগইন করুন।",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onRequireLogin() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("লগইন বা রেজিস্টার করুন", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    VideoPlayer(
                        videoItem = video,
                        isSelected = isPlaying,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = "Halal Circle",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!isPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onPlayClick() }
                                .background(Color.Black.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "ভিডিও প্লে করুন",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    if (video.status == "PENDING" && dismissedPendingBanners[video.docId] != true) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "অনুমোদন অপেক্ষমাণ (Pending)",
                                    color = Color.Yellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "community মডারেটরের অ্যাপ্রুভালের জন্য অপেক্ষমাণ।",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { dismissedPendingBanners[video.docId] = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("প্লে করুন", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            val rawLikes = video.likedBy.size + (if (isLiked && !video.likedBy.contains(currentUserId)) 1 else if (!isLiked && video.likedBy.contains(currentUserId)) -1 else 0)
            val likesCount = maxOf(0, rawLikes)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFF1877F2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$likesCount",
                        color = Color(0xFF65676B),
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "${video.viewsCount} views • ${video.sharesCount} shares",
                    color = Color(0xFF65676B),
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(color = Color(0xFFE4E6EB), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            if (currentUserId.isEmpty()) {
                                Toast.makeText(context, "ভিডিও লাইক করতে দয়া করে লগইন করুন!", Toast.LENGTH_SHORT).show()
                            } else if (video.docId.isNotEmpty()) {
                                val nextLikedState = !isLiked
                                isLiked = nextLikedState
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val videoRef = db.collection("videos").document(video.docId)
                                if (nextLikedState) {
                                    videoRef.update("likedBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
                                } else {
                                    videoRef.update("likedBy", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "লাইক",
                        tint = if (isLiked) Color(0xFF1877F2) else Color(0xFF65676B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "লাইক",
                        color = if (isLiked) Color(0xFF1877F2) else Color(0xFF65676B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            Toast.makeText(context, "ভিডিওর লিংকটি ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            if (video.docId.isNotEmpty()) {
                                try {
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    db.collection("videos").document(video.docId)
                                        .update("sharesCount", com.google.firebase.firestore.FieldValue.increment(1))
                                } catch (e: Exception) {
                                    android.util.Log.e("FacebookVideoPostCard", "Error incrementing shares", e)
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "শেয়ার",
                        tint = Color(0xFF65676B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "শেয়ার",
                        color = Color(0xFF65676B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val trackerDb = remember { com.example.database.TrackerDatabase.getDatabase(context) }
                val savedPostDao = remember { trackerDb.savedPostDao() }
                var isSaved by remember { mutableStateOf(false) }
                LaunchedEffect(video.docId) {
                    isSaved = savedPostDao.getPostById(video.docId) != null
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            scope.launch {
                                val nextSavedState = !isSaved
                                isSaved = nextSavedState
                                
                                if (nextSavedState) {
                                    savedPostDao.savePost(
                                        com.example.database.SavedPost(
                                            docId = video.docId,
                                            author = video.author,
                                            description = video.description,
                                            category = video.category,
                                            status = video.status,
                                            userId = video.userId,
                                            telegramFileId = video.telegramFileId,
                                            viewsCount = video.viewsCount,
                                            sharesCount = video.sharesCount,
                                            title = video.title,
                                            videoUri = video.videoUri,
                                            url = video.url
                                        )
                                    )
                                    Toast.makeText(context, "সেভ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                } else {
                                    val post = savedPostDao.getPostById(video.docId)
                                    if (post != null) {
                                        savedPostDao.deletePost(post)
                                        Toast.makeText(context, "সেভ থেকে রিমুভ করা হয়েছে।", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "সেভ",
                        tint = if (isSaved) Color(0xFFE0A900) else Color(0xFF65676B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "সেভ",
                        color = if (isSaved) Color(0xFFB58400) else Color(0xFF65676B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            if (!isDownloading) {
                                isDownloading = true
                                downloadProgress = 0f
                                scope.launch {
                                    while (downloadProgress < 1f) {
                                        delay(150)
                                        downloadProgress += 0.15f
                                    }
                                    isDownloading = false
                                    Toast.makeText(context, "ভিডিওটি সফলভাবে গ্যালারিতে সংরক্ষিত হয়েছে!", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Download,
                        contentDescription = "ডাউনলোড",
                        tint = if (isDownloading) PrimaryGreen else Color(0xFF65676B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ডাউনলোড",
                        color = if (isDownloading) PrimaryGreen else Color(0xFF65676B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("ভিডিও রিপোর্ট করুন", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf("ভুল তথ্য / অসত্য তথ্য", "অশালীন বিষয়বস্তু বা ভাষা", "স্প্যাম বা প্রতারণা", "অন্যান্য")
                    options.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedReportReason = reason }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedReportReason == reason,
                                onClick = { selectedReportReason = reason },
                                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = reason, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReportDialog = false
                        Toast.makeText(context, "রিপোর্ট জমা নেওয়া হয়েছে! পর্যালোচনা করে ব্যবস্থা নেওয়া হবে।", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("রিপোর্ট জমা দিন", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("বাতিল", color = Color.Gray)
                }
            }
        )
    }

    if (isDownloading) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("ভিডিও ডাউনলোড হচ্ছে", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        color = PrimaryGreen,
                        strokeWidth = 4.dp,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "গ্যালারিতে সেভ করার জন্য প্রসেস করা হচ্ছে: ${(downloadProgress * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            },
            confirmButton = {}
        )
    }
}
