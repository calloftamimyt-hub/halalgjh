package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.UserUploadedVideo
import com.example.ui.theme.PrimaryGreen
import com.example.viewmodel.GlobalLanguage
import com.example.viewmodel.toBengali
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity

fun getYoutubeThumbnail(url: String): String? {
    val pattern = "(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/" +
                "|.*[?&]v=)|youtu\\.be\\/)([^\"&?\\/\\s]{11})"
    val regex = Regex(pattern)
    val matchResult = regex.find(url)
    return matchResult?.groupValues?.get(1)?.let { videoId ->
        "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorProfileScreen(
    creatorUid: String,
    creatorName: String,
    onBack: () -> Unit,
    onVideoClick: (UserUploadedVideo) -> Unit
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val view = LocalView.current
    
    // Set status bar icons to dark when this screen is active
    SideEffect {
        val window = (view.context as android.app.Activity).window
        window.statusBarColor = Color.White.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
    }

    var creatorVideos by remember { mutableStateOf<List<UserUploadedVideo>>(emptyList()) }
    var followerCount by remember { mutableIntStateOf(0) }
    var followingCount by remember { mutableIntStateOf(0) }
    var isFollowing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(creatorUid) {
        isLoading = true
        // Fetch creator's videos
        db.collection("videos")
            .whereEqualTo("userId", creatorUid)
            .whereEqualTo("status", "APPROVED")
            .get()
            .addOnSuccessListener { snapshots ->
                creatorVideos = snapshots.mapNotNull { it.toObject(UserUploadedVideo::class.java) }
                isLoading = false
            }
        
        // Fetch follower count
        db.collection("follows")
            .whereEqualTo("creatorId", creatorUid)
            .get()
            .addOnSuccessListener { snapshots ->
                followerCount = snapshots.size()
            }
            
        // Fetch following count (how many people this creator follows)
        db.collection("follows")
            .whereEqualTo("followerId", creatorUid)
            .get()
            .addOnSuccessListener { snapshots ->
                followingCount = snapshots.size()
            }

        // Check if current user follows this creator
        if (currentUser != null) {
            db.collection("follows")
                .document("${currentUser.uid}_$creatorUid")
                .get()
                .addOnSuccessListener { doc ->
                    isFollowing = doc.exists()
                }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(creatorName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF111827)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF111827))
                    }
                },
                actions = {
                    IconButton(onClick = { /* General profile options if needed */ }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color(0xFF111827))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Logo
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(PrimaryGreen.copy(alpha = 0.08f), CircleShape)
                                .border(1.5.dp, PrimaryGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Stats row
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CreatorStatItem(
                                label = if (GlobalLanguage.isEnglish) "Posts" else "পোস্ট",
                                value = creatorVideos.size.toString().toBengali()
                            )
                            CreatorStatItem(
                                label = if (GlobalLanguage.isEnglish) "Followers" else "ফলোয়ার",
                                value = followerCount.toString().toBengali()
                            )
                            CreatorStatItem(
                                label = if (GlobalLanguage.isEnglish) "Following" else "ফলোয়িং",
                                value = followingCount.toString().toBengali()
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Name and Follow Button Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = creatorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF111827),
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Smaller Follow Button with Dotted Border on the right
                        val density = LocalDensity.current
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(34.dp)
                                .drawBehind {
                                    val strokeWidth = 1.dp.toPx()
                                    val dashPath = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    drawRoundRect(
                                        color = if (isFollowing) Color.LightGray else PrimaryGreen,
                                        style = Stroke(width = strokeWidth, pathEffect = dashPath),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                                    )
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (currentUser == null) {
                                        Toast.makeText(context, if (GlobalLanguage.isEnglish) "Please log in to follow" else "ফলো করতে লগইন করুন", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    if (creatorUid == currentUser.uid) {
                                        Toast.makeText(context, if (GlobalLanguage.isEnglish) "You cannot follow yourself" else "আপনি নিজেকে ফলো করতে পারবেন না", Toast.LENGTH_SHORT).show()
                                        return@clickable
                                    }
                                    
                                    val nextFollowing = !isFollowing
                                    isFollowing = nextFollowing
                                    val followDoc = db.collection("follows").document("${currentUser.uid}_$creatorUid")
                                    
                                    if (nextFollowing) {
                                        followDoc.set(mapOf(
                                            "followerId" to currentUser.uid,
                                            "creatorId" to creatorUid,
                                            "timestamp" to System.currentTimeMillis()
                                        )).addOnSuccessListener { followerCount++ }
                                    } else {
                                        followDoc.delete().addOnSuccessListener { followerCount = maxOf(0, followerCount - 1) }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFollowing) (if (GlobalLanguage.isEnglish) "Following" else "ফলো করছেন") 
                                       else (if (GlobalLanguage.isEnglish) "Follow" else "ফলো করুন"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isFollowing) Color.Gray else PrimaryGreen
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Tab line
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(2.5.dp)
                                        .background(PrimaryGreen, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                )
                            }
                        }
                        Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                    }
                    
                    // Video Grid (3 per row)
                    if (creatorVideos.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (GlobalLanguage.isEnglish) "No videos yet" else "কোনো ভিডিও নেই",
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(1.dp),
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(creatorVideos) { video ->
                                CreatorVideoThumbnail(video = video, onClick = { onVideoClick(video) })
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun CreatorStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF111827)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
fun CreatorVideoThumbnail(video: UserUploadedVideo, onClick: () -> Unit) {
    val context = LocalContext.current
    var showMoreOptions by remember { mutableStateOf(false) }

    val youtubeThumb = remember(video.videoUri) { getYoutubeThumbnail(video.videoUri) }
    val displayThumbUrl = if (video.thumbnailUrl.startsWith("http")) video.thumbnailUrl else youtubeThumb

    Box(
        modifier = Modifier
            .aspectRatio(0.85f) // Slightly adjusted for modern look
            .clickable { onClick() }
            .background(Color(0xFFF3F4F6)) 
    ) {
        // Thumbnail Content
        if (!displayThumbUrl.isNullOrEmpty()) {
            AsyncImage(
                model = displayThumbUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Darkness overlay at the top and bottom for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    )
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle, 
                    contentDescription = null, 
                    tint = PrimaryGreen.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.title,
                    fontSize = 10.sp,
                    maxLines = 2,
                    lineHeight = 12.sp,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Views Count Overlay (Bottom Left)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow, 
                contentDescription = null, 
                tint = Color.White, 
                modifier = Modifier.size(12.dp).graphicsLayer(shadowElevation = 2f)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = video.viewsCount.toString().toBengali(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer(shadowElevation = 2f)
            )
        }

        // More Options (Three Dots) Top Right for reporting - Now WHITE and VISIBLE
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
        ) {
            IconButton(
                onClick = { showMoreOptions = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert, 
                    contentDescription = "Options", 
                    tint = Color.White,
                    modifier = Modifier.size(20.dp).graphicsLayer(shadowElevation = 4f)
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
                            Icon(Icons.Default.Flag, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = if (GlobalLanguage.isEnglish) "Report Video" else "ভিডিও রিপোর্ট করুন", fontSize = 14.sp, color = Color.Black)
                        }
                    },
                    onClick = {
                        showMoreOptions = false
                        Toast.makeText(context, if (GlobalLanguage.isEnglish) "Report submitted" else "রিপোর্ট জমা দেওয়া হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
