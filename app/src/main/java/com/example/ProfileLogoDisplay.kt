package com.example

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PrimaryGreen

@Composable
fun ProfileLogoDisplay(
    modifier: Modifier = Modifier,
    userId: String = "",
    initialImageUrl: String = "",
    iconSizeDp: Int = 24,
    showBorder: Boolean = false
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    
    // Fallback info for the current user (from prefs)
    val myCustomAvatarUri = sharedPrefs.getString("custom_avatar_uri", "") ?: ""
    val mySelectedLogoIndex = sharedPrefs.getInt("selected_logo_index", 0)
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    // State for the uploader's info
    var uploaderImageUrl by remember(userId, initialImageUrl) { mutableStateOf(initialImageUrl) }
    var uploaderLogoIndex by remember(userId) { mutableStateOf(0) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            // First set to local value if it belongs to current user
            if (userId == currentUserId) {
                if (myCustomAvatarUri.isNotEmpty()) uploaderImageUrl = myCustomAvatarUri
                uploaderLogoIndex = mySelectedLogoIndex
            }

            // Always attempt to fetch from Firestore to stay synced and recover lost local data
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val img = doc.getString("profileImageUrl") ?: ""
                        val index = doc.getLong("selectedLogoIndex")?.toInt() ?: 0
                        uploaderImageUrl = img
                        uploaderLogoIndex = index
                        
                        // If it's my profile, also update local prefs for persistence
                        if (userId == currentUserId) {
                             sharedPrefs.edit()
                                 .putString("custom_avatar_uri", img)
                                 .putInt("selected_logo_index", index)
                                 .apply()
                        }
                    }
                }
        }
    }

    val displayImageUrl = if (userId.isNotEmpty()) uploaderImageUrl else myCustomAvatarUri
    val displayLogoIndex = if (userId.isNotEmpty()) uploaderLogoIndex else mySelectedLogoIndex

    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (showBorder) Modifier.border(1.5.dp, PrimaryGreen, CircleShape) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (displayImageUrl.isNotEmpty()) {
            coil.compose.AsyncImage(
                model = displayImageUrl,
                contentDescription = "Profile Photo",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val (icon, color, bgColor) = when (displayLogoIndex) {
                1 -> Triple(Icons.Default.Star, Color(0xFF3B82F6), Color(0xFFEBF5FF))
                2 -> Triple(Icons.Default.Favorite, Color(0xFFEC4899), Color(0xFFFDF2F8))
                3 -> Triple(Icons.Default.MenuBook, Color(0xFFD97706), Color(0xFFFEF3C7))
                4 -> Triple(Icons.Default.Face, Color(0xFF8B5CF6), Color(0xFFF5F3FF))
                5 -> Triple(Icons.Default.AccountCircle, Color(0xFF14B8A6), Color(0xFFF0FDFA))
                else -> Triple(Icons.Default.Person, Color(0xFF10B981), Color(0xFFE6F4EA))
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Profile Avatar",
                    tint = color,
                    modifier = Modifier.size(iconSizeDp.dp)
                )
            }
        }
    }
}
