package com.example

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.NotificationEntity
import com.example.database.TrackerDatabase
import com.example.ui.theme.PrimaryGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Database and Dao access
    val trackerDb = remember { TrackerDatabase.getDatabase(context) }
    val notificationDao = remember { trackerDb.notificationDao() }
    
    // State flow for notifications
    val notificationsFlow = remember { notificationDao.getAllNotifications() }
    val notifications by notificationsFlow.collectAsState(initial = emptyList())
    
    // Selected notification for the detail screen
    var selectedNotification by remember { mutableStateOf<NotificationEntity?>(null) }
    
    // No more demo notifications seeding

    // Double Screen implementation using AnimatedContent (Details page stack)
    AnimatedContent(
        targetState = selectedNotification,
        transitionSpec = {
            if (targetState != null) {
                // Slide in from right (forward transition)
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                // Slide out to right (back transition)
                slideInHorizontally { width -> -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "NotificationNavigation"
    ) { currentNotification ->
        if (currentNotification == null) {
            // Screen 1: Notification Feed Screen
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .statusBarsPadding()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF050505)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notifications",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF050505)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            
                            // Mark all as read action
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    notificationDao.markAllAsRead()
                                    Toast.makeText(context, "Marked as read", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Mark all as read",
                                    tint = PrimaryGreen
                                )
                            }
                            
                            // Delete all notifications
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    notificationDao.deleteAllNotifications()
                                    Toast.makeText(context, "All notifications deleted", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Delete all",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                },
                containerColor = Color(0xFFF0F2F5) // Beautiful Facebook gray spacer color
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (notifications.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.NotificationsOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("প্রতীক্ষা করুন... কোনো নোটিফিকেশন নেই।", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        val uniqueNotifications = remember(notifications) {
                            notifications.distinctBy { 
                                // Logical uniqueness: same person doing same action to same item
                                "${it.actorName}_${it.itemTitle}_${it.type}"
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Section header
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White)
                                        .padding(horizontal = 16.dp, vertical = 14.dp)
                                ) {
                                    Text(
                                        text = "Earlier",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF050505)
                                    )
                                }
                            }

                            // Notification Rows are tightly packed with minimal gap to look unified
                            items(uniqueNotifications, key = { it.id }) { item ->
                                val relativeTime = getFormattedTimeAgo(item.timestamp)
                                NotificationRowItem(
                                    notification = item,
                                    timeAgo = relativeTime,
                                    onClick = {
                                        // Mark this item as read upon clicking
                                        coroutineScope.launch {
                                            if (!item.isRead) {
                                                notificationDao.updateNotification(item.copy(isRead = true))
                                            }
                                        }
                                        selectedNotification = item
                                    },
                                    onDelete = {
                                        coroutineScope.launch {
                                            notificationDao.deleteNotificationById(item.id)
                                            Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }

                            // Bottom space padding
                            item {
                                Spacer(modifier = Modifier.height(30.dp))
                            }
                        }
                    }
                }
            }
        } else {
            // Screen 2: Detailed View of a Notification
            NotificationDetailScreen(
                notification = currentNotification,
                onBack = { selectedNotification = null }
            )
        }
    }
}

