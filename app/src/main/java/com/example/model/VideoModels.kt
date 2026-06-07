package com.example.model

import android.net.Uri

// Represents a user-uploaded video record persisted in SharedPreferences and Firestore
data class UserUploadedVideo(
    val docId: String = "",
    val userId: String = "",
    val title: String = "Untitled",
    val author: String = "Unknown Author",
    val description: String = "",
    val videoUri: String = "",
    val timestamp: Long = 0L,
    val isLocal: Boolean = true,
    val aspectSize: String = "৯:১৬",
    val category: String = "বয়ান (Bayan)",
    val isOfflineMode: Boolean = false,
    val isAutoSubtitles: Boolean = false,
    val isCommentModerated: Boolean = true,
    val isHideViews: Boolean = false,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val telegramFileId: String = "",
    val thumbnailUrl: String = "",
    val viewsCount: Long = 0L,
    val likedBy: List<String> = emptyList(),
    val sharesCount: Long = 0L
)
