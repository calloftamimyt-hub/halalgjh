package com.example

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.example.model.SuggestedProfile
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

import android.app.DownloadManager
import android.os.Environment
import com.example.database.TrackerDatabase
import com.example.database.SavedPost

fun formatNumber(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

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
    val videoUri: String = "",
    val thumbnailUrl: String = ""
)

// dummyVideos removed to only show user-uploaded videos

// Change this to your deployed Google Apps Script URL!
const val TELEGRAM_PROXY_URL = "https://script.google.com/macros/s/AKfycbyse-oVHrCgGjsCtN7q_TaCEf6YIGKxWkpjL9ILq_Uems0odlikDcO9dAIUMWTlWQ4B8Q/exec"

fun getYoutubeThumbnail(videoUri: String): String {
    if (videoUri.contains("youtube.com/watch?v=")) {
        val videoId = videoUri.substringAfter("v=").substringBefore("&")
        return "https://img.youtube.com/vi/$videoId/0.jpg"
    } else if (videoUri.contains("youtu.be/")) {
        val videoId = videoUri.substringAfter("youtu.be/").substringBefore("?")
        return "https://img.youtube.com/vi/$videoId/0.jpg"
    }
    return ""
}

val demoVideos = listOf(
    VideoItem(
        id = 101,
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        author = "ইসলামিক টিপস",
        description = "লগইন করে প্রতিদিনের নতুন নতুন ইসলামিক ভিডিও দেখুন।",
        category = "নমুনা",
        title = "পবিত্রতা ঈমানের অঙ্গ - ডেমো ভিডিও"
    ),
    VideoItem(
        id = 102,
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        author = "কুরআন শিক্ষা",
        description = "সঠিক নিয়মে কুরআন শিখতে আমাদের সাথেই থাকুন।",
        category = "নমুনা",
        title = "সূরা ফাতিহার গুরুত্ব - ডেমো ভিডিও"
    ),
    VideoItem(
        id = 103,
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        author = "হাদিস বাণী",
        description = "প্রতিদিনের আমল ও জিকির সম্পর্কে জানুন।",
        category = "নমুনা",
        title = "উত্তম নৈতিকতা - ডেমো ভিডিও"
    ),
    VideoItem(
        id = 104,
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        author = "ইসলামিক জীবন",
        description = "সাফল্যের চাবিকাঠি হিসেবে ধৈর্য ধারণ করুন।",
        category = "নমুনা",
        title = "ধৈর্যের প্রতিদান - ডেমো ভিডিও"
    ),
    VideoItem(
        id = 105,
        url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        author = "বাচ্চাদের ইসলাম",
        description = "ছোটদের জন্য মজার মজার ইসলামিক গল্প।",
        category = "নমুনা",
        title = "সততার পুরস্কার - ডেমো ভিডিও"
    )
)

