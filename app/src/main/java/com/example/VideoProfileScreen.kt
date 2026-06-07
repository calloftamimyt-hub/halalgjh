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

@Composable
fun VideoProfileScreen(
    userVideos: List<UserUploadedVideo>,
    onNavigateToCreate: () -> Unit,
    onNavigateToSaved: () -> Unit = {},
    onRequireLogin: () -> Unit
) {
    val context = LocalContext.current
    val currentUser = FirebaseAuth.getInstance().currentUser
    var playingVideoId by remember { mutableStateOf<String?>(null) }
    
    val myVideosOnly = remember(userVideos, currentUser) {
        val uid = currentUser?.uid ?: ""
        userVideos.filter { it.userId == uid }
    }

    var followerCount by remember { mutableIntStateOf(0) }
    var followingCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        val db = FirebaseFirestore.getInstance()
        
        // Fetch follower count
        db.collection("follows")
            .whereEqualTo("creatorId", uid)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    followerCount = snapshots.size()
                }
            }
            
        // Fetch following count
        db.collection("follows")
            .whereEqualTo("followerId", uid)
            .addSnapshotListener { snapshots, _ ->
                if (snapshots != null) {
                    followingCount = snapshots.size()
                }
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F2F5))
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Redesigned Single Premium Profile Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = androidx.compose.ui.graphics.RectangleShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Logo (Centered / Standardized)
                    ProfileLogoDisplay(
                        modifier = Modifier.size(90.dp),
                        userId = currentUser?.uid ?: "",
                        iconSizeDp = 48,
                        showBorder = false
                    )
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Name (Centered)
                    Text(
                        text = if (currentUser != null) {
                            currentUser.displayName ?: (if (GlobalLanguage.isEnglish) "User" else "ইউজার")
                        } else {
                            if (GlobalLanguage.isEnglish) "Guest User" else "অতিথি ইউজার"
                        },
                        color = Color(0xFF111827),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    if (currentUser != null) {
                        val handleText = (currentUser.displayName ?: "user").lowercase().replace(" ", "")
                        Text(
                            text = "@$handleText",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProfileStatItem(
                            label = if (GlobalLanguage.isEnglish) "Posts" else "পোস্ট",
                            value = myVideosOnly.size.toString().toBengali()
                        )
                        Divider(modifier = Modifier.height(20.dp).width(1.dp), color = Color(0xFFE5E7EB))
                        ProfileStatItem(
                            label = if (GlobalLanguage.isEnglish) "Followers" else "ফলোয়ার",
                            value = followerCount.toString().toBengali()
                        )
                        Divider(modifier = Modifier.height(20.dp).width(1.dp), color = Color(0xFFE5E7EB))
                        ProfileStatItem(
                            label = if (GlobalLanguage.isEnglish) "Following" else "ফলোয়িং",
                            value = followingCount.toString().toBengali()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Guest User Specific Content or CTAs
                    if (currentUser == null) {
                        Text(
                            text = if (GlobalLanguage.isEnglish) "Sign in to upload and manage your videos" else "ভিডিও ছাড়তে এবং পরিচালনা করতে সাইন ইন করুন",
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onRequireLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.85f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(
                                text = if (GlobalLanguage.isEnglish) "Sign Up" else "সাইন আপ করুন",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        // Quick Action Row within Single Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onNavigateToCreate,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (GlobalLanguage.isEnglish) "Upload Video" else "ভিডিও ছাড়ুন",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = onNavigateToSaved,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF9C3)),
                                border = BorderStroke(1.dp, Color(0xFFFDE047)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF854D0E))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (GlobalLanguage.isEnglish) "Saved Posts" else "সেভ করা ভিডিও",
                                    color = Color(0xFF854D0E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section Title for uploads
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White
            ) {
                Text(
                    text = if (GlobalLanguage.isEnglish) "My Uploads" else "আমার আপলোডসমূহ",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (currentUser == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (GlobalLanguage.isEnglish) "Please login to see and manage your videos." else "আপনার আপলোডকৃত ভিডিওগুলোর রেকর্ড দেখতে লগইন করুন।",
                                color = Color(0xFF4B5563),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else if (myVideosOnly.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VideoCameraFront,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (GlobalLanguage.isEnglish) "You haven't uploaded any videos yet." else "আপনি কোনো ভিডিও এখনো আপলোড করেননি।",
                                color = Color(0xFF4B5563),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            // LazyColumn Items representing videos in Feed Style
            items(myVideosOnly, key = { it.docId }) { uv ->
                val playUrl = if (uv.telegramFileId.isNotEmpty() && !TELEGRAM_PROXY_URL.contains("YOUR_SCRIPT_ID")) {
                    "$TELEGRAM_PROXY_URL?action=stream&file_id=${uv.telegramFileId}"
                } else if (uv.videoUri.startsWith("http")) {
                    uv.videoUri
                } else if (uv.videoUri.startsWith("content") && uv.userId == (currentUser?.uid ?: "")) {
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
                    onRequireLogin = onRequireLogin,
                    onNavigateToCreatorProfile = { _, _ -> },
                    onNavigateToSaved = onNavigateToSaved,
                    dismissedPendingBanners = remember { androidx.compose.runtime.mutableStateMapOf() }
                )
            }
        }
    }
}

@Composable
fun ProfileStatItem(label: String, value: String) {
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
fun MyProfileVideoGridItem(
    video: UserUploadedVideo,
    videoItem: VideoItem,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showMoreOptions by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                     android.util.Log.e("MyProfileVideoGridItem", "Error retrieving frame: ${video.videoUri}", e)
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

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(if (GlobalLanguage.isEnglish) "Confirm Deletion" else "মুছে ফেলার বিষয়ে নিশ্চিত হোন") },
            text = { Text(if (GlobalLanguage.isEnglish) "Are you sure you want to permanently delete this video submission?" else "আপনি কি নিশ্চিতভাবে এই ভিডিওটি চিরতরে ডিলেট কারতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDelete()
                    }
                ) {
                    Text(if (GlobalLanguage.isEnglish) "Delete" else "ডিলিট", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(if (GlobalLanguage.isEnglish) "Cancel" else "বাতিল", color = Color(0xFF4B5563))
                }
            },
            containerColor = Color.White,
            titleContentColor = Color(0xFF111827),
            textContentColor = Color(0xFF4B5563)
        )
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

        // Standardized dropdown options
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
                        showDeleteConfirmDialog = true
                    }
                )
            }
        }
    }
}
