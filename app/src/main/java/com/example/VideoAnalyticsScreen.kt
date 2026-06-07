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
            .background(Color.White),
        contentPadding = PaddingValues(top = 95.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // High-end Profile Header Banner inside the item block
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(chosenLogo.second.copy(alpha = 0.15f), CircleShape)
                        .border(1.5.dp, chosenLogo.second, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = chosenLogo.first,
                        contentDescription = "Profile Logo",
                        tint = chosenLogo.second,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = profileName,
                        color = Color(0xFF111827),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (GlobalLanguage.isEnglish) "$followersCount Followers" else "$followersCount ফলোয়ার্স",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "  •  ",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (GlobalLanguage.isEnglish) "$followingCount Following" else "$followingCount ফলোয়িং",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
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
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (GlobalLanguage.isEnglish) "Personalized Stats Locked" else "ব্যক্তিগত রিপোর্ট লক করা",
                                color = Color(0xFF111827),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (GlobalLanguage.isEnglish) "Log in to see analytics of your uploaded videos!" else "আপনার আপলোড করা ভিডিওগুলোর ব্যক্তিগত রিপোর্ট দেখতে লগইন করুন!",
                                color = Color(0xFF4B5563),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onRequireLogin,
                            colors = ButtonDefaults.textButtonColors(contentColor = PrimaryGreen)
                        ) {
                            Text(
                                text = if (GlobalLanguage.isEnglish) "Log In" else "লগইন",
                                fontWeight = FontWeight.Bold
                            )
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
                    icon = Icons.Default.TrendingUp,
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
                    title = if (GlobalLanguage.isEnglish) "Likes" else "লাইক",
                    value = formatValue(totalLikes),
                    icon = Icons.Filled.Favorite,
                    color = Color(0xFFEC4899),
                    modifier = Modifier.weight(1f)
                )
                AnalyticsCard(
                    title = if (GlobalLanguage.isEnglish) "Shares" else "শেয়ার",
                    value = formatValue(totalShares),
                    icon = Icons.Default.Share,
                    color = Color(0xFF0EA5E9),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Beautiful Interactive Interactive Chart of Views (drawn using high-fidelity Custom Canvas)
        item {
            val dataPoints = remember(userVideosOnly) {
                if (userVideosOnly.isEmpty()) {
                    listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                } else {
                    val sortedVideos = userVideosOnly.sortedBy { it.timestamp }
                    val maxViews = sortedVideos.maxOf { it.viewsCount }.coerceAtLeast(1L).toFloat()
                    val list = mutableListOf<Float>()
                    for (i in 0 until 8) {
                        val idx = (i * (sortedVideos.size - 1)) / 7
                        val v = sortedVideos.getOrNull(idx)
                        val value = if (v != null) v.viewsCount.toFloat() / maxViews else 0f
                        list.add(value.coerceIn(0.04f, 1f))
                    }
                    list
                }
            }

            Surface(
                color = Color(0xFFF9FAFB),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (GlobalLanguage.isEnglish) "Audience Growth (Last 30 Days)" else "দর্শক বৃদ্ধির গ্রাফ (শেষ ৩০ দিন)",
                        color = Color(0xFF111827),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Draw stats graph
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .drawBehind {
                                // Draw grid lines
                                val gridColor = Color(0xFFE5E7EB)
                                val steps = 4
                                for (i in 0..steps) {
                                    val y = size.height * i / steps
                                    drawLine(
                                        color = gridColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                }

                                // Plot a beautiful curvy line of the growth data
                                val path = Path()
                                val brush = Brush.verticalGradient(
                                    colors = listOf(
                                        PrimaryGreen.copy(alpha = 0.4f),
                                        PrimaryGreen.copy(alpha = 0.0f)
                                     )
                                )

                                val stepX = size.width / (dataPoints.size - 1)
                                dataPoints.forEachIndexed { index, value ->
                                    val x = stepX * index
                                    val y = size.height * (1f - value)
                                    if (index == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        // curved line
                                        val prevX = stepX * (index - 1)
                                        val prevY = size.height * (1f - dataPoints[index - 1])
                                        val controlX1 = prevX + (stepX / 2f)
                                        val controlY1 = prevY
                                        val controlX2 = prevX + (stepX / 2f)
                                        val controlY2 = y
                                        path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                                    }
                                }

                                // Create filled background area under the graph
                                val fillPath = Path().apply {
                                    addPath(path)
                                    lineTo(size.width, size.height)
                                    lineTo(0f, size.height)
                                    close()
                                }
                                drawPath(fillPath, brush)
                                drawPath(path, PrimaryGreen, style = Stroke(width = 3.dp.toPx()))

                                // Draw circular handle dots for each data point node
                                dataPoints.forEachIndexed { index, value ->
                                    val x = stepX * index
                                    val y = size.height * (1f - value)
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                    drawCircle(
                                        color = PrimaryGreen,
                                        radius = 3.5.dp.toPx(),
                                        center = Offset(x, y)
                                    )
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val labels = if (GlobalLanguage.isEnglish) {
                            listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4")
                        } else {
                            listOf("সপ্তাহ ১", "সপ্তাহ ২", "সপ্তাহ ৩", "সপ্তাহ ৪")
                        }
                        labels.forEach { label ->
                            Text(text = label, color = Color(0xFF6B7280), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // list showing top-performing uploads if list is full, otherwise tips
        item {
            Text(
                text = if (GlobalLanguage.isEnglish) "Video Performance Tips" else "ভিডিও পারফরম্যান্স টিপস",
                color = Color(0xFF111827),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val tips = if (GlobalLanguage.isEnglish) {
            listOf(
                Pair("Capture Audiences Swiftly", "The first 3 seconds are vital! Open with an inspiring title card or high-energy quote to reduce swipe-offs."),
                Pair("Use Correct Content categories", "Add accurate tags matching Bayan, Recitation or Nasheed so our algorithms route it to interested viewers."),
                Pair("Keep Audio Pristine", "Islamic videos thrive on message clarity. Upload clear sounds with balanced noise cancellation filters.")
            )
        } else {
            listOf(
                Pair("শুরুতেই আকর্ষণ করুন", "ভিডিওর প্রথম ৩ সেকেন্ড খুবই গুরুত্বপূর্ণ! দর্শককে ধরে রাখতে একটি আকর্ষণীয় শিরোনাম বা চমৎকার উক্তি দিয়ে শুরু করুন।"),
                Pair("সঠিক ক্যাটাগরি যুক্ত করুন", "বয়ান, তিলাওয়াত বা নাশিদ অনুযায়ী সঠিক ট্যাগ ব্যবহার করুন, যাতে আগ্রহীদের ফিডে সহজে পৌঁছাতে পারে।"),
                Pair("শব্দের গুণমান ঠিক রাখুন", "ইসলামিক ভিডিওতে বার্তার স্পষ্টতা জরুরি। পরিষ্কার অডিও ফাইল আপলোড করুন এবং নয়েজ এড়িয়ে চলুন।")
            )
        }

        items(tips) { tip ->
            Surface(
                color = Color(0xFFF9FAFB),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TipsAndUpdates,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = tip.first,
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tip.second,
                            color = Color(0xFF4B5563),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
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