@Composable
fun NotificationRowItem(
    notification: NotificationEntity,
    timeAgo: String,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (notification.isRead) Color.White else Color(0xFFE7F3FF))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with bottom-right Overlaid Badge
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .padding(end = 6.dp)
            ) {
                // Main Rounded Avatar Circle
                val initialLetter = if (notification.actorName.isNotEmpty()) {
                    notification.actorName.take(1).uppercase()
                } else "H"
                
                // Friendly Color mapping derived from user name letters
                val avatarBg = remember(notification.actorName) {
                    val colors = listOf(
                        Color(0xFFE0F2FE), Color(0xFFFEF3C7), Color(0xFFDCFCE7),
                        Color(0xFFF3E8FF), Color(0xFFFEE2E2), Color(0xFFFFEDD5)
                    )
                    val textColors = listOf(
                        Color(0xFF0369A1), Color(0xFFB45309), Color(0xFF15803D),
                        Color(0xFF6B21A8), Color(0xFF991B1B), Color(0xFFC2410C)
                    )
                    val index = Math.abs(notification.actorName.hashCode()) % colors.size
                    Pair(colors[index], textColors[index])
                }

                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(avatarBg.first),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialLetter,
                        color = avatarBg.second,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }

                // Overlaid Badge Icon indicating the action type
                val badgeConfig = getBadgeConfigForType(notification.type)
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(2.dp) // Border gap
                        .clip(CircleShape)
                        .background(badgeConfig.containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeConfig.icon,
                        contentDescription = "Action badge",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            // Middle: Description column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            ) {
                // Build Annotated Text style matches screenshot
                val annotatedBodyText = remember(notification.actorName, notification.itemTitle) {
                    buildAnnotatedString {
                        // Actor's name (Bold)
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF050505))) {
                            append(notification.actorName)
                        }
                        
                        // Verb Action dependent on notification type
                        val actionVerb = when (notification.type) {
                            "LIKE" -> " liked your "
                            "SHARE" -> " recently shared "
                            "COMMENT" -> " commented on your "
                            "VIDEO" -> " is ready to view. "
                            "FOLLOW" -> " has followed you. "
                            "UNFOLLOW" -> " has unfollowed you. "
                            else -> " interacted with your "
                        }
                        append(actionVerb)

                        // Subject context, e.g., reel title, post content, etc. (Bold)
                        if (notification.itemTitle.isNotEmpty()) {
                            val wrapper = if (notification.type == "LIKE" || notification.type == "COMMENT") {
                                "\"${notification.itemTitle}\""
                            } else {
                                notification.itemTitle
                            }
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF050505))) {
                                append(wrapper)
                            }
                        }
                    }
                }

                Text(
                    text = annotatedBodyText,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = timeAgo,
                    fontSize = 13.sp,
                    color = if (notification.isRead) Color.Gray else PrimaryGreen,
                    fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold
                )
            }

            // Right: Multi options menu (three dots)
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = Color.Gray
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete Notification", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }

        // Extremely thin divider
        Divider(
            color = Color(0xFFECEEF1),
            thickness = 0.8.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
    notification: NotificationEntity,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Notification Detail",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF050505)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF050505))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Headline header containing the notification action indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val badgeConfig = getBadgeConfigForType(notification.type)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(badgeConfig.containerColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = badgeConfig.icon,
                                contentDescription = "Badge icon",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = notification.type,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                fontSize = 12.sp
                            )
                            val relativeTime = getFormattedTimeAgo(notification.timestamp)
                            Text(
                                text = relativeTime,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = notification.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF050505)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = notification.body,
                        fontSize = 15.sp,
                        color = Color(0xFF374151),
                        lineHeight = 22.sp
                    )

                    if (notification.itemTitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF3F4F6))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (notification.type == "VIDEO") Icons.Default.PlayArrow else Icons.Default.Description,
                                    contentDescription = "Content link icon",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (notification.type == "VIDEO") "Attached Reel Item" else "Reference Context",
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = notification.itemTitle,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF1F2937),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Close Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// Utility configuration mapping Badge look
data class BadgeConfig(val icon: ImageVector, val containerColor: Color)

private fun getBadgeConfigForType(type: String): BadgeConfig {
    return when (type) {
        "LIKE" -> BadgeConfig(Icons.Default.ThumbUp, Color(0xFF1877F2)) // Facebook Blue circle
        "SHARE" -> BadgeConfig(Icons.Default.Share, Color(0xFF007A87)) // Deep Teal circle
        "COMMENT" -> BadgeConfig(Icons.Default.Comment, Color(0xFF4CAF50)) // Green circle
        "VIDEO" -> BadgeConfig(Icons.Default.Videocam, Color(0xFFE91E63)) // Pink/Red circle
        "FOLLOW" -> BadgeConfig(Icons.Default.PersonAdd, PrimaryGreen)
        "UNFOLLOW" -> BadgeConfig(Icons.Default.PersonRemove, Color.Red)
        else -> BadgeConfig(Icons.Default.Notifications, PrimaryGreen)
    }
}

// Helper method converting millies to clean format like 2h, 5h, 15h, 1d
private fun getFormattedTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d"
        else -> {
            DateUtils.getRelativeTimeSpanString(timestamp).toString()
        }
    }
}
