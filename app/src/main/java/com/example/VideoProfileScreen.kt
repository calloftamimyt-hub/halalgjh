package com.example

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserUploadedVideo
import com.example.ui.theme.PrimaryGreen
import com.example.viewmodel.GlobalLanguage
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
    
    val myVideosOnly = remember(userVideos, currentUser) {
        val uid = currentUser?.uid ?: ""
        userVideos.filter { it.userId == uid }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(top = 90.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Redesigned Creator Profile Header Card
        item {
            Surface(
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Logo (Centered)
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(PrimaryGreen.copy(alpha = 0.1f), CircleShape)
                            .border(2.dp, PrimaryGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
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

                    // Guest User Specific Content
                    if (currentUser == null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (GlobalLanguage.isEnglish) "You have no shame?" else "তোমার লজ্জা নেই",
                            color = Color(0xFFEF4444),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onRequireLogin,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.8f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Text(
                                text = if (GlobalLanguage.isEnglish) "Sign Up" else "সাইন আপ করুন",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Saved Posts Section (New)
        item {
            Surface(
                onClick = onNavigateToSaved,
                color = Color(0xFFFEF9C3), // Light yellow background
                border = BorderStroke(1.dp, Color(0xFFFDE047)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = Color(0xFF854D0E), // Brownish yellow
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (GlobalLanguage.isEnglish) "Saved Posts" else "সেভ করা ভিডিওসমূহ",
                            color = Color(0xFF854D0E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (GlobalLanguage.isEnglish) "Watch videos you've bookmarked" else "আপনার বুকমার্ক করা ভিডিওগুলো দেখুন",
                            color = Color(0xFF854D0E).copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF854D0E)
                    )
                }
            }
        }

        // Action CTA row for logged in users
        if (currentUser != null) {
            item {
                Button(
                    onClick = onNavigateToCreate,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (GlobalLanguage.isEnglish) "Upload New Video" else "নতুন ভিডিও ছাড়ুন",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        item {
            Text(
                text = if (GlobalLanguage.isEnglish) "My Upload Statuses" else "আমার আপলোড করা ভিডিওসমূহ",
                color = Color(0xFF111827),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (currentUser == null) {
            item {
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
                            text = if (GlobalLanguage.isEnglish) "Please login to see and manage your videos." else "আপনার উলোডকৃত ভিডিওগুলোর রেকর্ড দেখতে লগইন করুন।",
                            color = Color(0xFF4B5563),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (myVideosOnly.isEmpty()) {
            item {
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

        items(myVideosOnly, key = { it.docId }) { video ->
            MyVideoStatusRow(
                video = video,
                onDelete = {
                    try {
                        FirebaseFirestore.getInstance().collection("videos")
                            .document(video.docId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, if (GlobalLanguage.isEnglish) "Video deleted!" else "ভিডিও ডিলেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error deleting video", Toast.LENGTH_SHORT).show()
                            }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error connecting to Firestore", Toast.LENGTH_SHORT).show()
                    }
                }
            )
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
fun MyVideoStatusRow(video: UserUploadedVideo, onDelete: () -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (GlobalLanguage.isEnglish) "Confirm Deletion" else "মুছে ফেলার বিষয়ে নিশ্চিত হোন") },
            text = { Text(if (GlobalLanguage.isEnglish) "Are you sure you want to permanently delete this video submission?" else "আপনি কি নিশ্চিতভাবে এই ভিডিওটি চিরতরে ডিলেট কারতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onDelete()
                    }
                ) {
                    Text(if (GlobalLanguage.isEnglish) "Delete" else "ডিলেট", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(if (GlobalLanguage.isEnglish) "Cancel" else "বাতিল", color = Color(0xFF4B5563))
                }
            },
            containerColor = Color.White,
            titleContentColor = Color(0xFF111827),
            textContentColor = Color(0xFF4B5563)
        )
    }

    Surface(
        color = Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon depicting video
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (video.title.isBlank()) "Untitled" else video.title,
                    color = Color(0xFF111827),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (video.description.isBlank()) "No description" else video.description,
                    color = Color(0xFF4B5563),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Custom Status labels
                val statusConfig = when (video.status.trim().uppercase()) {
                    "APPROVED" -> Triple(
                        if (GlobalLanguage.isEnglish) "Approved" else "অনুমোদিত",
                        PrimaryGreen,
                        PrimaryGreen.copy(alpha = 0.12f)
                    )
                    "REJECTED" -> Triple(
                        if (GlobalLanguage.isEnglish) "Rejected" else "প্রত্যাখ্যাত",
                        Color.Red,
                        Color.Red.copy(alpha = 0.12f)
                    )
                    else -> Triple(
                        if (GlobalLanguage.isEnglish) "Pending Review" else "অপেক্ষমান",
                        Color(0xFFF59E0B),
                        Color(0xFFF59E0B).copy(alpha = 0.12f)
                    )
                }

                Surface(
                    color = statusConfig.third,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = statusConfig.first,
                        color = statusConfig.second,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
            }
        }
    }
}
