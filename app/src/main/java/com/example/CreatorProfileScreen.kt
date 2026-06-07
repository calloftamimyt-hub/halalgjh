package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity

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
            
        // Fetch following count
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

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Main Header Single Content Card as requested
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 36.dp, bottom = 24.dp, start = 20.dp, end = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Standardized profile logo aligned to top
                            ProfileLogoDisplay(
                                modifier = Modifier.size(90.dp),
                                userId = creatorUid,
                                iconSizeDp = 48,
                                showBorder = false
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Creator Name
                            Text(
                                text = creatorName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = Color(0xFF111827),
                                textAlign = TextAlign.Center
                            )

                            // Creator Handle
                            val creatorHandle = creatorName.lowercase().replace(" ", "")
                            Text(
                                text = "@$creatorHandle",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CreatorStatItem(
                                    label = if (GlobalLanguage.isEnglish) "Posts" else "পোস্ট",
                                    value = creatorVideos.size.toString().toBengali()
                                )
                                Divider(modifier = Modifier.height(20.dp).width(1.dp), color = Color(0xFFE5E7EB))
                                CreatorStatItem(
                                    label = if (GlobalLanguage.isEnglish) "Followers" else "ফলোয়ার",
                                    value = followerCount.toString().toBengali()
                                )
                                Divider(modifier = Modifier.height(20.dp).width(1.dp), color = Color(0xFFE5E7EB))
                                CreatorStatItem(
                                    label = if (GlobalLanguage.isEnglish) "Following" else "ফলোয়িং",
                                    value = followingCount.toString().toBengali()
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Follow Button Row (styled & aligned nicely inside the card)
                            val density = LocalDensity.current
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(44.dp)
                                    .drawBehind {
                                        val strokeWidth = 1.dp.toPx()
                                        val dashPath = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        drawRoundRect(
                                            color = if (isFollowing) Color.LightGray else PrimaryGreen,
                                            style = Stroke(width = strokeWidth, pathEffect = dashPath),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
                                        )
                                    }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isFollowing) Color(0xFFF9FAFB) else Color.White)
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
                                    fontSize = 14.sp,
                                    color = if (isFollowing) Color.Gray else PrimaryGreen
                                )
                            }
                        }
                    }
                }

                // Video List Title
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White
                    ) {
                        Text(
                            text = if (GlobalLanguage.isEnglish) "Videos" else "ভিডিওসমূহ",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                if (creatorVideos.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (GlobalLanguage.isEnglish) "No videos yet" else "কোনো ভিডিও নেই",
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                } else {
                    // Feed Style video items
                    items(creatorVideos, key = { it.docId }) { uv ->
                        val playUrl = if (uv.telegramFileId.isNotEmpty() && !TELEGRAM_PROXY_URL.contains("YOUR_SCRIPT_ID")) {
                            "$TELEGRAM_PROXY_URL?action=stream&file_id=${uv.telegramFileId}"
                        } else if (uv.videoUri.startsWith("http")) {
                            uv.videoUri
                        } else {
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
                        }
                        
                        val videoItem = VideoItem(
                            id = uv.docId.hashCode(),
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

                        FacebookVideoPostCard(
                            video = videoItem,
                            isPlaying = (playingVideoId == uv.docId),
                            onPlayClick = {
                                playingVideoId = if (playingVideoId == uv.docId) null else uv.docId
                            },
                            isLimitExceeded = false,
                            onRequireLogin = { },
                            onNavigateToCreatorProfile = { _, _ -> },
                            onNavigateToSaved = { },
                            dismissedPendingBanners = remember { androidx.compose.runtime.mutableStateMapOf() }
                        )
                    }
                }
            }
        }

        // Floating Overlapping Back Button top left
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(12.dp)
                .background(Color.White.copy(alpha = 0.8f), CircleShape)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF111827))
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

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = categorySelected,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(if (GlobalLanguage.isEnglish) "Category" else "ক্যাটাগরি") },
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = {
                                    IconButton(onClick = { categoryExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryGreen,
                                    focusedLabelColor = PrimaryGreen,
                                    cursorColor = PrimaryGreen
                                )
                            )

                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                categoryList.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            categorySelected = cat
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
                                    isSaving = false
                                    editingVideo = null
                                    Toast.makeText(context, if (GlobalLanguage.isEnglish) "Updated!" else "পরিবর্তন করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    
                                    // Refresh local video modifications
                                    creatorVideos = creatorVideos.map {
                                        if (it.docId == video.docId) {
                                            it.copy(title = titleText, description = descText, category = categorySelected)
                                        } else it
                                    }
                                }
                                .addOnFailureListener {
                                    isSaving = false
                                    Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        enabled = !isSaving
                    ) {
                        Text(if (GlobalLanguage.isEnglish) "Save" else "সংরক্ষণ", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { editingVideo = null },
                        enabled = !isSaving
                    ) {
                        Text(if (GlobalLanguage.isEnglish) "Cancel" else "বাতিল", color = Color(0xFF4B5563))
                    }
                },
                containerColor = Color.White
            )
        }

        // Dialog to confirm delete video submission
        if (deletingVideo != null) {
            val video = deletingVideo!!
            var isDeletingState by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { if (!isDeletingState) deletingVideo = null },
                title = { Text(if (GlobalLanguage.isEnglish) "Confirm Deletion" else "মুছে ফেলার বিষয়ে নিশ্চিত হোন") },
                text = { Text(if (GlobalLanguage.isEnglish) "Are you sure you want to permanently delete this video submission?" else "আপনি কি নিশ্চিতভাবে এই ভিডিওটি চিরতরে ডিলেট কারতে চান?") },
                confirmButton = {
                    Button(
                        onClick = {
                            isDeletingState = true
                            db.collection("videos").document(video.docId)
                                .delete()
                                .addOnSuccessListener {
                                    isDeletingState = false
                                    deletingVideo = null
                                    Toast.makeText(context, if (GlobalLanguage.isEnglish) "Deleted!" else "মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    creatorVideos = creatorVideos.filter { it.docId != video.docId }
                                }
                                .addOnFailureListener {
                                    isDeletingState = false
                                    Toast.makeText(context, "Error deleting video", Toast.LENGTH_SHORT).show()
                                }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        enabled = !isDeletingState
                    ) {
                        Text(if (GlobalLanguage.isEnglish) "Delete" else "ডিলেট", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { deletingVideo = null },
                        enabled = !isDeletingState
                    ) {
                        Text(if (GlobalLanguage.isEnglish) "Cancel" else "বাতিল", color = Color(0xFF4B5563))
                    }
                },
                containerColor = Color.White
            )
        }
    }
}

@Composable
fun CreatorStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = Color(0xFF111827),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = Color(0xFF4B5563),
            fontSize = 11.sp
        )
    }
}

@Composable
fun CreatorVideoGridItem(
    video: UserUploadedVideo,
    videoItem: VideoItem,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    isMyProfile: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                     android.util.Log.e("CreatorVideoGridItem", "Error retrieving frame: ${video.videoUri}", e)
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
            .fillMaxWidth()
            .aspectRatio(0.6f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F172A))
            .border(0.5.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
            .clickable { onPlayToggle() }
    ) {
        if (isPlaying) {
            VideoPlayer(videoItem = videoItem, isSelected = true, modifier = Modifier.fillMaxSize())
        } else {
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
        }

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

        // Standardized drop-down options
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                    .size(28.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = { showMoreOptions = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
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
                                Text(text = if (GlobalLanguage.isEnglish) "Edit" else "পরিবর্তন", fontSize = 14.sp, color = Color.Black)
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
                                Text(text = if (GlobalLanguage.isEnglish) "Delete" else "মুছে ফেলুন", fontSize = 14.sp, color = Color.Red)
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
