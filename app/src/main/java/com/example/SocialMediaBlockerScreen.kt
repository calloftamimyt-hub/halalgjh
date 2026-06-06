package com.example

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMediaBlockerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }

    // Navigation & back handler
    BackHandler(onBack = onBack)

    var showTermsAndPrivacyFullScreen by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showRestrictedHelp by remember { mutableStateOf(false) }

    // YouTube states
    var isYtLongBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("yt_long_blocked", false)) }
    var isYtReelsBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("yt_reels_blocked", false)) }
    var isYtSearchBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("yt_search_blocked", false)) }
    var isYtEntireBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("yt_entire_blocked", false)) }

    // Facebook states
    var isFbAppBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("fb_app_blocked", false)) }
    var isFbStoryBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("fb_story_blocked", false)) }
    var isFbSearchBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("fb_search_blocked", false)) }
    var isFbReelsBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("fb_reels_blocked", false)) }
    var isFbEntireBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("fb_entire_blocked", false)) }

    // Telegram states
    var isTgAppBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("tg_app_blocked", false)) }
    var isTgSearchBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("tg_search_blocked", false)) }
    var isTgStoryBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("tg_story_blocked", false)) }
    var isTgEntireBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("tg_entire_blocked", false)) }

    // WhatsApp states
    var isWaAppBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("wa_app_blocked", false)) }
    var isWaStoryBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("wa_story_blocked", false)) }
    var isWaEntireBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("wa_entire_blocked", false)) }

    // Messenger states
    var isMsAppBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("ms_app_blocked", false)) }
    var isMsStoryBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("ms_story_blocked", false)) }
    var isMsEntireBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("ms_entire_blocked", false)) }

    // Instagram states
    var isIgAppBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("ig_app_blocked", false)) }
    var isIgSearchBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("ig_search_blocked", false)) }
    var isIgReelsBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("ig_reels_blocked", false)) }
    var isIgFeaturesBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("ig_features_blocked", false)) }
    var isIgEntireBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("ig_entire_blocked", false)) }

    var isYtExpanded by remember { mutableStateOf(true) }
    var isFbExpanded by remember { mutableStateOf(true) }
    var isTgExpanded by remember { mutableStateOf(false) }
    var isWaExpanded by remember { mutableStateOf(false) }
    var isMsExpanded by remember { mutableStateOf(false) }
    var isIgExpanded by remember { mutableStateOf(true) }

    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogText by remember { mutableStateOf<String?>(null) }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = android.content.ComponentName(context, SocialAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val accessibilityEnabled = enabledServices?.contains(expectedComponentName.flattenToString()) == true
        val overlayEnabled = Settings.canDrawOverlays(context)
        return accessibilityEnabled && overlayEnabled
    }

    fun updateServiceState() {
        val keys = listOf(
            "fb_app_blocked", "fb_story_blocked", "fb_search_blocked", "fb_reels_blocked", "fb_entire_blocked",
            "yt_long_blocked", "yt_reels_blocked", "yt_search_blocked", "yt_entire_blocked",
            "ig_app_blocked", "ig_search_blocked", "ig_reels_blocked", "ig_features_blocked", "ig_entire_blocked",
            "tg_app_blocked", "tg_search_blocked", "tg_story_blocked", "tg_entire_blocked",
            "wa_app_blocked", "wa_story_blocked", "wa_entire_blocked",
            "ms_app_blocked", "ms_story_blocked", "ms_entire_blocked"
        )
        val anyActive = keys.any { sharedPrefs.getBoolean(it, false) }
        sharedPrefs.edit().putBoolean("social_blocked", anyActive).apply()
        
        if (anyActive) {
            SocialBlockerService.startService(context)
        } else {
            SocialBlockerService.stopService(context)
        }
    }

    fun checkAndSetBlock(checked: Boolean, onAllowed: () -> Unit) {
        if (checked) {
            if (isAccessibilityServiceEnabled(context)) {
                onAllowed()
                updateServiceState()
            } else {
                showPermissionDialog = true
            }
        } else {
            onAllowed()
            updateServiceState()
        }
    }

    if (showTermsAndPrivacyFullScreen) {
        TermsAndPrivacyScreen(onBack = { showTermsAndPrivacyFullScreen = false })
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgLight)
        ) {
            Spacer(modifier = Modifier.statusBarsPadding())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "পিছনে যান",
                            tint = TextDark
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Social Media Blocker",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = TextDark
                    )
                }

                IconButton(
                    onClick = { showTermsAndPrivacyFullScreen = true },
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                        .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "শর্তাবলী ও প্রাইভেসী পলিসি",
                        tint = TextDark
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFE0F5EE), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block, 
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "সোশ্যাল মিডিয়া নিয়ন্ত্রণ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                }

                // 1. FACEBOOK PLATFORM CARD
                PlatformBlockCard(
                    title = "Facebook",
                    icon = Icons.Default.Public,
                    iconColor = Color(0xFF1877F2),
                    isExpanded = isFbExpanded,
                    onToggleExpand = { isFbExpanded = !isFbExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BlockItemRow(
                            title = "ফেসবুক রিলস",
                            isEnabled = isFbReelsBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isFbReelsBlocked = checked
                                    sharedPrefs.edit().putBoolean("fb_reels_blocked", checked).apply()
                                    showNotificationToast(context, "ফেসবুক রিলস", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ফেসবুক রিলস লক"
                                infoDialogText = "ফেসবুক অ্যাপ বা ওয়েবসাইট খোলার পর রিলস বা ছোট ভিডিও ফিড সম্পূর্ণ লুকিয়ে ফেলা হবে।"
                            }
                        )

                        BlockItemRow(
                            title = "ফেসবুক স্টোরি",
                            isEnabled = isFbStoryBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isFbStoryBlocked = checked
                                    sharedPrefs.edit().putBoolean("fb_story_blocked", checked).apply()
                                    showNotificationToast(context, "ফেসবুক স্টোরি", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ফেসবুক স্টোরি লক"
                                infoDialogText = "স্মার্ট হোমপেজ থেকে ফেসবুক স্টোরির অংশটি স্বয়ংক্রিয়ভাবে ব্লক করে একাগ্রতা বাড়ানো হবে।"
                            }
                        )

                        BlockItemRow(
                            title = "ফেসবুক সার্চ",
                            isEnabled = isFbSearchBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isFbSearchBlocked = checked
                                    sharedPrefs.edit().putBoolean("fb_search_blocked", checked).apply()
                                    showNotificationToast(context, "ফেসবুক সার্চ", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ফেসবুক অনুসন্ধান লক"
                                infoDialogText = "ফেসবুক এ নতুন কোনো বিষয় সার্চ করা সাময়িকভাবে লক থাকবে।"
                            }
                        )

                        BlockItemRow(
                            title = "ফেসবুক অ্যাপ (Facebook App)",
                            isEnabled = isFbAppBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isFbAppBlocked = checked
                                    sharedPrefs.edit().putBoolean("fb_app_blocked", checked).apply()
                                    showNotificationToast(context, "ফেসবুক অ্যাপ ব্রাউজিং", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ফেসবুক অ্যাপ লক"
                                infoDialogText = "ফেসবুক মেইন অ্যাপ্লিকেশন ওপেন করা সীমাবদ্ধ থাকবে।"
                            }
                        )

                        BlockItemRow(
                            title = "ফেসবুক ব্লক করুন",
                            isEnabled = isFbEntireBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isFbEntireBlocked = checked
                                    isFbAppBlocked = checked
                                    isFbStoryBlocked = checked
                                    isFbSearchBlocked = checked
                                    isFbReelsBlocked = checked
                                    sharedPrefs.edit()
                                        .putBoolean("fb_entire_blocked", checked)
                                        .putBoolean("fb_app_blocked", checked)
                                        .putBoolean("fb_story_blocked", checked)
                                        .putBoolean("fb_search_blocked", checked)
                                        .putBoolean("fb_reels_blocked", checked)
                                        .apply()
                                    showNotificationToast(context, "ফেসবুক সম্পূর্ণ সার্ভিস", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ফেসবুক কমপ্লিট ব্লক"
                                infoDialogText = "ফেসবুকের যাবতীয় ব্রাউজিং, অ্যাপ এবং নোটিফিকেশন সার্ভিস সম্পূর্ণভাবে বন্ধ রাখা হবে।"
                            }
                        )
                    }
                }

                // 2. YOUTUBE PLATFORM CARD
                PlatformBlockCard(
                    title = "YouTube",
                    icon = Icons.Default.PlayCircle,
                    iconColor = Color(0xFFFF0000),
                    isExpanded = isYtExpanded,
                    onToggleExpand = { isYtExpanded = !isYtExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BlockItemRow(
                            title = "ইউটিউব রিলস (YouTube Reels / Shorts)",
                            isEnabled = isYtReelsBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isYtReelsBlocked = checked
                                    sharedPrefs.edit().putBoolean("yt_reels_blocked", checked).apply()
                                    showNotificationToast(context, "ইউটিউব শর্টস", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ইউটিউব শর্টস ব্লক"
                                infoDialogText = "ইউটিউবের ৬০ সেকেন্ডের শর্ট এবং ড্র্যাগ স্ক্রলিং আসক্তি কমাতে এটি কার্যকর।"
                            }
                        )

                        BlockItemRow(
                            title = "ইউটিউব লং ভিডিও (Long Video)",
                            isEnabled = isYtLongBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isYtLongBlocked = checked
                                    sharedPrefs.edit().putBoolean("yt_long_blocked", checked).apply()
                                    showNotificationToast(context, "ইউটিউব লং ভিডিও", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ইউটিউব লং ভিডিও লক"
                                infoDialogText = "ইউটিউবে যেকোনো দীর্ঘ ভিডিও দেখা বা কন্টেন্ট প্লে করা লক থাকবে।"
                            }
                        )

                        BlockItemRow(
                            title = "ইউটিউব সার্চ (YouTube Search)",
                            isEnabled = isYtSearchBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isYtSearchBlocked = checked
                                    sharedPrefs.edit().putBoolean("yt_search_blocked", checked).apply()
                                    showNotificationToast(context, "ইউটিউব সার্চিং", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ইউটিউব অনুসন্ধান লক"
                                infoDialogText = "ইউটিউবে সার্চের মাধ্যমে বিভ্রান্তিকর কন্টেন্ট খোঁজা প্রতিরোধ করার চমৎকার ফিচার।"
                            }
                        )

                        BlockItemRow(
                            title = "ইউটিউব সম্পূর্ণ ব্লক করুন",
                            isEnabled = isYtEntireBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isYtEntireBlocked = checked
                                    isYtReelsBlocked = checked
                                    isYtLongBlocked = checked
                                    isYtSearchBlocked = checked
                                    sharedPrefs.edit()
                                        .putBoolean("yt_entire_blocked", checked)
                                        .putBoolean("yt_reels_blocked", checked)
                                        .putBoolean("yt_long_blocked", checked)
                                        .putBoolean("yt_search_blocked", checked)
                                        .apply()
                                    showNotificationToast(context, "ইউটিউব সম্পূর্ণ সার্ভিস", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ইউটিউব ফুল ব্লক"
                                infoDialogText = "ইউটিউবের সকল কার্যকারিতা, ভিডিও স্ট্রিমিং ও সেটিংস ব্লক করা থাকবে দ্বীনের কল্যাণে।"
                            }
                        )
                    }
                }

                // 3. INSTAGRAM PLATFORM CARD
                PlatformBlockCard(
                    title = "Instagram",
                    icon = Icons.Default.CameraAlt,
                    iconColor = Color(0xFFE1306C),
                    isExpanded = isIgExpanded,
                    onToggleExpand = { isIgExpanded = !isIgExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BlockItemRow(
                            title = "ইনস্টাগ্রাম রিলস (Instagram Reels)",
                            isEnabled = isIgReelsBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isIgReelsBlocked = checked
                                    sharedPrefs.edit().putBoolean("ig_reels_blocked", checked).apply()
                                    showNotificationToast(context, "ইনস্টাগ্রাম রিলস", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ইনস্টাগ্রাম রিলস"
                                infoDialogText = "ইনস্টাগ্রামে অনাকাঙ্ক্ষিত ও আপত্তিকর শর্ট রিলস তিলে তিলে একাগ্রতা নষ্ট করা বন্ধ করতে সাহায্য করে।"
                            }
                        )

                        BlockItemRow(
                            title = "ইনস্টাগ্রাম সার্চ (Instagram Search)",
                            isEnabled = isIgSearchBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isIgSearchBlocked = checked
                                    sharedPrefs.edit().putBoolean("ig_search_blocked", checked).apply()
                                    showNotificationToast(context, "ইনস্টাগ্রাম এক্সপ্লোর সার্চ", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ইনস্টাগ্রাম সার্চ লক"
                                infoDialogText = "ইনস্টাগ্রাম এক্সপ্লোর ট্যাব ও সার্চ ব্লক করার মাধ্যমে পবিত্র মনের হিফাজত করা যাবে।"
                            }
                        )

                        BlockItemRow(
                            title = "ইনস্টাগ্রাম সম্পূর্ণ ব্লক করুন",
                            isEnabled = isIgEntireBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isIgEntireBlocked = checked
                                    isIgReelsBlocked = checked
                                    isIgSearchBlocked = checked
                                    isIgAppBlocked = checked
                                    isIgFeaturesBlocked = checked
                                    sharedPrefs.edit()
                                        .putBoolean("ig_entire_blocked", checked)
                                        .putBoolean("ig_reels_blocked", checked)
                                        .putBoolean("ig_search_blocked", checked)
                                        .putBoolean("ig_app_blocked", checked)
                                        .putBoolean("ig_features_blocked", checked)
                                        .apply()
                                    showNotificationToast(context, "ইনস্টাগ্রাম সম্পূর্ণ সার্ভিস", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "ইনস্টাগ্রাম ফুল ব্লক"
                                infoDialogText = "ইনস্টাগ্রামের সকল অ্যাকাউন্ট তদারকি, চ্যাটিং ও রিলস ব্রাউজিং সম্পূর্ণ অফ করা হবে।"
                            }
                        )
                    }
                }

                // 4. TELEGRAM PLATFORM CARD
                PlatformBlockCard(
                    title = "Telegram",
                    icon = Icons.Default.Send,
                    iconColor = Color(0xFF0088CC),
                    isExpanded = isTgExpanded,
                    onToggleExpand = { isTgExpanded = !isTgExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BlockItemRow(
                            title = "টেলিগ্রাম অ্যাপ (Telegram App)",
                            isEnabled = isTgAppBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isTgAppBlocked = checked
                                    sharedPrefs.edit().putBoolean("tg_app_blocked", checked).apply()
                                    showNotificationToast(context, "টেলিগ্রাম ব্রাউজিং", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "টেলিগ্রাম অ্যাপ লক"
                                infoDialogText = "টেলিগ্রাম অ্যাপ্লিকেশন ক্লায়েন্ট ওপেন করা ডিরেক্ট ব্লক থাকবে।"
                            }
                        )

                        BlockItemRow(
                            title = "টেলিগ্রাম সার্চ (Telegram Search)",
                            isEnabled = isTgSearchBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isTgSearchBlocked = checked
                                    sharedPrefs.edit().putBoolean("tg_search_blocked", checked).apply()
                                    showNotificationToast(context, "টেলিগ্রাম সার্চ", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "টেলিগ্রাম সার্চ লক"
                                infoDialogText = "টেলিগ্রামে নতুন কিছু বা গ্লোবাল সার্চ করা প্রতিরোধ করা হবে।"
                            }
                        )

                        BlockItemRow(
                            title = "টেলিগ্রাম স্টোরি (Telegram Story)",
                            isEnabled = isTgStoryBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isTgStoryBlocked = checked
                                    sharedPrefs.edit().putBoolean("tg_story_blocked", checked).apply()
                                    showNotificationToast(context, "টেলিগ্রাম স্টোরি", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "টেলিগ্রাম স্টোরি লক"
                                infoDialogText = "টেলিগ্রাম ফিডে থাকা স্টোরিগুলোর ভিউ ব্লক করা হবে।"
                            }
                        )

                        BlockItemRow(
                            title = "টেলিগ্রাম সম্পূর্ণ ব্লক করুন",
                            isEnabled = isTgEntireBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isTgEntireBlocked = checked
                                    isTgAppBlocked = checked
                                    isTgSearchBlocked = checked
                                    isTgStoryBlocked = checked
                                    sharedPrefs.edit()
                                        .putBoolean("tg_entire_blocked", checked)
                                        .putBoolean("tg_app_blocked", checked)
                                        .putBoolean("tg_search_blocked", checked)
                                        .putBoolean("tg_story_blocked", checked)
                                        .apply()
                                    showNotificationToast(context, "টেলিগ্রাম সম্পূর্ণ সার্ভিস", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "টেলিগ্রাম কমপ্লিট ব্লক"
                                infoDialogText = "টেলিগ্রামের সকল প্রকার সার্ভিস ও মেসেজিং সাময়িক বন্ধের অনন্য ফিচার।"
                            }
                        )
                    }
                }

                // 5. WHATSAPP PLATFORM CARD
                PlatformBlockCard(
                    title = "WhatsApp",
                    icon = Icons.Default.Chat,
                    iconColor = Color(0xFF25D366),
                    isExpanded = isWaExpanded,
                    onToggleExpand = { isWaExpanded = !isWaExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BlockItemRow(
                            title = "হোয়াটসঅ্যাপ অ্যাপ (WhatsApp App)",
                            isEnabled = isWaAppBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isWaAppBlocked = checked
                                    sharedPrefs.edit().putBoolean("wa_app_blocked", checked).apply()
                                    showNotificationToast(context, "হোয়াটসঅ্যাপ চ্যাটিং", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "হোয়াটসঅ্যাপ অ্যাপ লক"
                                infoDialogText = "হোয়াটসঅ্যাপ চ্যাটিং সেশন নিয়মিত ব্রাউজ বা বারবার ওপেন করা লক করে রাখা।"
                            }
                        )

                        BlockItemRow(
                            title = "হোয়াটসঅ্যাপ স্টোরি (WhatsApp Story)",
                            isEnabled = isWaStoryBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isWaStoryBlocked = checked
                                    sharedPrefs.edit().putBoolean("wa_story_blocked", checked).apply()
                                    showNotificationToast(context, "হোয়াটসঅ্যাপ স্ট্যাটাস/স্টোরি", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "হোয়াটসঅ্যাপ স্ট্যাটাস লক"
                                infoDialogText = "বন্ধুদের দেওয়া চমৎকার সব স্ট্যাটাস/স্টোরিস এর কারণে মনোযোগ চলে যাওয়া এড়াতে পারবেন।"
                            }
                        )

                        BlockItemRow(
                            title = "হোয়াটসঅ্যাপ সম্পূর্ণ ব্লক করুন",
                            isEnabled = isWaEntireBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isWaEntireBlocked = checked
                                    isWaAppBlocked = checked
                                    isWaStoryBlocked = checked
                                    sharedPrefs.edit()
                                        .putBoolean("wa_entire_blocked", checked)
                                        .putBoolean("wa_app_blocked", checked)
                                        .putBoolean("wa_story_blocked", checked)
                                        .apply()
                                    showNotificationToast(context, "হোয়াটসঅ্যাপ সম্পূর্ণ সার্ভিস", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "হোয়াটসঅ্যাপ কমপ্লিট ব্লক"
                                infoDialogText = "হোয়াটসঅ্যাপের সকল মেসেজ প্রাপ্তি ও স্ট্যাটাস আপডেট দেখা পূর্ণাঙ্গ সাময়িক বন্ধ।"
                            }
                        )
                    }
                }

                // 6. MESSENGER PLATFORM CARD
                PlatformBlockCard(
                    title = "Messenger",
                    icon = Icons.Default.Forum,
                    iconColor = Color(0xFF006AFF),
                    isExpanded = isMsExpanded,
                    onToggleExpand = { isMsExpanded = !isMsExpanded }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BlockItemRow(
                            title = "মেসেঞ্জার (Messenger)",
                            isEnabled = isMsAppBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isMsAppBlocked = checked
                                    sharedPrefs.edit().putBoolean("ms_app_blocked", checked).apply()
                                    showNotificationToast(context, "মেসেঞ্জার মেসেজিং", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "মেসেঞ্জার চ্যাট লক"
                                infoDialogText = "মেসেঞ্জার অ্যাপ্লিকেশন ওপেন করা সীমাবদ্ধ থাকবে।"
                            }
                        )

                        BlockItemRow(
                            title = "মেসেঞ্জার স্টোরি (Messenger Story)",
                            isEnabled = isMsStoryBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isMsStoryBlocked = checked
                                    sharedPrefs.edit().putBoolean("ms_story_blocked", checked).apply()
                                    showNotificationToast(context, "মেসেঞ্জার স্টোরি", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "মেসেঞ্জার স্টোরি লক"
                                infoDialogText = "মেসেঞ্জারে বন্ধুদের শেয়ার করা স্টোরিজ বা স্ট্যাটাস আপডেট ব্লক করে একাগ্রতা বাড়ানো হবে।"
                            }
                        )

                        BlockItemRow(
                            title = "মেসেঞ্জার সম্পূর্ণ ব্লক করুন",
                            isEnabled = isMsEntireBlocked,
                            onCheckedChange = { checked ->
                                checkAndSetBlock(checked) {
                                    isMsEntireBlocked = checked
                                    isMsAppBlocked = checked
                                    isMsStoryBlocked = checked
                                    sharedPrefs.edit()
                                        .putBoolean("ms_entire_blocked", checked)
                                        .putBoolean("ms_app_blocked", checked)
                                        .putBoolean("ms_story_blocked", checked)
                                        .apply()
                                    showNotificationToast(context, "মেসেঞ্জার সম্পূর্ণ সার্ভিস", checked)
                                }
                            },
                            onInfoClick = {
                                infoDialogTitle = "মেসেঞ্জার কমপ্লিট ব্লক"
                                infoDialogText = "ফেসবুক মেসেঞ্জারের ক্রমাগত আড্ডা ও নোটিফিকেশন থেকে সুরক্ষিত থাকার অনন্য উপায়।"
                            }
                        )
                    }
                }
            }
        }
    }

    // Restricted Settings Help Dialog
    if (showRestrictedHelp) {
        AlertDialog(
            onDismissRequest = { showRestrictedHelp = false },
            shape = RoundedCornerShape(24.dp),
            title = {
                Text("কিভাবে Restricted Setting ঠিক করবেন?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("অ্যান্ড্রয়েড ১৩+ ভার্সনে নিরাপত্তার কারণে বাইরের অ্যাপে সরাসরি পারমিশন দেওয়া যায় না। এটি ঠিক করতে:", fontSize = 14.sp)
                    Text("১. আপনার ফোনের Settings > Apps-এ যান।", fontSize = 14.sp)
                    Text("২. 'Halal Circle' অ্যাপটি খুঁজে বের করুন।", fontSize = 14.sp)
                    Text("৩. উপরের ডানদিকের তিনটি ডট (⋮) মেনুতে ক্লিক করুন।", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("৪. 'Allow restricted settings' এ ক্লিক করুন।", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    Text("৫. এখন ফিরে এসে আবার পারমিশনটি দিন।", fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRestrictedHelp = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("বুঝেছি", color = Color.White)
                }
            }
        )
    }

    // Modal Details info Alert Dialog
    infoDialogTitle?.let { title ->
        AlertDialog(
            shape = RoundedCornerShape(16.dp),
            onDismissRequest = {
                infoDialogTitle = null
                infoDialogText = null
            },
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextDark
                )
            },
            text = {
                Text(
                    text = infoDialogText ?: "",
                    fontSize = 14.sp,
                    color = TextGray,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        infoDialogTitle = null
                        infoDialogText = null
                    }
                ) {
                    Text(
                        text = "ঠিক আছে",
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    // Rounded corner platform permission request dialog
    if (showPermissionDialog) {
        val isAccessEnabled = run {
            val expectedComponentName = android.content.ComponentName(context, SocialAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            enabledServices?.contains(expectedComponentName.flattenToString()) == true
        }

        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFEF3C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security, 
                            contentDescription = null, 
                            tint = Color(0xFFD97706), 
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text("অনুমতি প্রয়োজন", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (!isAccessEnabled) 
                            "সোশ্যাল মিডিয়া অ্যাপস রিয়েল-টাইমে ব্লক করতে হলে আপনার 'Accessibility Service' সক্রিয় করা প্রয়োজন।\n\n১. নিচে বাটনে ক্লিক করে 'Halal Circle' Accessibility অন করুন।"
                        else 
                            "ব্লকিং পপ-আপ সরাসরি অ্যাপের ওপর দেখানোর জন্য 'Display over other apps' পারমিশনটি অন করুন।",
                        fontSize = 13.sp,
                        color = TextGray,
                        lineHeight = 20.sp
                    )
                    Divider()
                    Text(
                        "নিরবচ্ছিন্ন ব্লকিং নিশ্চিত করতে ব্যাটারি অপ্টিমাইজেশন বন্ধ করার পরামর্শ দেওয়া হলো।",
                        fontSize = 12.sp,
                        color = TextGray,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    TextButton(
                        onClick = { showRestrictedHelp = true },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("'Restricted Setting' সমস্যা হচ্ছে?", color = PrimaryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isAccessEnabled) {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "সেটিংস খোলা যাচ্ছে না।", Toast.LENGTH_LONG).show()
                            }
                        } else if (!Settings.canDrawOverlays(context)) {
                            try {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = android.net.Uri.parse("package:${context.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Overlay সেটিংস খোলা যাচ্ছে না।", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            showPermissionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (!isAccessEnabled) "Accessibility অন করুন" else "Overlay অন করুন", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun PlatformBlockCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleExpand
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(iconColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "বন্ধ করুন" else "খুলুন",
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                    content()
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun BlockItemRow(
    title: String,
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) Color(0xFFFFF1F2) else Color(0xFFFAFAFA)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isEnabled) Color(0xFFFECDD3) else Color(0xFFE5E7EB)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (title.contains("ব্লক করুন")) Icons.Outlined.Block else Icons.Outlined.VideoSettings,
                    contentDescription = null,
                    tint = if (isEnabled) Color(0xFFF43F5E) else TextGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isEnabled) Color(0xFF9F1239) else TextDark
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "সহায়তা তথ্য",
                        tint = TextGray.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFFF43F5E),
                        checkedBorderColor = Color(0xFFF43F5E),
                        uncheckedThumbColor = Color(0xFF9CA3AF),
                        uncheckedTrackColor = Color(0xFFE5E7EB),
                        uncheckedBorderColor = Color(0xFFD1D5DB)
                    ),
                    modifier = Modifier.scaleSwitch()
                )
            }
        }
    }
}

@Composable
private fun Modifier.scaleSwitch(): Modifier {
    return this.padding(vertical = 2.dp)
}

private fun showNotificationToast(context: Context, label: String, isBlocked: Boolean) {
    val stateBengali = if (isBlocked) "লক করা হয়েছে" else "লক মুক্ত করা হয়েছে"
    Toast.makeText(context, "$label $stateBengali", Toast.LENGTH_SHORT).show()
}

@Composable
fun TermsAndPrivacyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White, CircleShape)
                    .border(1.dp, Color(0xFFE5E7EB), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "পিছনে যান",
                    tint = TextDark
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "শর্তাবলী ও প্রাইভেসী পলিসি",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextDark
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "আমাদের মূল দর্শন (Our Vision)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "শয়তানের ধোঁকা ও সোশ্যাল মিডিয়ার আসক্তি থেকে মুসলিম ভাই-বোনদের মূল্যবান সময়কে রক্ষা করে দ্বীনি কাজে সক্রিয় রাখার লক্ষ্যেই 'Halal Circle' সোশ্যাল মিডিয়া ব্লকার ফিচারটি ডেভেলপ করা হয়েছে।",
                        fontSize = 13.sp,
                        color = TextDark,
                        lineHeight = 20.sp
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "১. তথ্য ও গোপনীয়তা সুরক্ষা (Data Privacy)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )
                    Divider(color = Color(0xFFF1F5F9))
                    Text(
                        text = "• শতভাগ অফলাইন ও সুরক্ষিত: আমরা কোনো প্রকার ব্যবহারকারীর ট্র্যাকিং তথ্য, ইন্টারনেটের ইতিহাস বা ব্যক্তিগত ডাটা সংগ্রহ বা সার্ভারে সংরক্ষণ করি না। এটি সম্পূর্ণ অফলাইনে আপনার ফোনেই কাজ করে।\n\n• Usage Stats পারমিশন: বাস্তবসম্মতভাবে অ্যাপস ব্লক করার সুবিধার্থে এই বিশেষ পারমিশনটি প্রয়োজন হয়। এটি শুধুমাত্র কোন সোশ্যাল মিডিয়া অ্যাপটি বর্তমানে চালু আছে তা যাচাই করতে অন-ডিভাইস রান করা হয় এবং কোনো তথ্য বাহিরে যায় না।",
                        fontSize = 12.sp,
                        color = TextGray,
                        lineHeight = 18.sp
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "২. ব্যবহার বিধিসমূহ (Terms of Use)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )
                    Divider(color = Color(0xFFF1F5F9))
                    Text(
                        text = "• তদারকি চালু রাখা: সোশ্যাল মিডিয়া ব্লকার সিস্টেম চালু রাখলে, চিহ্নিত ক্ষতিকারক অ্যাপস ওপেন করামাত্র ৫ সেকেন্ডের সতর্কবার্তা দেখিয়ে আপনাকে হোম স্ক্রিনে ফিরিয়ে আনা হবে।\n\n• ইচ্ছা স্বাধীন নিয়ন্ত্রণ: আপনি যেকোনো সময় সোশ্যাল মিডিয়া ড্যাশবোর্ড থেকে ব্লক ফিচার নিয়ন্ত্রণ করতে এবং প্রয়োজন অনুসারে চালু অথবা বন্ধ রাখতে পারবেন।",
                        fontSize = 12.sp,
                        color = TextGray,
                        lineHeight = 18.sp
                    )
                }
            }

            Text(
                text = "আল্লাহ আমাদের সময়কে বারাকাহ দান করুন এবং নেক আমল করার তৌফিক দান করুন। আমিন।",
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = TextGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            )
        }
    }
}
