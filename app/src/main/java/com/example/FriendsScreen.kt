package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(onBack: () -> Unit) {
    var profiles by remember { mutableStateOf<List<SuggestedProfile>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val currentUserId = currentUser?.uid ?: ""
    val isEnglish = com.example.viewmodel.GlobalLanguage.isEnglish

    LaunchedEffect(currentUserId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                profiles = snapshot.documents.mapNotNull { doc ->
                    val id = doc.id
                    if (id == currentUserId) return@mapNotNull null
                    SuggestedProfile(
                        id = id,
                        name = doc.getString("name") ?: "Unknown",
                        profileImageUrl = doc.getString("profileImageUrl") ?: "",
                        mutualFriendsCount = 0
                    )
                }
            }
            isLoading = false
        }.addOnFailureListener {
            isLoading = false
        }
    }

    val filteredProfiles = profiles.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        TopAppBar(
            title = { 
                Text(
                    text = if (isEnglish) "Suggested Profiles" else "পরিচিত হতে পারেন যারা", 
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
        )

        if (isSearchVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(if (isEnglish) "Search suggestions" else "পরিচিতদের খুঁজুন") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEnglish) "People you may know" else "পরিচিত মুখ সমূহ", 
                fontWeight = FontWeight.Bold, 
                fontSize = 15.sp,
                color = Color(0xFF475569)
            )
            
            Text(
                text = if (isEnglish) "${filteredProfiles.size} Profiles" else "${filteredProfiles.size} জন",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1877F2))
            }
        } else if (filteredProfiles.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isEnglish) "No suggestions available!" else "কোনো সাজেশন পাওয়া যায়নি!",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProfiles, key = { it.id }) { profile ->
                    FriendsProfileCard(
                        profile = profile,
                        onRemove = { id ->
                            profiles = profiles.filterNot { it.id == id }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FriendsProfileCard(
    profile: SuggestedProfile,
    onRemove: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val currentUserId = currentUser?.uid ?: ""
    val db = remember { FirebaseFirestore.getInstance() }
    
    var isFollowing by remember { mutableStateOf(false) }
    var followerCount by remember { mutableIntStateOf(0) }
    val isEnglish = com.example.viewmodel.GlobalLanguage.isEnglish
    
    // Listen to follow state
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
    
    // Listen to follower count
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
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
                            fontSize = 28.sp,
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
            Spacer(modifier = Modifier.height(12.dp))
            
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                OutlinedButton(
                    onClick = { onRemove(profile.id) },
                    modifier = Modifier.weight(1f).height(32.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF475569)),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Remove" else "বাদ দিন", 
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
