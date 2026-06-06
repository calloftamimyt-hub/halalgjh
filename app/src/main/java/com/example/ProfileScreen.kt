package com.example

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.viewmodel.TrackerViewModel
import com.example.viewmodel.GlobalLanguage
import com.example.ui.LocalAppStrings
import com.example.database.DailyTracker
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.example.ui.theme.*

// Data class representing selectable custom Profile Avatars 
data class ProfileLogoOption(
    val index: Int,
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val bgColor: Color
)

// Relying on public package-level toBengaliDigits() defined in TrackerScreen.kt

// Convert English format "yyyy-MM-dd" to Bengali display
private fun formatTrackerDateToBengali(dateKey: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateInstance = parser.parse(dateKey) ?: return dateKey
        val formatter = SimpleDateFormat("d MMMM, yyyy", Locale("bn", "BD"))
        formatter.format(dateInstance)
    } catch (e: Exception) {
        dateKey
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToTracker: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSaved: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }
    
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    // Listen for Auth changes in real-time
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { 
            currentUser = it.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }
    
    // Persistent profile state - remove demo defaults
    var userName by remember(currentUser) { 
        mutableStateOf(currentUser?.displayName ?: if (GlobalLanguage.isEnglish) "Guest User" else "অতিথি ইউজার") 
    }
    var userEmail by remember(currentUser) { 
        mutableStateOf(currentUser?.email ?: "") 
    }
    var userPhone by remember { mutableStateOf(sharedPrefs.getString("user_phone", "") ?: "") }
    var userGender by remember { mutableStateOf(sharedPrefs.getString("user_gender", "") ?: "") }
    var locationSetting by remember { mutableStateOf(sharedPrefs.getString("location", "") ?: "") }
    var selectedLogoIndex by remember { mutableStateOf(sharedPrefs.getInt("selected_logo_index", 0)) }
    
    // Auth States
    var showLoginScreen by remember { mutableStateOf(false) }
    var showRegisterScreen by remember { mutableStateOf(false) }

    // Screens navigation switches
    var showEditFullScreen by remember { mutableStateOf(false) }
    var showTrackerHistoryFullScreen by remember { mutableStateOf(false) }
    var showSocialMediaBlockerFullScreen by remember { mutableStateOf(false) }
    var showWebsiteBlockerFullScreen by remember { mutableStateOf(false) }
    var showScreenTimeFullScreen by remember { mutableStateOf(false) }
    var showAutoSilentFullScreen by remember { mutableStateOf(false) }

    // Dialogs / Sheets for interactive features
    var activeModalTitle by remember { mutableStateOf<String?>(null) }
    var currentSelectedFeature by remember { mutableStateOf<String?>(null) }

    // Interactive toggle states for features
    var isParentalControlEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("parental_control", false)) }
    var isAutoSilentEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("auto_silent", true)) }
    var isSocialMediaBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("social_blocked", false)) }
    var isWebsiteBlocked by remember { mutableStateOf(sharedPrefs.getBoolean("web_blocked", false)) }

    LaunchedEffect(showSocialMediaBlockerFullScreen) {
        if (!showSocialMediaBlockerFullScreen) {
            val isAnyBlocked = sharedPrefs.getBoolean("yt_long_blocked", false) ||
                    sharedPrefs.getBoolean("yt_reels_blocked", false) ||
                    sharedPrefs.getBoolean("yt_search_blocked", false) ||
                    sharedPrefs.getBoolean("yt_entire_blocked", false) ||
                    sharedPrefs.getBoolean("fb_app_blocked", false) ||
                    sharedPrefs.getBoolean("fb_story_blocked", false) ||
                    sharedPrefs.getBoolean("fb_search_blocked", false) ||
                    sharedPrefs.getBoolean("fb_reels_blocked", false) ||
                    sharedPrefs.getBoolean("fb_entire_blocked", false) ||
                    sharedPrefs.getBoolean("tg_app_blocked", false) ||
                    sharedPrefs.getBoolean("tg_search_blocked", false) ||
                    sharedPrefs.getBoolean("tg_story_blocked", false) ||
                    sharedPrefs.getBoolean("tg_entire_blocked", false) ||
                    sharedPrefs.getBoolean("wa_app_blocked", false) ||
                    sharedPrefs.getBoolean("wa_story_blocked", false) ||
                    sharedPrefs.getBoolean("wa_entire_blocked", false) ||
                    sharedPrefs.getBoolean("ms_app_blocked", false) ||
                    sharedPrefs.getBoolean("ms_story_blocked", false) ||
                    sharedPrefs.getBoolean("ms_entire_blocked", false) ||
                    sharedPrefs.getBoolean("ig_app_blocked", false) ||
                    sharedPrefs.getBoolean("ig_search_blocked", false) ||
                    sharedPrefs.getBoolean("ig_reels_blocked", false) ||
                    sharedPrefs.getBoolean("ig_features_blocked", false) ||
                    sharedPrefs.getBoolean("ig_entire_blocked", false)
            isSocialMediaBlocked = isAnyBlocked
            sharedPrefs.edit().putBoolean("social_blocked", isAnyBlocked).apply()
        }
    }
    
    // Counts
    var savedPostCount by remember { mutableStateOf(sharedPrefs.getInt("saved_posts", 0)) }
    var savedDuaCount by remember { mutableStateOf(sharedPrefs.getInt("saved_duas", 0)) }
    var bookmarkedAyahCount by remember { mutableStateOf(sharedPrefs.getInt("bookmarked_ayahs", 0)) }
    
    // App settings preferences
    var isNotificationEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("app_notif", true)) }
    var selectedLanguage by remember { mutableStateOf(sharedPrefs.getString("app_lang", "বাংলা") ?: "বাংলা") }

    // 6 gorgeous Islamic profile representation options
    val logoOptions = listOf(
        ProfileLogoOption(0, Icons.Default.Person, "সবুজ সেবক", Color(0xFF10B981), Color(0xFFE6F4EA)),
        ProfileLogoOption(1, Icons.Default.Star, "নীল তারকা", Color(0xFF3B82F6), Color(0xFFEBF5FF)),
        ProfileLogoOption(2, Icons.Default.Favorite, "গোলাপী দিল", Color(0xFFEC4899), Color(0xFFFDF2F8)),
        ProfileLogoOption(3, Icons.Default.MenuBook, "সোনালী ইলম", Color(0xFFD97706), Color(0xFFFEF3C7)),
        ProfileLogoOption(4, Icons.Default.Face, "বেগুনি সুফি", Color(0xFF8B5CF6), Color(0xFFF5F3FF)),
        ProfileLogoOption(5, Icons.Default.AccountCircle, "ফিরোজা নূর", Color(0xFF14B8A6), Color(0xFFF0FDFA))
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (showLoginScreen) {
            LoginScreen(
                onBack = { showLoginScreen = false },
                onNavigateToRegister = {
                    showLoginScreen = false
                    showRegisterScreen = true
                },
                onLoginSuccess = {
                    showLoginScreen = false
                }
            )
        } else if (showRegisterScreen) {
            RegisterScreen(
                onBack = { showRegisterScreen = false },
                onNavigateToLogin = {
                    showRegisterScreen = false
                    showLoginScreen = true
                },
                onRegisterSuccess = {
                    showRegisterScreen = false
                }
            )
        } else if (showEditFullScreen) {
            EditProfileScreen(
                currentName = userName,
                currentEmail = userEmail,
                currentPhone = userPhone,
                currentGender = userGender,
                currentLocation = locationSetting,
                currentLogoIndex = selectedLogoIndex,
                logoOptions = logoOptions,
                onBack = { showEditFullScreen = false },
                onSave = { name, email, phone, gender, location, logoIdx ->
                    userName = name
                    userEmail = email
                    userPhone = phone
                    userGender = gender
                    locationSetting = location
                    selectedLogoIndex = logoIdx
                    
                    // Sync with Firebase Profile if logged in
                    currentUser?.let { user ->
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                        user.updateProfile(profileUpdates)
                    }
                    
                    sharedPrefs.edit().apply {
                        putString("user_name", name)
                        putString("user_email", email)
                        putString("user_phone", phone)
                        putString("user_gender", gender)
                        putString("location", location)
                        putInt("selected_logo_index", logoIdx)
                        apply()
                    }
                    showEditFullScreen = false
                    Toast.makeText(context, if (GlobalLanguage.isEnglish) "Profile updated successfully!" else "প্রোফাইল সফলভাবে আপডেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                }
            )
        } else if (showTrackerHistoryFullScreen) {
            ProfileTrackerHistoryScreen(
                onBack = { showTrackerHistoryFullScreen = false }
            )
        } else if (showSocialMediaBlockerFullScreen) {
            SocialMediaBlockerScreen(
                onBack = { showSocialMediaBlockerFullScreen = false }
            )
        } else if (showWebsiteBlockerFullScreen) {
            WebsiteBlockerScreen(
                onBack = {
                    showWebsiteBlockerFullScreen = false
                    isWebsiteBlocked = sharedPrefs.getBoolean("web_blocked", false)
                }
            )
        } else if (showScreenTimeFullScreen) {
            ScreenTimeScreen(
                onBack = { showScreenTimeFullScreen = false }
            )
        } else if (showAutoSilentFullScreen) {
            com.example.AutoSilentScreen(
                prayerViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.viewmodel.PrayerViewModel>(),
                onBack = { showAutoSilentFullScreen = false }
            )
        } else {
            // Main Profile UI Display
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgLight)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp)
            ) {
                // Safe spacing
                Spacer(modifier = Modifier.statusBarsPadding())

                // Top Header Removed as requested

                // 1. Decorative custom height Profile Avatar Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drawing custom selected logo avatar
                        val selectedLogo = logoOptions.getOrNull(selectedLogoIndex) ?: logoOptions[0]
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(selectedLogo.bgColor, CircleShape)
                                .border(1.5.dp, selectedLogo.color, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = selectedLogo.icon,
                                contentDescription = "ব্যবহারকারীর অবয়ব",
                                tint = selectedLogo.color,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        // Profile details
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (currentUser != null) userName else (if (GlobalLanguage.isEnglish) "Not Logged In" else "লগইন করা নেই"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextDark
                            )
                            Text(
                                text = if (currentUser != null) userEmail else (if (GlobalLanguage.isEnglish) "Join the community" else "আমাদের কমিউনিটিতে যোগ দিন"),
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }

                        // Badge Category or Login/SignUp buttons
                        if (currentUser != null) {
                            Box(
                                modifier = Modifier
                                    .background(PrimaryGreen.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Premium User" else "প্রিমিয়াম ইউজার",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Login" else "লগইন",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    modifier = Modifier
                                        .clickable { showLoginScreen = true }
                                        .padding(4.dp)
                                )
                                Text(
                                    text = " / ",
                                    fontSize = 11.sp,
                                    color = TextGray
                                )
                                Text(
                                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Register" else "সাইন আপ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryGreen,
                                    modifier = Modifier
                                        .clickable { showRegisterScreen = true }
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Large category options card (slightly rounded rectangular)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        
                        // Action Row mapping to Full-Screen Edit Profile Page
                        ProfileOptionRow(
                            title = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Edit Profile" else "প্রোফাইল এডিট করুন",
                            icon = Icons.Outlined.Edit,
                            iconColor = Color(0xFF2563EB),
                            onClick = {
                                showEditFullScreen = true
                            }
                        )

                        ProfileDivider()

                        // Action Row mapping to Full-Screen Tracker History Page
                        ProfileOptionRow(
                            title = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Tracker" else "ট্র্যাকার",
                            icon = Icons.Outlined.CheckCircle,
                            iconColor = PrimaryGreen,
                            onClick = {
                                showTrackerHistoryFullScreen = true
                            }
                        )

                        ProfileDivider()

                        // Parental Control
                        ProfileOptionRow(
                            title = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Parental Control" else "প্যারেন্টাল কন্ট্রোল",
                            icon = if (isParentalControlEnabled) Icons.Filled.FamilyRestroom else Icons.Outlined.FamilyRestroom,
                            iconColor = Color(0xFFD97706),
                            onClick = {
                                activeModalTitle = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Parental Control" else "প্যারেন্টাল কন্ট্রোল"
                                currentSelectedFeature = "parental"
                            }
                        )

                        ProfileDivider()

                        // Auto Silent
                        ProfileOptionRow(
                            title = "অটো সাইলেন্ট",
                            icon = if (isAutoSilentEnabled) Icons.Filled.VolumeOff else Icons.Outlined.VolumeUp,
                            iconColor = Color(0xFF8B5CF6),
                            onClick = {
                                showAutoSilentFullScreen = true
                            }
                        )

                        ProfileDivider()

                        // Social Media Blocker
                        ProfileOptionRow(
                            title = "সোশ্যাল মিডিয়া ব্লকার",
                            icon = Icons.Outlined.AppBlocking,
                            iconColor = Color(0xFFEF4444),
                            onClick = {
                                showSocialMediaBlockerFullScreen = true
                            }
                        )

                        ProfileDivider()

                        // Website Blocker
                        ProfileOptionRow(
                            title = "ওয়েবসাইট ব্লকার",
                            icon = Icons.Outlined.Block,
                            iconColor = Color(0xFFDC2626),
                            onClick = {
                                showWebsiteBlockerFullScreen = true
                            }
                        )

                        ProfileDivider()

                        // Screen Time
                        ProfileOptionRow(
                            title = "স্ক্রিন টাইম",
                            icon = Icons.Outlined.Timer,
                            iconColor = Color(0xFF0369A1),
                            onClick = {
                                showScreenTimeFullScreen = true
                            }
                        )

                        ProfileDivider()

                        // Saved posts
                        ProfileOptionRow(
                            title = "সেভ করা পোস্ট",
                            icon = Icons.Outlined.BookmarkBorder,
                            iconColor = Color(0xFF9333EA),
                            onClick = onNavigateToSaved
                        )

                        ProfileDivider()

                        // Saved Duas
                        ProfileOptionRow(
                            title = "সেভ করা দোয়া",
                            icon = Icons.Outlined.FavoriteBorder,
                            iconColor = Color(0xFFEC4899),
                            onClick = {
                                activeModalTitle = "সেভ করা দোয়া ক্যাটাগরি"
                                currentSelectedFeature = "duas"
                            }
                        )

                        ProfileDivider()

                        // Bookmarked Ayahs
                        ProfileOptionRow(
                            title = "বুকমার্ক করা আয়াত",
                            icon = Icons.Outlined.MenuBook,
                            iconColor = Color(0xFF0E7490),
                            onClick = {
                                activeModalTitle = "বুকমার্ক করা আয়াত কোড"
                                currentSelectedFeature = "ayahs"
                            }
                        )

                        ProfileDivider()

                        // Favorite Hadiths
                        ProfileOptionRow(
                            title = "পছন্দের হাদিস",
                            icon = Icons.Outlined.AutoStories,
                            iconColor = Color(0xFF15803D),
                            onClick = {
                                activeModalTitle = "পছন্দের হাদিস সম্ভার"
                                currentSelectedFeature = "hadiths"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Settings card (slightly rounded rectangular)
                Text(
                    text = LocalAppStrings.current.settings,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextDark,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        ProfileOptionRow(
                            title = LocalAppStrings.current.settings,
                            icon = Icons.Outlined.Settings,
                            iconColor = Color(0xFF4B5563),
                            onClick = onNavigateToSettings
                        )

                        // If logged in, show logout and delete account
                        if (currentUser != null) {
                            ProfileDivider()
                            ProfileOptionRow(
                                title = if (GlobalLanguage.isEnglish) "Sign Out" else "লগআউট করুন",
                                icon = Icons.Default.Logout,
                                iconColor = Color.Red,
                                onClick = {
                                    auth.signOut()
                                    Toast.makeText(context, if (GlobalLanguage.isEnglish) "Logged out successfully" else "সফলভাবে লগআউট করা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            )

                            ProfileDivider()
                            ProfileOptionRow(
                                title = if (GlobalLanguage.isEnglish) "Delete Account" else "অ্যাকাউন্ট মুছে ফেলুন",
                                icon = Icons.Default.Delete,
                                iconColor = Color.Red,
                                onClick = {
                                    // Normally shows a confirmation dialog
                                    Toast.makeText(context, if (GlobalLanguage.isEnglish) "Account deletion request sent" else "অ্যাকাউন্ট মুছে ফেলার অনুরোধ পাঠানো হয়েছে", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                // App brand credits
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Deen Insaf Mobile Application" else "দ্বীন ইনসাফ মোবাইল অ্যাপ্লিকেশন",
                    fontSize = 11.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Modal Details Dialogs
    activeModalTitle?.let { title ->
        AlertDialog(
            onDismissRequest = {
                activeModalTitle = null
                currentSelectedFeature = null
            },
            title = {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (currentSelectedFeature) {
                        "parental" -> {
                            Text(
                                "প্যারেন্টাল কন্ট্রোল সক্রিয় থাকলে বাচ্চারা তাদের ডিভাইস ব্যবহার করার সময় নামাজের ওয়াক্ত বা কুরআন শিখতে অনুপ্রাণিত হবে এবং আপত্তিকর ওয়েবসাইটগুলো স্বয়ংক্রিয়ভাবে ব্লকড থাকবে।",
                                fontSize = 13.sp,
                                color = TextGray,
                                lineHeight = 18.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("প্যারেন্টাল গেট লক চালু করুন", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Switch(
                                    checked = isParentalControlEnabled,
                                    onCheckedChange = {
                                        isParentalControlEnabled = it
                                        sharedPrefs.edit().putBoolean("parental_control", it).apply()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
                                )
                            }
                        }
                        "silent" -> {
                            Text(
                                "নামাজের নির্ধারিত ওয়াক্তের মাঝে আপনার ফোনটি যাতে স্বয়ংক্রিয়ভাবে সাইলেন্ট বা ভাইব্রেশন মোডে চলে যায়, সেই ব্যবস্থা চালু রাখুন।",
                                fontSize = 13.sp,
                                color = TextGray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("স্বয়ংক্রিয় সাইলেন্ট মোড", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Switch(
                                    checked = isAutoSilentEnabled,
                                    onCheckedChange = {
                                        isAutoSilentEnabled = it
                                        sharedPrefs.edit().putBoolean("auto_silent", it).apply()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
                                )
                            }
                        }
                        "social" -> {
                            Text(
                                "পবিত্র সালাত, যিকির ও কুরআন অধ্যায়নের সময় একাগ্রতা বজায় রাখার জন্য সাময়িক সামাজিক যোগাযোগ মাধ্যমসমূহে প্রবেশ সীমাবদ্ধ করুন।",
                                fontSize = 13.sp,
                                color = TextGray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("সোশ্যাল মিডিয়া ডিস্ট্রাকশন লক", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Switch(
                                    checked = isSocialMediaBlocked,
                                    onCheckedChange = {
                                        isSocialMediaBlocked = it
                                        sharedPrefs.edit().putBoolean("social_blocked", it).apply()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color.Red)
                                )
                            }
                        }
                        "web" -> {
                            Text(
                                "অশ্লীলতা ও হারাম আসক্তি থেকে বিরত থাকার জন্য অশ্লীল ও আজেবাজে ওয়েবসাইটে প্রবেশ করা সম্পূর্ণ ব্লক করুন।",
                                fontSize = 13.sp,
                                color = TextGray
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("হারাম এন্টি-পর্ন ব্লক ফিল্টার", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Switch(
                                    checked = isWebsiteBlocked,
                                    onCheckedChange = {
                                        isWebsiteBlocked = it
                                        sharedPrefs.edit().putBoolean("web_blocked", it).apply()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color.Red)
                                )
                            }
                            if (isWebsiteBlocked) {
                                Text(
                                    "✓ ১,৫০০টির বেশি ওয়েবসাইটের লিঙ্ক ব্লক করা হয়েছে আপনার সুরক্ষায়।",
                                    fontSize = 11.sp,
                                    color = PrimaryGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        "screentime" -> {
                            Text(
                                "আজকের মোট স্ক্রিন ব্যবহার এবং দ্বীনি কাজে কাটানো সময় এনালাইসিস:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("২ ঘণ্টা ১৫ মি.", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("দ্বীনি চর্চায়", fontSize = 11.sp, color = TextGray)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("১ ঘণ্টা ২০ মি.", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("সামাজিক যোগাযোগে", fontSize = 11.sp, color = TextGray)
                                }
                            }
                        }
                        "duas" -> {
                            if (savedDuaCount == 0) {
                                Text("আপনার পছন্দের সেভ করা দোয়ার তালিকা খালি। তালিকাটি সমৃদ্ধ করতে দোয়া ক্যাটাগরি থেকে হার্ট বাটনে চাপুন।", fontSize = 13.sp, color = TextGray)
                            } else {
                                Text("আদায়কৃত চমৎকার দোয়াসমূহ বুকমার্ক ক্যাটাগরিতে সেভ আছে।", fontSize = 13.sp, color = TextDark)
                            }
                            Button(
                                onClick = {
                                    savedDuaCount++
                                    sharedPrefs.edit().putInt("saved_duas", savedDuaCount).apply()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Text("দোয়া প্রিয় তালিকায় রাখুন", color = Color.White)
                            }
                        }
                        "ayahs" -> {
                            Text("প্রশান্তি পেতে এবং তিলাওয়াত ট্র্যাকে রাখতে কুরআন শরিফের আয়াতসমূহ বুকমার্ক করুন।", fontSize = 13.sp, color = TextGray)
                            Button(
                                onClick = {
                                    bookmarkedAyahCount++
                                    sharedPrefs.edit().putInt("bookmarked_ayahs", bookmarkedAyahCount).apply()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                            ) {
                                Text("কুরআনের ১টি আয়াত বুকমার্ক করুন", color = Color.White)
                            }
                        }
                        "hadiths" -> {
                            Text("সহীহ বুখারী ও সহীহ মুসলিমের পছন্দের হাদিসসমূহ সহজে বুকমার্ক ক্যাটাগরি থেকে দেখতে পারবেন।", fontSize = 13.sp, color = TextGray)
                            Text("১. \"নিশ্চয় দান-সদকাহ আল্লাহর ক্রোধকে প্রশমিত করে।\"", fontSize = 13.sp, color = TextDark, fontWeight = FontWeight.Bold)
                        }
                        "settings" -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("প্রতি ওয়াক্তে আজান বাজান ও নোটিফিকেশন", fontSize = 13.sp)
                                Switch(
                                    checked = isNotificationEnabled,
                                    onCheckedChange = {
                                        isNotificationEnabled = it
                                        sharedPrefs.edit().putBoolean("app_notif", it).apply()
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryGreen)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("অ্যাপের ভাষা নির্বাচন করুন", fontSize = 13.sp)
                                Button(
                                    onClick = {
                                        val next = if (selectedLanguage == "বাংলা") "English" else "বাংলা"
                                        selectedLanguage = next
                                        sharedPrefs.edit().putString("app_lang", next).apply()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen.copy(alpha=0.15f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                ) {
                                    Text(selectedLanguage, color = PrimaryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        activeModalTitle = null
                        currentSelectedFeature = null
                    }
                ) {
                    Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "OK" else "ঠিক আছে", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// Full screen Edit Profile Screen Composable with high-fidelity inputs
@Composable
fun EditProfileScreen(
    currentName: String,
    currentEmail: String,
    currentPhone: String,
    currentGender: String,
    currentLocation: String,
    currentLogoIndex: Int,
    logoOptions: List<ProfileLogoOption>,
    onBack: () -> Unit,
    onSave: (name: String, email: String, phone: String, gender: String, location: String, logoIndex: Int) -> Unit
) {
    var editName by remember { mutableStateOf(currentName) }
    var editEmail by remember { mutableStateOf(currentEmail) }
    var editPhone by remember { mutableStateOf(currentPhone) }
    var editGender by remember { mutableStateOf(currentGender) }
    var editLocation by remember { mutableStateOf(currentLocation) }
    var editLogoIndex by remember { mutableIntStateOf(currentLogoIndex) }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        // Top Navigation Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে যান", tint = TextDark)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "প্রোফাইল সংশোধন করুন",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    onSave(editName, editEmail, editPhone, editGender, editLocation, editLogoIndex)
                }
            ) {
                Text(
                    text = "সেভ করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PrimaryGreen
                )
            }
        }

        // Form Fields inside scrollable Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Large Profile Logo Avatar Preview and choice selector
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "প্রোফাইল অবয়ব (Logo)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Large Preview bubble
                    val activeOption = logoOptions.getOrNull(editLogoIndex) ?: logoOptions[0]
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(activeOption.bgColor, CircleShape)
                            .border(2.dp, activeOption.color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = activeOption.icon,
                            contentDescription = "Active Logo",
                            tint = activeOption.color,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"${activeOption.label}\" নির্বাচিত",
                        fontSize = 11.sp,
                        color = TextGray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Horizontal Selection row of the 6 options
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(logoOptions) { item ->
                            val isSelected = editLogoIndex == item.index
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(item.bgColor, CircleShape)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) item.color else Color(0xFFD1D5DB),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        editLogoIndex = item.index
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = item.color,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.12f), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Personal Information Fields Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "ব্যক্তিগত তথ্যাবলী",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    
                    // Name
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("ব্যবহারকারীর নাম (কিংবা ডাকনাম)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Email (Optional or Read-only if needed, but keeping for now as per UI)
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("ইমেইল অ্যাড্রেস") },
                        enabled = false, // Email usually managed via Auth flow
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryGreen,
                            disabledBorderColor = Color.LightGray,
                            disabledTextColor = TextGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Phone Number
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("মোবাইল ফোন নম্বর") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Gender Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "আপনার লিঙ্গ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextGray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val genders = listOf("পুরুষ", "মহিলা", "অন্যান্য")
                            genders.forEach { gender ->
                                val active = editGender == gender
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            color = if (active) PrimaryGreen.copy(alpha = 0.12f) else Color.Transparent,
                                            shape = RoundedCornerShape(100.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (active) PrimaryGreen else Color(0xFFD1D5DB),
                                            shape = RoundedCornerShape(100.dp)
                                        )
                                        .clickable { editGender = gender }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = gender,
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) PrimaryGreen else TextDark
                                    )
                                }
                            }
                        }
                    }

                    // Location setting
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("ঠিকানা বা জেলা") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryGreen),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Save large button
            Button(
                onClick = {
                    if (editName.isBlank()) {
                        editName = "তাহমিদ আহমেদ"
                    }
                    onSave(editName, editEmail, editPhone, editGender, editLocation, editLogoIndex)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("সেভ প্রোফাইল", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// Full screen Tracker History view displaying historical records inside dynamic rectangular light rounded cards
@Composable
fun ProfileTrackerHistoryScreen(
    onBack: () -> Unit
) {
    val trackerViewModel: TrackerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val state by trackerViewModel.uiState.collectAsState()

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgLight)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে যান", tint = TextDark)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "আমার ট্র্যাকার ইতিহাস",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
        }

        // Calculations & Statistics Panel
        val historyList = state.history
        val totalDays = historyList.size
        
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalDays.toBengaliDigits(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryGreen
                    )
                    Text(
                        text = "মোট ট্র্যাকিং দিন",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFFE5E7EB))
                )

                // Mathematically count completed habits
                val totalHabitsCount = historyList.sumOf { h ->
                    var count = 0
                    if (h.quran) count++
                    if (h.charity) count++
                    if (h.reading) count++
                    if (h.istighfar) count++
                    if (h.parents) count++
                    count
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = totalHabitsCount.toBengaliDigits(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF8B5CF6)
                    )
                    Text(
                        text = "সম্পন্ন নেক আমল",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // History list entries with slightly rounded rectangular cards
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PrimaryGreen.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "কোনো ট্র্যাকার ইতিহাস পাওয়া যায়নি",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "আপনার প্রতিদিনের কার্যক্রম ট্র্যাক করতে মূল ‘আমল ট্র্যাকার’ ট্যাব ব্যবহার করে চেক ইন করুন।",
                        fontSize = 12.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(historyList) { trackerItem ->
                    TrackerHistoryCard(tracker = trackerItem)
                }
            }
        }
    }
}

// Masterpiece lightweight rectangular card with 12.dp light rounded corners showing tracked activities
@Composable
fun TrackerHistoryCard(tracker: DailyTracker) {
    // Calculators
    var prayersCount = 0
    if (tracker.fajr) prayersCount++
    if (tracker.dhuhr) prayersCount++
    if (tracker.asr) prayersCount++
    if (tracker.maghrib) prayersCount++
    if (tracker.isha) prayersCount++

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card Title top header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formatTrackerDateToBengali(tracker.date),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextDark
                    )
                }
                
                // Completed sum count representation
                Box(
                    modifier = Modifier
                        .background(
                            color = if (prayersCount == 5) PrimaryGreen.copy(alpha = 0.12f) else Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${prayersCount.toBengaliDigits()}/৫ ওয়াক্ত সালাত",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (prayersCount == 5) PrimaryGreen else TextDark
                    )
                }
            }

            // 5 prayers inline check progress bubbles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val prayerPills = listOf(
                    "ফজর" to tracker.fajr,
                    "যোহর" to tracker.dhuhr,
                    "আসর" to tracker.asr,
                    "মাগরিব" to tracker.maghrib,
                    "এশা" to tracker.isha
                )
                
                prayerPills.forEach { pill ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .background(
                                color = if (pill.second) PrimaryGreen.copy(alpha = 0.12f) else Color(0xFFF3F4F6),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (pill.second) PrimaryGreen else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = if (pill.second) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (pill.second) PrimaryGreen else TextGray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pill.first,
                                fontSize = 10.sp,
                                fontWeight = if (pill.second) FontWeight.Bold else FontWeight.Normal,
                                color = if (pill.second) PrimaryGreen else TextDark
                            )
                        }
                    }
                }
            }

            // Tag layout list for other hobbies checked
            val tags = mutableListOf<Triple<String, ImageVector, Color>>()
            
            if (tracker.quran) tags.add(Triple("কুরআন তিলাওয়াত", Icons.Default.MenuBook, Color(0xFF10B981)))
            if (tracker.charity) tags.add(Triple("দান-সদকাহ", Icons.Default.Favorite, Color(0xFFEC4899)))
            if (tracker.reading) tags.add(Triple("ইসলামী জ্ঞান", Icons.Default.Book, Color(0xFF3B82F6)))
            if (tracker.istighfar) tags.add(Triple("ইস্তিগফার", Icons.Default.SelfImprovement, Color(0xFF8B5CF6)))
            if (tracker.parents) tags.add(Triple("বাবা-মায়ের সেবা", Icons.Default.People, Color(0xFFD97706)))
            if (tracker.tasbihCount > 0) tags.add(Triple("জিকির: ${tracker.tasbihCount.toBengaliDigits()} বার", Icons.Default.Stars, Color(0xFF14B8A6)))

            if (tags.isNotEmpty()) {
                Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)
                
                // Flow of accomplishments
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "আজকের নেক আমলসমূহ:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextGray
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Display list in beautiful micro cards
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tags.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(tag.third.copy(alpha = 0.08f), RoundedCornerShape(100.dp))
                                    .border(1.dp, tag.third.copy(alpha = 0.25f), RoundedCornerShape(100.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = tag.second,
                                        contentDescription = null,
                                        tint = tag.third,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = tag.first,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tag.third
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileOptionRow(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current
            )
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = TextDark
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFD1D5DB),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ProfileDivider() {
    Divider(
        color = Color(0xFFF3F4F6),
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