@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoScreen(
    activeSubTab: String = "home",
    onActiveSubTabChange: (String) -> Unit = {},
    onRequireLogin: () -> Unit = {},
    onNavigateToCreatorProfile: (String, String) -> Unit = { _, _ -> },
    onNavigateToSaved: () -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    isFeedActive: Boolean = true
) {
    val context = LocalContext.current
    val view = LocalView.current

    var approvedVideos by remember { mutableStateOf<List<UserUploadedVideo>>(emptyList()) }
    var myVideos by remember { mutableStateOf<List<UserUploadedVideo>>(emptyList()) }
    
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val currentUserId = remember { currentUser?.uid ?: "" }
    
    val userVideosList = remember(approvedVideos, myVideos) {
        val combined = (approvedVideos + myVideos).distinctBy { it.docId }
        combined.sortedByDescending { it.timestamp }
    }
    
    // Track viewed non-offline unique videos count
    val viewedNonOfflineVideos = remember { mutableStateListOf<String>() }
    val dismissedPendingBanners = remember { mutableStateMapOf<String, Boolean>() }
    
    var isVideosLoading by remember { mutableStateOf(true) }
    
    // Pagination state
    var lastVisibleDoc by remember { mutableStateOf<com.google.firebase.firestore.DocumentSnapshot?>(null) }
    var isFetchingMore by remember { mutableStateOf(false) }

    // Real-time Firestore Listener
    DisposableEffect(currentUserId) {
        if (currentUserId.isEmpty()) {
            // Guest mode: no need to fetch from Firestore
            isVideosLoading = false
            return@DisposableEffect onDispose {}
        }

        // Safety timeout for loading
        val loadingTimeoutTask = java.util.Timer().schedule(object : java.util.TimerTask() {
            override fun run() {
                isVideosLoading = false
            }
        }, 5000)

        var isApprovedLoaded = false
        var isMyVideosLoaded = false
        
        fun checkFinished() {
            if (isApprovedLoaded && isMyVideosLoaded) {
                isVideosLoading = false
            }
        }
        
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        
        // Initial Query: Get first 15 APPROVED videos (increased for better early experience)
        val approvedListener = firestore.collection("videos")
            .whereEqualTo("status", "APPROVED")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(15)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    android.util.Log.e("VideoScreen", "Approved videos listen failed: ${e.message}", e)
                    // If index is missing, we might need a simpler query as fallback
                    if (e.message?.contains("index") == true) {
                        firestore.collection("videos")
                            .whereEqualTo("status", "APPROVED")
                            .limit(15)
                            .get()
                            .addOnSuccessListener { fallbackSnapshots ->
                                val list = mutableListOf<UserUploadedVideo>()
                                for (doc in fallbackSnapshots) {
                                    doc.toObject(UserUploadedVideo::class.java)?.copy(docId = doc.id)?.let { list.add(it) }
                                }
                                approvedVideos = list.sortedByDescending { it.timestamp }
                                isApprovedLoaded = true
                                checkFinished()
                            }
                    } else {
                        isApprovedLoaded = true
                        isVideosLoading = false
                    }
                    return@addSnapshotListener
                }
                
                if (snapshots != null) {
                    android.util.Log.d("VideoScreen", "Approved snapshots received: ${snapshots.size()}")
                    val list = mutableListOf<UserUploadedVideo>()
                    for (doc in snapshots) {
                        doc.toObject(UserUploadedVideo::class.java)?.copy(docId = doc.id)?.let { list.add(it) }
                    }
                    approvedVideos = list
                    if (!snapshots.isEmpty) {
                        lastVisibleDoc = snapshots.documents[snapshots.size() - 1]
                    }
                    
                    isApprovedLoaded = true
                    checkFinished()
                }
            }
            
        // My Videos Listener
        val myListener = firestore.collection("videos")
            .whereEqualTo("userId", currentUserId)
            .limit(20)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    isMyVideosLoaded = true
                    checkFinished()
                    return@addSnapshotListener
                }
                if (snapshots != null) {
                    val list = mutableListOf<UserUploadedVideo>()
                    for (doc in snapshots) {
                        doc.toObject(UserUploadedVideo::class.java)?.copy(docId = doc.id)?.let { list.add(it) }
                    }
                    myVideos = list
                    isMyVideosLoaded = true
                    checkFinished()
                }
            }
        
        onDispose {
            approvedListener.remove()
            myListener.remove()
        }
    }

    val allCategories = listOf("All", "Bayan", "Recitation", "Nasheed", "Education", "Hadith")
    var selectedCategory by remember { mutableStateOf("All") }

    val combinedVideos = remember(userVideosList.toList(), selectedCategory, currentUserId) {
        val baseList = if (currentUserId.isEmpty()) {
             // For guest users, show demo feed
             demoVideos.map { it.copy(status = "APPROVED") }
        } else {
             userVideosList.filter { it.status == "APPROVED" }.map { uv ->
                val playUrl = if (uv.telegramFileId.isNotEmpty() && !TELEGRAM_PROXY_URL.contains("YOUR_SCRIPT_ID")) {
                    "$TELEGRAM_PROXY_URL?action=stream&file_id=${uv.telegramFileId}"
                } else if (uv.videoUri.startsWith("http")) {
                    uv.videoUri
                } else {
                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
                }
                VideoItem(
                    id = uv.docId?.hashCode() ?: java.util.UUID.randomUUID().hashCode(),
                    url = playUrl ?: "",
                    author = uv.author ?: "Unknown Author",
                    description = uv.description ?: "",
                    category = uv.category ?: "সাধারণ",
                    status = uv.status ?: "PENDING",
                    docId = uv.docId ?: "",
                    userId = uv.userId ?: "",
                    isOfflineMode = uv.isOfflineMode ?: false,
                    telegramFileId = uv.telegramFileId ?: "",
                    viewsCount = uv.viewsCount ?: 0L,
                    likedBy = uv.likedBy ?: emptyList(),
                    sharesCount = uv.sharesCount ?: 0L,
                    title = uv.title ?: "",
                    videoUri = uv.videoUri ?: "",
                    thumbnailUrl = uv.thumbnailUrl ?: ""
                )
             }
        }

        if (selectedCategory == "All") baseList 
        else baseList.filter { it.category == selectedCategory }
    }

    // Load More Function for Infinite Scroll
    fun loadMoreVideos() {
        if (currentUserId.isEmpty() || isFetchingMore || lastVisibleDoc == null) return
        
        isFetchingMore = true
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        firestore.collection("videos")
            .whereEqualTo("status", "APPROVED")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .startAfter(lastVisibleDoc!!)
            .limit(10)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots != null && !snapshots.isEmpty) {
                    val list = mutableListOf<UserUploadedVideo>()
                    for (doc in snapshots) {
                        doc.toObject(UserUploadedVideo::class.java)?.copy(docId = doc.id)?.let { list.add(it) }
                    }
                    approvedVideos = (approvedVideos + list).distinctBy { it.docId }
                    lastVisibleDoc = snapshots.documents[snapshots.size() - 1]
                }
                isFetchingMore = false
            }
            .addOnFailureListener {
                isFetchingMore = false
            }
    }

    var playingVideoId by remember { 
        mutableStateOf(null as String?) 
    }

    // Lazy List state to track visible items and handle autoplay on scroll
    val lazyListState = rememberLazyListState()

    // Lazy scroll tracking algorithm: updates active playing ID to the one closest to middle of viewport
    LaunchedEffect(lazyListState, combinedVideos) {
        snapshotFlow { lazyListState.layoutInfo }
            .collect { layoutInfo ->
                val visibleItems = layoutInfo.visibleItemsInfo
                if (visibleItems.isNotEmpty() && combinedVideos.isNotEmpty()) {
                    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                    val centerLine = viewportHeight / 2
                    
                    val bestItem = visibleItems.minByOrNull { item ->
                        val itemCenter = item.offset + (item.size / 2)
                        kotlin.math.abs(itemCenter - centerLine)
                    }
                    
                    bestItem?.let { item ->
                        val videoIndex = item.index
                        if (videoIndex in combinedVideos.indices) {
                            val activeVideo = combinedVideos[videoIndex]
                            if (playingVideoId != activeVideo.docId) {
                                playingVideoId = activeVideo.docId
                            }
                        }
                    }
                } else if (combinedVideos.isNotEmpty() && playingVideoId == null) {
                    playingVideoId = combinedVideos.firstOrNull()?.docId
                }
            }
    }

    // Track seen unique non-offline videos matching Facebook active play behavior
    LaunchedEffect(playingVideoId, combinedVideos) {
        val activeVideo = combinedVideos.find { it.docId == playingVideoId }
        if (activeVideo != null && !activeVideo.isOfflineMode && activeVideo.docId.isNotEmpty()) {
            if (!viewedNonOfflineVideos.contains(activeVideo.docId)) {
                viewedNonOfflineVideos.add(activeVideo.docId)
            }
        }
    }
    
    val suggestedProfiles = remember { mutableStateListOf<SuggestedProfile>() }

    LaunchedEffect(currentUserId) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("users")
            .limit(35)
            .get()
            .addOnSuccessListener { snapshots ->
                if (snapshots != null) {
                    val list = snapshots.documents.mapNotNull { doc ->
                        val id = doc.id
                        if (id == currentUserId) return@mapNotNull null
                        val name = doc.getString("name") ?: "Unknown"
                        val profileImageUrl = doc.getString("profileImageUrl") ?: ""
                        SuggestedProfile(
                            id = id,
                            name = name,
                            profileImageUrl = profileImageUrl,
                            mutualFriendsCount = 0
                        )
                    }
                    suggestedProfiles.clear()
                    suggestedProfiles.addAll(list.take(20))
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("Firebase", "Failed to fetch users", e)
            }
    }

    // Preloading improvement: use beyondViewportPageCount for smoother transitions
    val pagerState = rememberPagerState(pageCount = { combinedVideos.size })

    val mainBgColor = Color(0xFFF0F2F5) // Restored Light grey background for feed
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().background(mainBgColor)) {
        // Global FIXED Top Icons
        VideoTopIcons(
            activeSubTab = activeSubTab,
            onActiveSubTabChange = onActiveSubTabChange,
            selectedCategory = selectedCategory,
            allCategories = allCategories,
            onCategoryChange = { selectedCategory = it },
            isLoading = isVideosLoading,
            onNavigateToFriends = onNavigateToFriends
        )

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (activeSubTab) {
                "home" -> {
                    // Fallback to demo videos if network fetch is empty to avoid blank screen
                    val displayList = remember(isVideosLoading, combinedVideos) {
                        if (!isVideosLoading && combinedVideos.isEmpty()) {
                            demoVideos.map { it.copy(status = "APPROVED") }
                        } else {
                            // Ensure the feed is mixed and doesn't prioritize only self videos
                            combinedVideos
                        }
                    }

                    if (isVideosLoading && displayList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = PrimaryGreen)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Loading Feed..." else "ফিড লোড হচ্ছে...", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    } else if (displayList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "No videos available. Tap to refresh." else "কোনো ভিডিও পাওয়া যায়নি। রিফ্রেশ করতে ট্যাপ করুন।", 
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { isVideosLoading = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Text("রিফ্রেশ করুন")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize().background(Color(0xFFF0F2F5)),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            itemsIndexed(displayList, key = { _, video -> video.docId }) { index, video ->
                                val isVisible = playingVideoId == video.docId && isFeedActive
                                
                                // Infinite Scroll Trigger
                                if (index >= displayList.size - 3 && !isFetchingMore) {
                                    LaunchedEffect(Unit) { loadMoreVideos() }
                                }

                                FacebookVideoPostCard(
                                    video = video,
                                    isPlaying = isVisible,
                                    onPlayClick = {
                                        playingVideoId = if (playingVideoId == video.docId) null else video.docId
                                    },
                                    isLimitExceeded = false, // Handled internally now
                                    onRequireLogin = onRequireLogin,
                                    onNavigateToCreatorProfile = onNavigateToCreatorProfile,
                                    onNavigateToSaved = onNavigateToSaved,
                                    dismissedPendingBanners = dismissedPendingBanners
                                )
                            }

                            if (isFetchingMore) {
                                item { 
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(24.dp))
                                    }
                                }
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
        }
    }
}

