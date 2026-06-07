package com.example

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserUploadedVideo
import com.example.ui.theme.PrimaryGreen
import com.example.viewmodel.GlobalLanguage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
    onBack: () -> Unit,
    onVideoClick: (UserUploadedVideo) -> Unit
) {
    val context = LocalContext.current
    val trackerDb = remember { com.example.database.TrackerDatabase.getDatabase(context) }
    val savedPostDao = remember { trackerDb.savedPostDao() }
    val savedPostsFlow = remember { savedPostDao.getAllSavedPosts() }
    val savedPosts by savedPostsFlow.collectAsState(initial = emptyList())
    
    val savedVideos = remember(savedPosts) {
        savedPosts.map { post ->
            UserUploadedVideo(
                docId = post.docId,
                author = post.author,
                description = post.description,
                category = post.category,
                status = post.status,
                userId = post.userId,
                telegramFileId = post.telegramFileId,
                viewsCount = post.viewsCount,
                sharesCount = post.sharesCount,
                title = post.title,
                videoUri = post.videoUri
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = if (GlobalLanguage.isEnglish) "Saved Posts" else "সেভ করা ভিডিওসমূহ", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp, 
                        color = Color(0xFF111827) 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF111827))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (savedVideos.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (GlobalLanguage.isEnglish) "No saved posts yet" else "এখনো কোনো ভিডিও সেভ করা হয়নি",
                        color = Color.Gray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (GlobalLanguage.isEnglish) "Bookmark videos to see them here" else "ভিডিও বুকমার্ক করলে এখানে দেখতে পাবেন",
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 12.sp
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
                    items(savedVideos) { video ->
                        val playUrl = if (video.telegramFileId.isNotEmpty() && !TELEGRAM_PROXY_URL.contains("YOUR_SCRIPT_ID")) {
                            "$TELEGRAM_PROXY_URL?action=stream&file_id=${video.telegramFileId}"
                        } else if (video.videoUri.startsWith("http")) {
                            video.videoUri
                        } else {
                            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"
                        }
                        
                        val videoItem = VideoItem(
                            id = video.docId.hashCode(),
                            url = playUrl,
                            author = video.author,
                            description = video.description,
                            category = video.category,
                            status = video.status,
                            docId = video.docId,
                            userId = video.userId,
                            viewsCount = video.viewsCount,
                            likedBy = video.likedBy,
                            sharesCount = video.sharesCount,
                            title = video.title,
                            videoUri = video.videoUri
                        )
                        CreatorVideoGridItem(
                            video = video,
                            videoItem = videoItem,
                            isPlaying = false,
                            onPlayToggle = { onVideoClick(video) },
                            isMyProfile = false,
                            onEditClick = {},
                            onDeleteClick = {}
                        )
                    }
                }
            }
        }
    }
}
