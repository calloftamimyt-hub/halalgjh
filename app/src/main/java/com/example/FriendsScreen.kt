package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SuggestedProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onBack: () -> Unit,
    onNavigateToCreatorProfile: (String, String) -> Unit
) {
    var profiles by remember { mutableStateOf<List<SuggestedProfile>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val currentUserId = currentUser?.uid ?: ""
    val isEnglish = com.example.viewmodel.GlobalLanguage.isEnglish

    // Animation states
    var searchBarExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Init expansion animation
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        searchBarExpanded = true
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignored if focus fails
        }
    }

    // Load initial users
    LaunchedEffect(currentUserId) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .limit(100)
            .get()
            .addOnSuccessListener { snapshot ->
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
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    // Dynamic Firestore prefix search in real-time
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            val queryText = searchQuery.trim()
            db.collection("users")
                .orderBy("name")
                .startAt(queryText)
                .endAt(queryText + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null) {
                        val queriedList = snapshot.documents.mapNotNull { doc ->
                            val id = doc.id
                            if (id == currentUserId) return@mapNotNull null
                            SuggestedProfile(
                                id = id,
                                name = doc.getString("name") ?: "Unknown",
                                profileImageUrl = doc.getString("profileImageUrl") ?: "",
                                mutualFriendsCount = 0
                            )
                        }
                        // Append newly queried users, maintaining uniqueness
                        val existingIds = profiles.map { it.id }.toSet()
                        val uniqueNew = queriedList.filter { it.id !in existingIds }
                        if (uniqueNew.isNotEmpty()) {
                            profiles = profiles + uniqueNew
                        }
                    }
                }
        }
    }

    // Local filter on the combined list
    val filteredProfiles = remember(profiles, searchQuery) {
        if (searchQuery.isEmpty()) {
            profiles
        } else {
            profiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
    ) {
        // Beautiful Top Animated Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Animated Expanding Search Bar
            val animatedWeight by animateFloatAsState(
                targetValue = if (searchBarExpanded) 1f else 0.1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "search_bar_width"
            )

            Box(
                modifier = Modifier
                    .weight(animatedWeight)
                    .fillMaxHeight()
                    .height(48.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = if (isEnglish) "Search profiles to connect..." else "কাউকে খুঁজতে নাম লিখে সার্চ করুন...",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF64748B)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )
            }
        }

        // Subtitle header
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + slideInVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isEmpty()) {
                        if (isEnglish) "Profiles Recommended For You" else "আপনার জন্য সাজেস্টেড প্রোফাইল সমূহ"
                    } else {
                        if (isEnglish) "Search Matches" else "সার্চের ফলাফল সমূহ"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color(0xFF334155)
                )

                Text(
                    text = if (isEnglish) "${filteredProfiles.size} Profiles" else "${filteredProfiles.size} জন",
                    fontSize = 12.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF10B981))
            }
        } else if (filteredProfiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isEnglish) "No matching profiles found!" else "কোনো প্রোফাইল পাওয়া যায়নি!",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredProfiles, key = { it.id }) { profile ->
                    FriendsProfileCard(
                        profile = profile,
                        onClickCard = {
                            onNavigateToCreatorProfile(profile.id, profile.name)
                        },
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
    onClickCard: () -> Unit,
    onRemove: (String) -> Unit
) {
    val context = LocalContext.current
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
            .clickable(onClick = onClickCard),
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
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                if (profile.profileImageUrl.isNotEmpty()) {
                    coil.compose.AsyncImage(
                        model = profile.profileImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
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
                overflow = TextOverflow.Ellipsis
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
                            android.widget.Toast.makeText(
                                context,
                                if (isEnglish) "Please log in first" else "দয়া করে প্রথমে লগইন করুন",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        if (currentUserId == profile.id) {
                            android.widget.Toast.makeText(
                                context,
                                if (isEnglish) "You cannot follow yourself" else "আপনি নিজেকে ফলো করতে পারবেন না",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        val followDoc = db.collection("follows").document("${currentUserId}_${profile.id}")
                        if (isFollowing) {
                            followDoc.delete()
                        } else {
                            followDoc.set(
                                mapOf(
                                    "followerId" to currentUserId,
                                    "creatorId" to profile.id,
                                    "timestamp" to System.currentTimeMillis()
                                )
                            ).addOnSuccessListener {
                                db.collection("remote_notifications").add(
                                    mapOf(
                                        "toId" to profile.id,
                                        "actorId" to currentUserId,
                                        "actorName" to (currentUser?.displayName ?: "Someone"),
                                        "type" to "FOLLOW",
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .height(32.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) Color(0xFFE2E8F0) else Color(0xFF10B981),
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
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
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
