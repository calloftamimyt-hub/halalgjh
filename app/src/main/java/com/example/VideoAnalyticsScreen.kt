package com.example

import android.content.Context
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserUploadedVideo
import com.example.ui.theme.PrimaryGreen
import com.example.viewmodel.GlobalLanguage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import coil.compose.AsyncImage

@Composable
fun VideoAnalyticsScreen(
    userVideos: List<UserUploadedVideo>,
    onRequireLogin: () -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userVideosOnly = remember(userVideos, currentUser) {
        val uid = currentUser?.uid ?: ""
        userVideos.filter { it.userId == uid }
    }

    var followersCount by remember { mutableStateOf(0) }
    var followingCount by remember { mutableStateOf(0) }

    LaunchedEffect(currentUser) {
        val uid = currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            // Real-time followers listener
            val followersListener = db.collection("follows")
                .whereEqualTo("creatorId", uid)
                .addSnapshotListener { snapshots, e ->
                    if (e == null && snapshots != null) {
                        followersCount = snapshots.size()
                    }
                }
            // Real-time following listener
            val followingListener = db.collection("follows")
                .whereEqualTo("followerId", uid)
                .addSnapshotListener { snapshots, e ->
                    if (e == null && snapshots != null) {
                        followingCount = snapshots.size()
                    }
                }
        } else {
            followersCount = 0
            followingCount = 0
        }
    }

    // Real Stats calculations (no mock or demo figures)
    val totalUploads = if (currentUser != null) userVideosOnly.size else 0
    val totalViews = if (currentUser != null) {
        userVideosOnly.sumOf { it.viewsCount }
    } else 0L

    val totalLikes = if (currentUser != null) {
        userVideosOnly.sumOf { it.likedBy.size.toLong() }
    } else 0L

    val totalShares = if (currentUser != null) {
        userVideosOnly.sumOf { it.sharesCount }
    } else 0L

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    val selectedLogoIndex = sharedPrefs.getInt("selected_logo_index", 0)
    val customAvatarUri = sharedPrefs.getString("custom_avatar_uri", "") ?: ""
    
    val logoIcons = listOf(
        Icons.Default.Person to Color(0xFF10B981),
        Icons.Default.Star to Color(0xFF3B82F6),
        Icons.Default.Favorite to Color(0xFFEC4899),
        Icons.Default.MenuBook to Color(0xFFD97706),
        Icons.Default.Face to Color(0xFF8B5CF6),
        Icons.Default.AccountCircle to Color(0xFF14B8A6)
    )
    val chosenLogo = logoIcons.getOrNull(selectedLogoIndex) ?: logoIcons[0]
    val profileName = currentUser?.displayName ?: (if (GlobalLanguage.isEnglish) "Guest Creator" else "অতিথি ক্রিয়েটর")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB)), // Light gray background for professional look
        contentPadding = PaddingValues(top = 80.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-end Profile Header Banner inside the item block
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                         ProfileLogoDisplay(
                            modifier = Modifier.size(64.dp),
                            userId = currentUser?.uid ?: "",
                            iconSizeDp = 32,
                            showBorder = true
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profileName,
                            color = Color(0xFF111827),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (GlobalLanguage.isEnglish) "$followersCount Followers" else "$followersCount ফলোয়ার্স",
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = " • ",
                                color = Color(0xFF9CA3AF),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            Text(
                                text = if (GlobalLanguage.isEnglish) "$followingCount Following" else "$followingCount ফলোয়িং",
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = { /* Settings context */ }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (currentUser == null) {
            item {
                Surface(
                    color = PrimaryGreen.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(24.dp)
                         )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (GlobalLanguage.isEnglish) "Sign in for Analytics" else "অ্যানালিটিক্স দেখতে লগইন করুন",
                                color = Color(0xFF111827),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (GlobalLanguage.isEnglish) "Track your performance and growth!" else "আপনার ভিডিওর পারফরম্যান্স ট্র্যাক করুন!",
                                color = Color(0xFF4B5563),
                                fontSize = 12.sp
                            )
                        }
                        TextButton(
                            onClick = onRequireLogin,
                            colors = ButtonDefaults.textButtonColors(contentColor = PrimaryGreen)
                        ) {
                            Text(text = if (GlobalLanguage.isEnglish) "Log In" else "লগইন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Key Numeric Stats Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsCard(
                    title = if (GlobalLanguage.isEnglish) "Total Videos" else "মোট ভিডিও",
                    value = formatValue(totalUploads.toLong()),
                    icon = Icons.Default.VideoLibrary,
                    color = Color(0xFF6366F1),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsCard(
                    title = if (GlobalLanguage.isEnglish) "Total Views" else "মোট দর্শক",
                    value = formatValue(totalViews),
                    icon = Icons.Default.Visibility,
                    color = PrimaryGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsCard(
                    title = if (GlobalLanguage.isEnglish) "Total Likes" else "মোট লাইক",
                    value = formatValue(totalLikes),
                    icon = Icons.Filled.Favorite,
                    color = Color(0xFFEC4899),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsCard(
                    title = if (GlobalLanguage.isEnglish) "Total Shares" else "মোট শেয়ার",
                    value = formatValue(totalShares),
                    icon = Icons.Default.Share,
                    color = Color(0xFF0EA5E9),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Beautiful Interactive Chart
        item {
            val dataPoints = remember(userVideosOnly) {
                if (userVideosOnly.isEmpty()) {
                    listOf(0.2f, 0.15f, 0.3f, 0.25f, 0.45f, 0.35f, 0.6f, 0.5f)
                } else {
                    val sortedVideos = userVideosOnly.sortedBy { it.timestamp }
                    val maxViews = sortedVideos.maxOf { it.viewsCount }.coerceAtLeast(1L).toFloat()
                    val list = mutableListOf<Float>()
                    for (i in 0 until 8) {
                        val idx = (i * (sortedVideos.size - 1)) / 7
                        val v = sortedVideos.getOrNull(idx)
                        val value = if (v != null) v.viewsCount.toFloat() / maxViews else 0.1f
                        list.add(value.coerceIn(0.1f, 1f))
                    }
                    list
                }
            }

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (GlobalLanguage.isEnglish) "Engagement Trends" else "এনগেজমেন্ট ট্রেন্ড",
                        color = Color(0xFF111827),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .drawBehind {
                                val gridColor = Color(0xFFF3F4F6)
                                for (i in 0..4) {
                                    val y = size.height * i / 4
                                    drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1.dp.toPx())
                                }

                                val path = Path()
                                val stepX = size.width / (dataPoints.size - 1)
                                dataPoints.forEachIndexed { index, value ->
                                    val x = stepX * index
                                    val y = size.height * (1f - value)
                                    if (index == 0) path.moveTo(x, y)
                                    else {
                                        val prevX = stepX * (index - 1)
                                        val prevY = size.height * (1f - dataPoints[index - 1])
                                        path.cubicTo(prevX + stepX/2, prevY, prevX + stepX/2, y, x, y)
                                    }
                                }

                                val fillPath = Path().apply {
                                    addPath(path)
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(fillPath, Brush.verticalGradient(listOf(PrimaryGreen.copy(0.2f), Color.Transparent)))
                                drawPath(path, PrimaryGreen, style = Stroke(3.dp.toPx()))
                            }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4").forEach {
                            Text(text = it, color = Color(0xFF9CA3AF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = if (GlobalLanguage.isEnglish) "Performance Tips" else "পারফরম্যান্স টিপস",
                color = Color(0xFF111827),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val tips = if (GlobalLanguage.isEnglish) {
            listOf(
                Pair("Higher Quality Audio", "Islamic content relies heavily on clear speech. Ensure your microphone is close."),
                Pair("Optimal Video Length", "Try to keep reels between 30-50 seconds for best retention rates."),
                Pair("Engage in Comments", "Replying to viewers increases your video's visibility in social circles.")
            )
        } else {
            listOf(
                Pair("উন্নত আডিও মান", "ইসলামিক কন্টেন্টে বার্তার স্পষ্টতা জরুরি। ভালো মানের অডিও বজায় রাখুন।"),
                Pair("সঠিক সময়সীমা", "ভিডিও ৩০-৫০ সেকেন্ডের মধ্যে রাখার চেষ্টা করুন, এতে দর্শক ধরে রাখা সহজ হয়।"),
                Pair("কমেন্টে অ্যাক্টিভ থাকা", "দর্শকদের কমেন্টের উত্তর দিলে আপনার ভিডিওর রিচ বৃদ্ধি পায়।")
            )
        }

        items(tips) { tip ->
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 0.5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.size(36.dp).background(PrimaryGreen.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lightbulb, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(tip.first, color = Color(0xFF111827), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(tip.second, color = Color(0xFF6B7280), fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, color = Color(0xFF4B5563), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                color = Color(0xFF111827),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatValue(num: Long): String {
    if (num >= 1000) {
        val thousands = num / 1000f
        return String.format(java.util.Locale.US, "%.1fK", thousands)
    }
    return num.toString()
}
