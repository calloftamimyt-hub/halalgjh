package com.example.model

import android.net.Uri

// Represents a user-uploaded video record persisted in SharedPreferences and Firestore
data class UserUploadedVideo(
    val docId: String = "",
    val userId: String = "",
    val title: String,
    val author: String,
    val description: String,
    val videoUri: String,
    val timestamp: Long,
    val isLocal: Boolean = true,
    val aspectSize: String = "৯:১৬",
    val category: String = "বয়ান (Bayan)",
    val isOfflineMode: Boolean = false,
    val isAutoSubtitles: Boolean = false,
    val isCommentModerated: Boolean = true,
    val isHideViews: Boolean = false,
    val status: String = "PENDING" // PENDING, APPROVED, REJECTED
) {
    // Empty constructor for Firestore
    constructor() : this("", "", "", "", "", "", 0L)
}