private var isGlobalMuted by androidx.compose.runtime.mutableStateOf(false)

private val globalOkHttpClient = okhttp3.OkHttpClient.Builder()
    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
    .build()

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoItem: VideoItem, 
    isSelected: Boolean, 
    modifier: Modifier = Modifier.fillMaxSize(),
    onReadyChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isUserPaused by remember(videoItem.id, isSelected) { mutableStateOf(false) }
    
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
                    val botToken = "8968904429:AAE3Ce849ysMuaxQhdMebsBwyB_nlIPQ1Os"
                    val request = okhttp3.Request.Builder()
                        .url("https://api.telegram.org/bot$botToken/getFile?file_id=$fileId")
                        .build()
                    globalOkHttpClient.newCall(request).execute().use { response ->
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
                    val request = okhttp3.Request.Builder().url(videoItem.url).build()
                    globalOkHttpClient.newCall(request).execute().use { response ->
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

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var isReady by remember(resolvedUrl) { mutableStateOf(false) }
    
    LaunchedEffect(isReady) {
        onReadyChange(isReady)
    }

    DisposableEffect(resolvedUrl, isSelected) {
        if (resolvedUrl.isEmpty() || !isSelected) {
            isReady = false
            return@DisposableEffect onDispose {}
        }
        
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(resolvedUrl)))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            volume = if (isGlobalMuted) 0f else 1f
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isReady = playbackState == Player.STATE_READY
                }
            })
            prepare()
        }
        exoPlayer = player

        onDispose {
            player.release()
            exoPlayer = null
            isReady = false
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

    LaunchedEffect(isGlobalMuted) {
        exoPlayer?.volume = if (isGlobalMuted) 0f else 1f
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                exoPlayer?.pause()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (shouldPlay) {
                    exoPlayer?.play()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
        // Immediate Thumbnail Placeholder to eliminate Black Screen
        if (videoItem.thumbnailUrl.isNotEmpty() || getYoutubeThumbnail(videoItem.videoUri).isNotEmpty()) {
            val imageUrl = if (videoItem.thumbnailUrl.isNotEmpty()) videoItem.thumbnailUrl else getYoutubeThumbnail(videoItem.videoUri)
            coil.compose.AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = { playerView ->
                if (playerView.player != exoPlayer) {
                    playerView.player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Detailed Loading State on top of thumbnail
        androidx.compose.animation.AnimatedVisibility(
            visible = !isReady && isSelected,
            exit = androidx.compose.animation.fadeOut(animationSpec = tween(400))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)), 
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(36.dp))
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
                    .addSnapshotListener { doc, _ -> isFollowed = doc?.exists() == true }
            } catch (e: Exception) {
                android.util.Log.e("VideoOverlay", "Error checking follow state", e)
            }
        }
    }

    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("ভুল তথ্য") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        // TOP OVERLAY: Profile and Follow
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clickable { onNavigateToCreatorProfile(videoItem.userId, videoItem.author) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileLogoDisplay(
                modifier = Modifier.size(32.dp),
                userId = videoItem.userId,
                iconSizeDp = 18,
                showBorder = false
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = videoItem.author,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!isFollowed && videoItem.userId != currentUserId && videoItem.userId.isNotEmpty()) {
                    Text(
                        text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Follow" else "ফলো করুন",
                        color = PrimaryGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            if (currentUserId.isEmpty()) {
                                Toast.makeText(context, "লগইন করুন!", Toast.LENGTH_SHORT).show()
                            } else {
                                isFollowed = true
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                                val currentUser = auth.currentUser
                                db.collection("follows").document("${currentUserId}_${videoItem.userId}")
                                    .set(mapOf("followerId" to currentUserId, "creatorId" to videoItem.userId))
                                    .addOnSuccessListener {
                                        db.collection("remote_notifications").add(mapOf(
                                            "toId" to videoItem.userId,
                                            "actorId" to currentUserId,
                                            "actorName" to (currentUser?.displayName ?: "Someone"),
                                            "type" to "FOLLOW",
                                            "title" to "New Follower",
                                            "body" to "${currentUser?.displayName ?: "Someone"} has followed you. You can choose to follow back.",
                                            "timestamp" to System.currentTimeMillis(),
                                            "isRead" to false
                                        ))
                                    }
                            }
                        }
                    )
                }
            }
        }

        // BOTTOM OVERLAY: Title, Actions
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                    )
                )
                .padding(16.dp)
        ) {
            if (videoItem.title.isNotEmpty()) {
                Text(
                    text = videoItem.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Like Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            if (currentUserId.isEmpty()) {
                                Toast.makeText(context, "লগইন করুন!", Toast.LENGTH_SHORT).show()
                            } else if (videoItem.docId.isNotEmpty()) {
                                isLiked = !isLiked
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val videoRef = db.collection("videos").document(videoItem.docId)
                                if (isLiked) videoRef.update("likedBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
                                else videoRef.update("likedBy", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFF1877F2) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Like" else "লাইক", color = Color.White, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    // Comment Action
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comment",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Comment" else "মন্তব্য", color = Color.White, fontSize = 10.sp)
                    }

                    Spacer(modifier = Modifier.width(28.dp))

                    // Share Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            Toast.makeText(context, "লিংক কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Share" else "শেয়ার", color = Color.White, fontSize = 10.sp)
                    }
                }
                
                IconButton(onClick = { isGlobalMuted = !isGlobalMuted }) {
                    Icon(
                        imageVector = if (isGlobalMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 1.dp) // Sleeker top/bottom container padding
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoading || allCategories.isEmpty()) {
                items(5) {
                    ShimmerPlaceholder(
                        modifier = Modifier
                            .width(80.dp)
                            .height(30.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            } else {
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), // Thinner padding
                            fontSize = 13.sp, // Slimmer elegant font size
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoTopIcons(
    activeSubTab: String,
    onActiveSubTabChange: (String) -> Unit,
    selectedCategory: String = "All",
    allCategories: List<String> = emptyList(),
    onCategoryChange: (String) -> Unit = {},
    isLoading: Boolean = false,
    onNavigateToFriends: () -> Unit = {}
) {
    val isDarkTheme = false
    val backgroundModifier = Modifier.background(Color.White)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = androidx.compose.ui.graphics.RectangleShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(backgroundModifier)
                .padding(top = 0.dp, bottom = 0.dp)
        ) {
            // First Row: Halal Circle Brand Heading in English
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp), // Reduce padding
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
                IconButton(onClick = onNavigateToFriends) {
                    Icon(
                        imageVector = Icons.Default.Search, 
                        contentDescription = "Search", 
                        tint = Color.Black
                    )
                }
            }

            HorizontalDivider(
                color = Color(0xFFF0F2F5),
                thickness = 1.dp
            )

            // Second Row: Navigation Icons Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 1.dp), // Thinner vertical space
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
                    icon = Icons.Default.Analytics,
                    label = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Analytics" else "অ্যানালিটিক্স",
                    isSelected = activeSubTab == "analytics",
                    isDarkTheme = isDarkTheme,
                    onClick = { onActiveSubTabChange("analytics") }
                )
                Spacer(modifier = Modifier.weight(1.8f)) // Gap between Analytics and Tools!
                IconSubTabButton(
                    icon = Icons.Default.AutoAwesome,
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

            if (activeSubTab == "home") {
                HorizontalDivider(
                    color = Color(0xFFF0F2F5),
                    thickness = 1.dp
                )
                VideoCategoryList(
                    selectedCategory = selectedCategory,
                    allCategories = allCategories,
                    onCategoryChange = onCategoryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White),
                    isLoading = isLoading
                )
                HorizontalDivider(
                    color = Color(0xFFF0F2F5),
                    thickness = 8.dp
                )
            } else {
                HorizontalDivider(
                    color = Color(0xFFE4E6EB),
                    thickness = 1.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var isSaved by remember { mutableStateOf(false) }
    
    val savedPostDao = remember { TrackerDatabase.getDatabase(context).savedPostDao() }

    LaunchedEffect(video.docId) {
        if (video.docId.isNotEmpty()) {
            val saved = savedPostDao.getPostById(video.docId)
            isSaved = saved != null
        }
    }
    
    var showOptionsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // View Increment Logic
    LaunchedEffect(isPlaying) {
        if (isPlaying && video.docId.isNotEmpty()) {
            delay(4000) // Count as view after 4 seconds of continuous playing (Standard watch threshold)
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("videos").document(video.docId)
                    .update("viewsCount", com.google.firebase.firestore.FieldValue.increment(1))
            } catch (e: Exception) {
                android.util.Log.e("FacebookVideoPostCard", "Error incrementing views", e)
            }
        }
    }

    LaunchedEffect(currentUserId, video.userId) {
        if (currentUserId.isNotEmpty() && video.userId.isNotEmpty()) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                db.collection("follows").document("${currentUserId}_${video.userId}")
                    .addSnapshotListener { doc, _ -> isFollowed = doc?.exists() == true }
            } catch (e: Exception) {
                android.util.Log.e("FacebookVideoPostCard", "Error follow state", e)
            }
        }
    }

    var showReportDialog by remember { mutableStateOf(false) }
    var selectedReportReason by remember { mutableStateOf("ভুল তথ্য") }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = androidx.compose.ui.graphics.RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            // Header: Profile Info - ALWAYS ON WHITE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileLogoDisplay(
                    modifier = Modifier
                        .size(42.dp)
                        .clickable { onNavigateToCreatorProfile(video.userId, video.author) },
                    userId = video.userId,
                    iconSizeDp = 24,
                    showBorder = true
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = video.author,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (!isFollowed && video.userId != currentUserId && video.userId.isNotEmpty()) {
                            Text(text = " • ", color = Color.Gray, fontSize = 12.sp)
                            Text(
                                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Follow" else "ফলো করুন",
                                color = Color(0xFF1877F2),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    if (currentUserId.isEmpty()) {
                                        Toast.makeText(context, "লগইন করুন!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        isFollowed = true
                                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                        db.collection("follows")
                                            .document("${currentUserId}_${video.userId}")
                                            .set(mapOf("followerId" to currentUserId, "creatorId" to video.userId))
                                            .addOnSuccessListener {
                                                // Send Follow Notification
                                                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                                                val currentUser = auth.currentUser
                                                db.collection("remote_notifications").add(mapOf(
                                                    "toId" to video.userId,
                                                    "actorId" to currentUserId,
                                                    "actorName" to (currentUser?.displayName ?: "Someone"),
                                                    "type" to "FOLLOW",
                                                    "title" to "New Follower",
                                                    "body" to "${currentUser?.displayName ?: "Someone"} has followed you. You can choose to follow back.",
                                                    "timestamp" to System.currentTimeMillis(),
                                                    "isRead" to false
                                                ))
                                            }
                                    }
                                }
                            )
                        }
                    }
                    Text(text = video.category, color = Color.Gray, fontSize = 12.sp)
                }

                var isDownloadingLocal by remember { mutableStateOf(false) }

                Box {
                    IconButton(onClick = { showOptionsSheet = true }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            // Title and Description - FIXED: Use unique content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                if (video.title.isNotEmpty()) {
                    Text(
                        text = video.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                }
                if (video.description.isNotEmpty() && video.description != video.title) {
                    Text(
                        text = video.description,
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Video Content Container - ADAPTIVE RATIO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 500.dp)
                    .background(Color.Black)
                    .clip(androidx.compose.ui.graphics.RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isLimitExceeded) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)) // Simple dark overlay
                            .background(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.8f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Login to Watch Full Video" else "🔒 সম্পূর্ণ ভিডিও দেখতে লগইন করুন",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRequireLogin,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(44.dp).fillMaxWidth(0.6f)
                            ) {
                                Text(
                                    if (com.example.viewmodel.GlobalLanguage.isEnglish) "Login Now" else "লগইন করুন",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        VideoPlayer(
                            videoItem = video, 
                            isSelected = isPlaying, 
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        if (!isPlaying) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onPlayClick() }
                                    .background(Color.Black.copy(alpha = 0.1f)), // Subtle tint
                                contentAlignment = Alignment.Center
                            ) {
                                if (video.thumbnailUrl.isNotEmpty() || getYoutubeThumbnail(video.videoUri).isNotEmpty()) {
                                     androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                                         val imageUrl = if (video.thumbnailUrl.isNotEmpty()) video.thumbnailUrl else getYoutubeThumbnail(video.videoUri)
                                         coil.compose.AsyncImage(
                                             model = imageUrl,
                                             contentDescription = null,
                                             modifier = Modifier.fillMaxSize(),
                                             contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                         )
                                         // Play icon overlay
                                         Icon(
                                             imageVector = Icons.Default.PlayArrow,
                                             contentDescription = null,
                                             tint = Color.White.copy(alpha = 0.8f),
                                             modifier = Modifier.size(60.dp).align(Alignment.Center)
                                         )
                                     }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(60.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Small Metrics Info (Likes & Views/Shares) - Facebook Style
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Likes
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val count = video.likedBy.size + if (isLiked && !video.likedBy.contains(currentUserId)) 1 else if (!isLiked && video.likedBy.contains(currentUserId)) -1 else 0
                    if (count > 0) {
                        Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = Color(0xFF1877F2), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = formatNumber(count.toLong()), fontSize = 12.sp, color = Color.Gray)
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Right side: Views & Shares
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "${formatNumber(video.viewsCount)} views", fontSize = 12.sp, color = Color.Gray)
                    
                    if (video.sharesCount > 0) {
                        Text(text = " • ", color = Color.Gray, fontSize = 12.sp)
                        Text(text = "${formatNumber(video.sharesCount)} shares", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFF0F2F5), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 12.dp))

            // Actions Row - Standard Facebook Style on WHITE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Like Action
                TextButton(onClick = {
                    if (currentUserId.isEmpty()) {
                        Toast.makeText(context, "Please login", Toast.LENGTH_SHORT).show()
                    } else {
                        isLiked = !isLiked
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val videoRef = db.collection("videos").document(video.docId)
                        if (isLiked) videoRef.update("likedBy", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
                        else videoRef.update("likedBy", com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                    }
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.ThumbUpAlt else Icons.Outlined.ThumbUpAlt,
                            contentDescription = null,
                            tint = if (isLiked) Color(0xFF1877F2) else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Like" else "লাইক",
                            color = if (isLiked) Color(0xFF1877F2) else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Save Action
                TextButton(onClick = { 
                    scope.launch {
                        if (isSaved) {
                            val saved = savedPostDao.getPostById(video.docId)
                            if (saved != null) {
                                savedPostDao.deletePost(saved)
                                isSaved = false
                                Toast.makeText(context, "Removed from saved posts", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val entity = SavedPost(
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
                            savedPostDao.savePost(entity)
                            isSaved = true
                            Toast.makeText(context, "Saved to your list", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = if (isSaved) Color.Black else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Save" else "সেভ",
                            color = if (isSaved) Color.Black else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Share Action
                TextButton(onClick = {
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("videos").document(video.docId).update("sharesCount", com.google.firebase.firestore.FieldValue.increment(1))
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Share" else "শেয়ার",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Mute/Unmute Icon Only
                IconButton(onClick = { isGlobalMuted = !isGlobalMuted }) {
                    Icon(
                        imageVector = if (isGlobalMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFF0F2F5), thickness = 8.dp)
        }
    }

    if (showOptionsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Options List
                val options = listOf(
                    Triple(Icons.Default.Share, "Share post", {
                        showOptionsSheet = false
                        Toast.makeText(context, "Shared!", Toast.LENGTH_SHORT).show()
                    }),
                    Triple(Icons.Default.Link, "Copy link", {
                        showOptionsSheet = false
                        Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                    }),
                    Triple(Icons.Default.Report, "Report", {
                        showOptionsSheet = false
                        showReportDialog = true
                    }),
                    Triple(Icons.Default.Copyright, "Copyright claim", {
                        showOptionsSheet = false
                        Toast.makeText(context, "Reported for copyright", Toast.LENGTH_SHORT).show()
                    }),
                    Triple(Icons.Default.PersonAdd, if (isFollowed) "Unfollow" else "Follow", {
                        showOptionsSheet = false
                        if (currentUserId.isEmpty()) Toast.makeText(context, "Login required", Toast.LENGTH_SHORT).show()
                        else {
                            val nextFollowState = !isFollowed
                            isFollowed = nextFollowState
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                            val currentUser = auth.currentUser
                            if (nextFollowState) {
                                db.collection("follows").document("${currentUserId}_${video.userId}").set(mapOf("followerId" to currentUserId, "creatorId" to video.userId))
                                    .addOnSuccessListener {
                                        db.collection("remote_notifications").add(mapOf(
                                            "toId" to video.userId,
                                            "actorId" to currentUserId,
                                            "actorName" to (currentUser?.displayName ?: "Someone"),
                                            "type" to "FOLLOW",
                                            "title" to "New Follower",
                                            "body" to "${currentUser?.displayName ?: "Someone"} has followed you. You can choose to follow back.",
                                            "timestamp" to System.currentTimeMillis(),
                                            "isRead" to false
                                        ))
                                    }
                            } else {
                                db.collection("follows").document("${currentUserId}_${video.userId}").delete()
                                    .addOnSuccessListener {
                                        db.collection("remote_notifications").add(mapOf(
                                            "toId" to video.userId,
                                            "actorId" to currentUserId,
                                            "actorName" to (currentUser?.displayName ?: "Someone"),
                                            "type" to "UNFOLLOW",
                                            "title" to "Someone unfollowed you",
                                            "body" to "${currentUser?.displayName ?: "Someone"} has unfollowed you. ধৈর্য ধরুন! প্রতিটি পদক্ষেপে পরীক্ষা থাকে। আপনার ভালো কাজ চালিয়ে যান।",
                                            "timestamp" to System.currentTimeMillis(),
                                            "isRead" to false
                                        ))
                                    }
                            }
                        }
                    }),
                    Triple(Icons.Default.Block, "Block user", {
                        showOptionsSheet = false
                        Toast.makeText(context, "User blocked", Toast.LENGTH_SHORT).show()
                    }),
                    Triple(Icons.Default.VisibilityOff, "Not interested", {
                        showOptionsSheet = false
                        Toast.makeText(context, "Thanks, we'll show fewer posts like this", Toast.LENGTH_SHORT).show()
                    }),
                    Triple(Icons.Default.BookmarkBorder, if (isSaved) "Unsave post" else "Save post", {
                        showOptionsSheet = false
                        scope.launch {
                            if (isSaved) {
                                val saved = savedPostDao.getPostById(video.docId)
                                if (saved != null) {
                                    savedPostDao.deletePost(saved)
                                    isSaved = false
                                    Toast.makeText(context, "Removed from saved posts", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val entity = SavedPost(
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
                                savedPostDao.savePost(entity)
                                isSaved = true
                                Toast.makeText(context, "Saved to your list", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }),
                    Triple(Icons.Default.Download, "Download", {
                        showOptionsSheet = false
                        if (video.url.isNotEmpty()) {
                            try {
                                val request = DownloadManager.Request(Uri.parse(video.url))
                                    .setTitle("Downloading Video")
                                    .setDescription("Saving video from ${video.author}")
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DCIM, "Camera/Hidayah_${video.docId}.mp4")
                                    .setAllowedOverMetered(true)
                                    .setAllowedOverRoaming(true)
                                    .setMimeType("video/mp4")

                                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadManager.enqueue(request)
                                Toast.makeText(context, "Download started. It will appear in Gallery.", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                android.util.Log.e("Download", "Error starting download", e)
                                Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Invalid video URL", Toast.LENGTH_SHORT).show()
                        }
                    })
                )

                options.forEach { (icon, label, action) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { action() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = label, fontSize = 16.sp, color = Color.Black)
                    }
                }
            }
        }
    }

    if (isDownloading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Downloading...") },
            text = { LinearProgressIndicator(progress = { downloadProgress }, color = PrimaryGreen, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {}
        )
    }
}

@Composable
fun ShimmerPlaceholder(
    modifier: Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(4.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 10f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val shimmerColors = listOf(
        Color(0xFFE4E6EB),
        Color(0xFFF0F2F5),
        Color(0xFFE4E6EB),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
    )
}

@Composable
fun VideoPostCardSkeleton() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = androidx.compose.ui.graphics.RectangleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header: Avatar + Author + Category Info
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ShimmerPlaceholder(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerPlaceholder(
                        modifier = Modifier.width(140.dp).height(12.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerPlaceholder(
                        modifier = Modifier.width(90.dp).height(10.dp)
                    )
                }
            }
            // Title and description placeholders
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                ShimmerPlaceholder(
                    modifier = Modifier.fillMaxWidth(0.9f).height(14.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerPlaceholder(
                    modifier = Modifier.fillMaxWidth(0.7f).height(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            // Video placeholder container - YouTube/Facebook Feed aspect ratio
            ShimmerPlaceholder(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.77f),
                shape = androidx.compose.ui.graphics.RectangleShape
            )
            
            // Likes + views row
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    ShimmerPlaceholder(modifier = Modifier.size(16.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(4.dp))
                    ShimmerPlaceholder(modifier = Modifier.width(30.dp).height(10.dp))
                }
                ShimmerPlaceholder(
                    modifier = Modifier.width(80.dp).height(10.dp)
                )
            }
            
            HorizontalDivider(color = Color(0xFFF0F2F5), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 12.dp))
            
            // Bottom action tabs (Like, Share, Save)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                repeat(3) {
                    ShimmerPlaceholder(
                        modifier = Modifier.width(80.dp).height(24.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFF0F2F5), thickness = 8.dp)
        }
    }
}
