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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.model.UserUploadedVideo
import com.example.ui.theme.PrimaryGreen
import com.example.viewmodel.GlobalLanguage
import com.example.viewmodel.toBengali
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.graphics.asImageBitmap

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

    var editingVideo by remember { mutableStateOf<UserUploadedVideo?>(null) }
    var deletingVideo by remember { mutableStateOf<UserUploadedVideo?>(null) }
    var playingVideoId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(creatorUid) {
        isLoading = true
        // Fetch creator's videos based on ownership
        val baseQuery = db.collection("videos").whereEqualTo("userId", creatorUid)
        val finalQuery = if (currentUser != null && creatorUid == currentUser.uid) {
            baseQuery
        } else {
            baseQuery.whereEqualTo("status", "APPROVED")
        }
        
        finalQuery.get()
            .addOnSuccessListener { snapshots ->
                creatorVideos = snapshots.mapNotNull { it.toObject(UserUploadedVideo::class.java) }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
        
        // Fetch follower count
        db.collection("follows")
            .whereEqualTo("creatorId", creatorUid)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    followerCount = snapshots.size()
                }
            }
            
        // Fetch following count (how many people this creator follows)
        db.collection("follows")
            .whereEqualTo("followerId", creatorUid)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    followingCount = snapshots.size()
                }
            }

        // Check if current user follows this creator
        if (currentUser != null) {
            db.collection("follows")
                .document("${currentUser.uid}_$creatorUid")
                .addSnapshotListener { doc, _ ->
                    isFollowing = doc?.exists() == true
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
                                        )).addOnSuccessListener { 
                                            // Send Follow Notification
                                            db.collection("remote_notifications").add(mapOf(
                                                "toId" to creatorUid,
                                                "actorId" to currentUser.uid,
                                                "actorName" to (currentUser.displayName ?: "Someone"),
                                                "type" to "FOLLOW",
                                                "title" to "New Follower",
                                                "body" to "${currentUser.displayName ?: "Someone"} has followed you. You can choose to follow back.",
                                                "timestamp" to System.currentTimeMillis(),
                                                "isRead" to false
                                            ))
                                        }
                                    } else {
                                        followDoc.delete().addOnSuccessListener { 
                                            // Send Unfollow Notification with motivational message
                                            db.collection("remote_notifications").add(mapOf(
                                                "toId" to creatorUid,
                                                "actorId" to currentUser.uid,
                                                "actorName" to (currentUser.displayName ?: "Someone"),
                                                "type" to "UNFOLLOW",
                                                "title" to "Someone unfollowed you",
                                                "body" to "${currentUser.displayName ?: "Someone"} has unfollowed you. ধৈর্য ধরুন! প্রতিটি পদক্ষেপে পরীক্ষা থাকে। আপনার ভালো কাজ চালিয়ে যান।",
                                                "timestamp" to System.currentTimeMillis(),
                                                "isRead" to false
                                            ))
                                        }
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
                    
                    // Video list in feed style
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
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(creatorVideos) { uv ->
                                val playUrl = if (uv.telegramFileId.isNotEmpty() && !TELEGRAM_PROXY_URL.contains("YOUR_SCRIPT_ID")) {
                                    "$TELEGRAM_PROXY_URL?action=stream&file_id=${uv.telegramFileId}"
                                } else if (uv.videoUri.startsWith("http")) {
                                    uv.videoUri
                                } else {
                                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
                                }
                                val videoItem = VideoItem(
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
                                    videoUri = uv.videoUri ?: ""
                                )

                                CreatorProfileVideoPostWrapper(
                                    video = uv,
                                    videoItem = videoItem,
                                    isPlaying = (playingVideoId == uv.docId),
                                    onPlayClick = {
                                        playingVideoId = if (playingVideoId == uv.docId) null else uv.docId
                                    },
                                    isMyProfile = (currentUser != null && creatorUid == currentUser.uid),
                                    onEditClick = { editingVideo = uv },
                                    onDeleteClick = { deletingVideo = uv }
                                )
                            }
                        }
                    }

                    // Dialog to edit video details
                    if (editingVideo != null) {
                        val video = editingVideo!!
                        var titleText by remember { mutableStateOf(video.title) }
                        var descText by remember { mutableStateOf(video.description) }
                        var categorySelected by remember { mutableStateOf(video.category) }
                        var isSaving by remember { mutableStateOf(false) }
                        var categoryExpanded by remember { mutableStateOf(false) }

                        val categoryList = listOf("Bayan", "Recitation", "Nasheed", "Education", "Hadith")

                        AlertDialog(
                            onDismissRequest = { if (!isSaving) editingVideo = null },
                            title = {
                                Text(
                                    text = if (GlobalLanguage.isEnglish) "Edit Video Details" else "ভিডিওর তথ্য পরিবর্তন করুন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF111827)
                                )
                            },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Title field
                                    OutlinedTextField(
                                        value = titleText,
                                        onValueChange = { titleText = it },
                                        label = { Text(if (GlobalLanguage.isEnglish) "Title" else "শিরোনাম") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryGreen,
                                            focusedLabelColor = PrimaryGreen,
                                            cursorColor = PrimaryGreen
                                        ),
                                        singleLine = true
                                    )

                                    // Description field
                                    OutlinedTextField(
                                        value = descText,
                                        onValueChange = { descText = it },
                                        label = { Text(if (GlobalLanguage.isEnglish) "Description" else "বিবরণ") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PrimaryGreen,
                                            focusedLabelColor = PrimaryGreen,
                                            cursorColor = PrimaryGreen
                                        ),
                                        maxLines = 4
                                    )

                                    // Category dropdown field
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = categorySelected,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text(if (GlobalLanguage.isEnglish) "Category" else "ক্যাটাগরি") },
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = {
                                                IconButton(onClick = { categoryExpanded = true }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Category")
                                                }
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryGreen,
                                                focusedLabelColor = PrimaryGreen
                                            )
                                        )

                                        DropdownMenu(
                                            expanded = categoryExpanded,
                                            onDismissRequest = { categoryExpanded = false },
                                            modifier = Modifier.background(Color.White)
                                        ) {
                                            categoryList.forEach { category ->
                                                DropdownMenuItem(
                                                    text = { Text(category) },
                                                    onClick = {
                                                        categorySelected = category
                                                        categoryExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        isSaving = true
                                        val updatedData = mapOf(
                                            "title" to titleText,
                                            "description" to descText,
                                            "category" to categorySelected
                                        )
                                        db.collection("videos").document(video.docId)
                                            .update(updatedData)
                                            .addOnSuccessListener {
                                                creatorVideos = creatorVideos.map { item ->
                                                    if (item.docId == video.docId) {
                                                        item.copy(title = titleText, description = descText, category = categorySelected)
                                                    } else {
                                                        item
                                                    }
                                                }
                                                isSaving = false
                                                Toast.makeText(context, if (GlobalLanguage.isEnglish) "Updated successfully!" else "সাফল্যজনকভাবে আপডেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                editingVideo = null
                                            }
                                            .addOnFailureListener {
                                                isSaving = false
                                                Toast.makeText(context, if (GlobalLanguage.isEnglish) "Failed to update" else "আপডেট করতে ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                                            }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    enabled = !isSaving
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text(if (GlobalLanguage.isEnglish) "Save" else "সংরক্ষণ করুন", color = Color.White)
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { editingVideo = null },
                                    enabled = !isSaving
                                ) {
                                    Text(if (GlobalLanguage.isEnglish) "Cancel" else "বাতিল", color = Color.Gray)
                                }
                            },
                            containerColor = Color.White
                        )
                    }

                    // Dialog to confirm deletion of video
                    if (deletingVideo != null) {
                        val video = deletingVideo!!
                        var isDeleting by remember { mutableStateOf(false) }

                        AlertDialog(
                            onDismissRequest = { if (!isDeleting) deletingVideo = null },
                            title = {
                                Text(
                                    text = if (GlobalLanguage.isEnglish) "Confirm Deletion" else "মুছে ফেলার বিষয়ে নিশ্চিত হোন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color(0xFF111827)
                                )
                            },
                            text = {
                                Text(
                                    text = if (GlobalLanguage.isEnglish) "Are you sure you want to permanently delete this video?" else "আপনি কি নিশ্চিতভাবে এই ভিডিওটি চিরতরে ডিলেট কারতে চান?",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        isDeleting = true
                                        db.collection("videos").document(video.docId)
                                            .delete()
                                            .addOnSuccessListener {
                                                creatorVideos = creatorVideos.filter { it.docId != video.docId }
                                                isDeleting = false
                                                Toast.makeText(context, if (GlobalLanguage.isEnglish) "Video deleted!" else "ভিডিওটি মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                deletingVideo = null
                                            }
                                            .addOnFailureListener {
                                                isDeleting = false
                                                Toast.makeText(context, if (GlobalLanguage.isEnglish) "Failed to delete" else "মুছে ফেলতে ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                                            }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    enabled = !isDeleting
                                ) {
                                    if (isDeleting) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text(if (GlobalLanguage.isEnglish) "Delete" else "ডিলেট", color = Color.White)
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { deletingVideo = null },
                                    enabled = !isDeleting
                                ) {
                                    Text(if (GlobalLanguage.isEnglish) "Cancel" else "বাতিল", color = Color.Gray)
                                }
                            },
                            containerColor = Color.White
                        )
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
fun CreatorVideoThumbnail(
    video: UserUploadedVideo,
    isMyProfile: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var showMoreOptions by remember { mutableStateOf(false) }

    val youtubeThumb = remember(video.videoUri) { getYoutubeThumbnail(video.videoUri) }
    val displayThumbUrl = if (video.thumbnailUrl.startsWith("http")) video.thumbnailUrl else youtubeThumb

    var videoFrameBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(video.videoUri, displayThumbUrl) {
         if (displayThumbUrl.isNullOrEmpty() && video.videoUri.isNotEmpty()) {
             kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                 var retriever: android.media.MediaMetadataRetriever? = null
                 try {
                     retriever = android.media.MediaMetadataRetriever()
                     if (video.videoUri.startsWith("http://") || video.videoUri.startsWith("https://")) {
                         retriever.setDataSource(video.videoUri, HashMap<String, String>())
                     } else {
                         val parsedUri = android.net.Uri.parse(video.videoUri)
                         retriever.setDataSource(context, parsedUri)
                     }
                     val bitmap = retriever.getFrameAtTime(1000000, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                     if (bitmap != null) {
                         videoFrameBitmap = bitmap
                     }
                 } catch (e: Exception) {
                     android.util.Log.e("CreatorVideoThumbnail", "Error getting frame from video: ${video.videoUri}", e)
                 } finally {
                     try {
                         retriever?.release()
                     } catch (e: Exception) {
                         e.printStackTrace()
                     }
                 }
             }
         }
    }

    Box(
        modifier = Modifier
            .aspectRatio(0.6f) // YouTube Shorts vertical ratio
            .clickable { onClick() }
            .background(Color(0xFF0F172A))
            .border(0.5.dp, Color.White.copy(alpha = 0.1f))
    ) {
        // Thumbnail Content
        if (!displayThumbUrl.isNullOrEmpty()) {
            AsyncImage(
                model = displayThumbUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else if (videoFrameBitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = videoFrameBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Gradient fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Darkness overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        ),
                        startY = 300f
                    )
                )
        )

        // Views Count Overlay (Bottom Center/Left)
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
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = video.viewsCount.toString().toBengali(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // More Options (Three Dots) Top Right for reporting/editing/deleting
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    .size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { showMoreOptions = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert, 
                        tint = Color.White,
                        contentDescription = "Options", 
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showMoreOptions,
                onDismissRequest = { showMoreOptions = false },
                modifier = Modifier.background(Color.White, RoundedCornerShape(12.dp))
            ) {
                if (isMyProfile) {
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = if (GlobalLanguage.isEnglish) "Edit Details" else "তথ্য পরিবর্তন করুন", fontSize = 14.sp, color = Color.Black)
                            }
                        },
                        onClick = {
                            showMoreOptions = false
                            onEditClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = if (GlobalLanguage.isEnglish) "Delete Video" else "ভিডিওটি মুছে ফেলুন", fontSize = 14.sp, color = Color.Black)
                            }
                        },
                        onClick = {
                            showMoreOptions = false
                            onDeleteClick()
                        }
                    )
                } else {
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
}

@Composable
fun CreatorProfileVideoPostWrapper(
    video: UserUploadedVideo,
    videoItem: VideoItem,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    isMyProfile: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        if (isMyProfile) {
            val statusColor = when (video.status.trim().uppercase()) {
                "APPROVED" -> PrimaryGreen
                "REJECTED" -> Color.Red
                else -> Color(0xFFF59E0B)
            }
            val statusBg = statusColor.copy(alpha = 0.08f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusBg)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusText = when (video.status.trim().uppercase()) {
                    "APPROVED" -> if (GlobalLanguage.isEnglish) "Approved" else "অনুমোদিত"
                    "REJECTED" -> if (GlobalLanguage.isEnglish) "Rejected" else "প্রত্যাখ্যাত"
                    else -> if (GlobalLanguage.isEnglish) "Pending Review" else "অনুমোদনের অপেক্ষায়"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(statusColor, androidx.compose.foundation.shape.CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (GlobalLanguage.isEnglish) "Edit" else "তথ্য পরিবর্তন",
                        color = Color(0xFF1D4ED8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onEditClick() }
                    )
                    Text(
                        text = if (GlobalLanguage.isEnglish) "Delete" else "মুছে ফেলুন",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onDeleteClick() }
                    )
                }
            }
        }
        
        FacebookVideoPostCard(
            video = videoItem,
            isPlaying = isPlaying,
            onPlayClick = onPlayClick,
            isLimitExceeded = false,
            onRequireLogin = {},
            onNavigateToCreatorProfile = { _, _ -> },
            onNavigateToSaved = {},
            dismissedPendingBanners = remember { androidx.compose.runtime.mutableStateMapOf() }
        )
    }
}
