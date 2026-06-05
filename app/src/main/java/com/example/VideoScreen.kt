package com.example

import android.content.Context
import android.net.Uri
import android.widget.Toast
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
    val userId: String = ""
)

// dummyVideos removed to only show user-uploaded videos

@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoScreen() {
    val context = LocalContext.current
    val view = LocalView.current
    
    // Set status bar to black with white icons
    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = android.graphics.Color.BLACK
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    val userVideosList = remember { mutableStateListOf<UserUploadedVideo>() }
    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val currentUserId = remember { currentUser?.uid ?: "" }
    
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
                            val video = doc.toObject(UserUploadedVideo::class.java)
                            userVideosList.add(video)
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("Firebase", "Firestore not initialized", e)
        }
    }

    val allCategories = if (com.example.viewmodel.GlobalLanguage.isEnglish) {
        listOf("All", "Bayan", "Recitation", "Nasheed", "Education", "Hadith")
    } else {
        listOf("সবগুলো", "বয়ান (Bayan)", "তিলাওয়াত (Recitation)", "নাশিদ (Nasheed)", "শিক্ষা (Education)", "হাদিস (Hadith)")
    }
    var selectedCategory by remember { mutableStateOf(if (com.example.viewmodel.GlobalLanguage.isEnglish) "All" else "সবগুলো") }

    val combinedVideos = remember(userVideosList.toList(), selectedCategory) {
        val mapped = userVideosList.mapIndexed { idx, uv ->
            val playUrl = if (uv.videoUri.startsWith("http") || uv.videoUri.startsWith("content")) {
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
                userId = uv.userId
            )
        }.filter { it.status == "APPROVED" } // Show approved only

        val total = mapped
        if (selectedCategory == "সবগুলো" || selectedCategory == "All") total 
        else total.filter { it.category == selectedCategory }
    }

    val pagerState = rememberPagerState(pageCount = { combinedVideos.size })
    
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (combinedVideos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "No videos found in this category" else "এই ক্যাটাগরিতে কোনো ভিডিও পাওয়া যায়নি", color = Color.White)
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val video = combinedVideos[page]
                Box(modifier = Modifier.fillMaxSize()) {
                    VideoPlayer(videoItem = video, isSelected = pagerState.currentPage == page)
                    VideoOverlay(videoItem = video)
                    
                     if (video.status == "PENDING") {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            // High-end Glassmorphism effect for the Pending overlay
                            Surface(
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .padding(16.dp),
                                shape = RoundedCornerShape(24.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(Color.White.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.HourglassEmpty,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Awaiting Admin Approval" else "এডমিন অ্যাপ্রুভালের অপেক্ষায়...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Your video is private until approved by the community moderator." else "অনুমোদিত না হওয়া পর্যন্ত এই ভিডিওটি শুধুমাত্র আপনি দেখতে পাবেন।",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Search/Category Bar (YouTube-style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allCategories) { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        onClick = { selectedCategory = category },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.15f),
                        border = if (isSelected) null else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = category,
                            color = if (isSelected) Color.Black else Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(videoItem: VideoItem, isSelected: Boolean) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    
    DisposableEffect(Unit) {
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoItem.url)))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
        exoPlayer = player
        onDispose {
            player.release()
        }
    }
    
    LaunchedEffect(isSelected) {
        if (isSelected) {
            exoPlayer?.play()
        } else {
            exoPlayer?.pause()
            exoPlayer?.seekTo(0)
        }
    }
    
    exoPlayer?.let { player ->
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun VideoOverlay(videoItem: VideoItem) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // States for custom interactions
    var isLiked by remember { mutableStateOf(false) }
    var isFollowed by remember { mutableStateOf(false) }
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
                        .background(chosenLogo.second.copy(alpha = 0.25f), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                        .clickable {
                            Toast.makeText(context, "এটি আপনার নির্বাচিত প্রোফাইল অবয়ব!", Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = chosenLogo.first,
                        contentDescription = "প্রোফাইল লোগো",
                        tint = chosenLogo.second,
                        modifier = Modifier.size(24.dp)
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
                            isFollowed = !isFollowed
                            val message = if (isFollowed) "আপনি এখন ${videoItem.author} কে ফলো করছেন!" else "ফলো করা বাতিল করা হয়েছে।"
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
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
                onClick = { isLiked = !isLiked },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color.Red else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = if (isLiked) (if (com.example.viewmodel.GlobalLanguage.isEnglish) "12.6K" else "১২.৬K") else (if (com.example.viewmodel.GlobalLanguage.isEnglish) "12.5K" else "১২.৫K"), 
                color = Color.White, 
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // 3. Share Button
            IconButton(
                onClick = { 
                    Toast.makeText(context, "ভিডিওর লিংকটি ক্লিপবোর্ডে কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
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

            // 4. Report (Flag) Button under Share
            IconButton(
                onClick = { showReportDialog = true },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Flag,
                    contentDescription = "Report",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Report" else "রিপোর্ট", 
                color = Color.White, 
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(14.dp))

            // 5. Download Button under Report
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
