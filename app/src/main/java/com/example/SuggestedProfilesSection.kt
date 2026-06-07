package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SuggestedProfile

@Composable
fun SuggestedProfilesSection(
    profiles: List<SuggestedProfile>,
    onFollow: (String) -> Unit,
    onRemove: (String) -> Unit,
    onSeeAll: () -> Unit
) {
    if (profiles.isEmpty()) return

    val isEnglish = com.example.viewmodel.GlobalLanguage.isEnglish
    Column(modifier = Modifier.fillMaxWidth().background(Color.White).padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEnglish) "People you may know" else "পরিচিত হতে পারেন এমন ব্যক্তি",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF1E293B)
            )
            Text(
                text = if (isEnglish) "See all" else "সবগুলো দেখুন", 
                color = Color(0xFF1877F2), 
                fontSize = 13.sp, 
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { onSeeAll() }
                    .border(1.dp, Color(0xFF1877F2), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                SuggestedProfileCard(profile, onFollow, onRemove)
            }
        }
    }
}

@Composable
fun SuggestedProfileCard(profile: SuggestedProfile, onFollow: (String) -> Unit, onRemove: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser }
    val currentUserId = currentUser?.uid ?: ""
    val db = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    
    var isFollowing by remember { mutableStateOf(false) }
    var followerCount by remember { mutableIntStateOf(0) }
    val isEnglish = com.example.viewmodel.GlobalLanguage.isEnglish
    
    // Real-time Follow State Listener
    DisposableEffect(profile.id, currentUserId) {
        if (currentUserId.isNotEmpty()) {
            val followDoc = db.collection("follows").document("${currentUserId}_${profile.id}")
            val listener = followDoc.addSnapshotListener { snapshot, _ ->
                isFollowing = snapshot?.exists() == true
            }
            onDispose { listener.remove() }
        } else {
            onDispose {}
        }
    }
    
    // Real-time Follower Count Listener
    DisposableEffect(profile.id) {
        val countListener = db.collection("follows")
            .whereEqualTo("creatorId", profile.id)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    followerCount = snapshot.size()
                }
            }
        onDispose { countListener.remove() }
    }

    Card(
        modifier = Modifier.width(155.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                if (profile.profileImageUrl.isNotEmpty()) {
                    coil.compose.AsyncImage(
                        model = profile.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    val firstChar = if (profile.name.isNotEmpty()) profile.name.take(1).uppercase() else ""
                    if (firstChar.isNotEmpty()) {
                        Text(
                            text = firstChar,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color(0xFF475569)
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = profile.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF1E293B),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            
            val followerText = if (isEnglish) {
                "$followerCount Followers"
            } else {
                val bngDigits = followerCount.toString().map { char ->
                    when (char) {
                        '0' -> '০'
                        '1' -> '১'
                        '2' -> '২'
                        '3' -> '৩'
                        '4' -> '৪'
                        '5' -> '৫'
                        '6' -> '৬'
                        '7' -> '৭'
                        '8' -> '৮'
                        '9' -> '৯'
                        else -> char
                    }
                }.joinToString("")
                "$bngDigits ফলোয়ার"
            }
            
            Text(
                text = followerText,
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        if (currentUserId.isEmpty()) {
                            android.widget.Toast.makeText(context, if (isEnglish) "Please log in first" else "দয়া করে প্রথমে লগইন করুন", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (currentUserId == profile.id) {
                            android.widget.Toast.makeText(context, if (isEnglish) "You cannot follow yourself" else "আপনি নিজেকে ফলো করতে পারবেন না", android.widget.Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val followDoc = db.collection("follows").document("${currentUserId}_${profile.id}")
                        if (isFollowing) {
                            followDoc.delete()
                        } else {
                            followDoc.set(mapOf(
                                "followerId" to currentUserId,
                                "creatorId" to profile.id,
                                "timestamp" to System.currentTimeMillis()
                            )).addOnSuccessListener {
                                db.collection("remote_notifications").add(mapOf(
                                    "toId" to profile.id,
                                    "fromId" to currentUserId,
                                    "fromName" to (currentUser?.displayName ?: "Someone"),
                                    "type" to "FOLLOW",
                                    "timestamp" to System.currentTimeMillis()
                                ))
                            }
                        }
                    },
                    modifier = Modifier.weight(1.2f).height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color(0xFFE2E8F0) else Color(0xFF1877F2),
                        contentColor = if (isFollowing) Color(0xFF334155) else Color.White
                    ),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(
                        text = if (isFollowing) {
                            if (isEnglish) "Following" else "ফলোয়িং"
                        } else {
                            if (isEnglish) "Follow" else "ফলো"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                OutlinedButton(
                    onClick = { onRemove(profile.id) },
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Remove" else "বাদ দিন", 
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

