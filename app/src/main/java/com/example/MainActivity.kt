package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PrayerViewModel
import com.example.viewmodel.toBengali
import com.example.viewmodel.SettingsViewModel
import com.example.viewmodel.GlobalLanguage
import com.example.viewmodel.AppLanguage
import com.example.ui.LocalAppStrings
import com.example.ui.theme.*
import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.toArgb
import com.google.accompanist.permissions.*
import com.google.firebase.auth.FirebaseAuth


class MainActivity : ComponentActivity() {
    var interceptedPlatformName by mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null && intent.getBooleanExtra("action_intercept_social", false)) {
            interceptedPlatformName = intent.getStringExtra("intercepted_platform_name")
        } else if (intent != null && intent.action == Intent.ACTION_MAIN) {
            // If the user manually opens the app, clear any previous interception state
            interceptedPlatformName = null
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val activePlatform = interceptedPlatformName

                // Handle starting/stopping service reactively based on preferences
                val sharedPrefs = remember { context.getSharedPreferences("profile_prefs", MODE_PRIVATE) }
                val isSocialBlocked = sharedPrefs.getBoolean("social_blocked", false)
                
                val settingsViewModel: com.example.viewmodel.SettingsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return com.example.viewmodel.SettingsViewModel(context) as T
                    }
                })
                val appLanguage by settingsViewModel.language.collectAsState()
                val strings = com.example.ui.getString(appLanguage)

                CompositionLocalProvider(com.example.ui.LocalAppStrings provides strings) {
                    LaunchedEffect(isSocialBlocked) {
                        if (isSocialBlocked && SocialBlockerService.isPermissionGranted(context)) {
                            SocialBlockerService.startService(context)
                        } else {
                            SocialBlockerService.stopService(context)
                        }
                    }

                    if (activePlatform != null) {
                        SocialBlockerOverlay(
                            platformName = activePlatform,
                            onDismissToHome = {
                                interceptedPlatformName = null
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(homeIntent)
                            }
                        )
                    } else {
                        val viewModel: PrayerViewModel = viewModel()
                        val state by viewModel.state.collectAsState()

                        val permissions = mutableListOf(
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                        }

                        val multiplePermissionsState = rememberMultiplePermissionsState(permissions)
                        var selectedTab by remember { mutableStateOf("home") }

                        val view = LocalView.current
                        val isDarkStatusBar = selectedTab == "video" || selectedTab == "create"
                        
                        SideEffect {
                            val window = (view.context as Activity).window
                            if (isDarkStatusBar) {
                                window.statusBarColor = android.graphics.Color.BLACK
                                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                            } else {
                                // Ensure light status icons and transparent bar for main sections
                                window.statusBarColor = android.graphics.Color.TRANSPARENT
                                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                            }
                        }

                        LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
                            if (multiplePermissionsState.allPermissionsGranted) {
                                viewModel.startLocationUpdates(context)
                            } else {
                                viewModel.setPermissionDenied()
                            }
                        }

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = if (isDarkStatusBar) Color.Black else BgLight,
                            topBar = { if (!isDarkStatusBar) GlassStatusBarHeader() },
                            bottomBar = { AppBottomNavigation(selectedTab) { selectedTab = it } }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(
                                        if (isDarkStatusBar) {
                                            // Skip top padding to ensure video fills the status bar area
                                            Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                                        } else {
                                            Modifier.padding(innerPadding)
                                        }
                                    )
                            ) {
                                if (multiplePermissionsState.allPermissionsGranted || !state.isAutoLocation) {
                                    if (selectedTab == "home") {
                                        HomeScreen(
                                            state = state,
                                            onToggleAlarm = { viewModel.toggleAlarm(context, it) },
                                            onNavigateToTracker = { selectedTab = "tracker" },
                                            onNavigateToQuran = { selectedTab = "quran" },
                                            onNavigateToLocation = { selectedTab = "location" }
                                        )
                                    } else if (selectedTab == "location") {
                                        LocationSelectionScreen(
                                            viewModel = viewModel,
                                            onBack = { selectedTab = "home" }
                                        )
                                    } else if (selectedTab == "quran") {
                                        QuranScreen(onBack = { selectedTab = "home" })
                                    } else if (selectedTab == "video") {
                                        VideoScreen()
                                    } else if (selectedTab == "create") {
                                        val currentUser = FirebaseAuth.getInstance().currentUser
                                        if (currentUser == null) {
                                            var authTab by remember { mutableStateOf("login") }
                                            if (authTab == "login") {
                                                LoginScreen(
                                                    onBack = { selectedTab = "home" },
                                                    onNavigateToRegister = { authTab = "register" },
                                                    onLoginSuccess = { selectedTab = "create" }
                                                )
                                            } else {
                                                RegisterScreen(
                                                    onBack = { selectedTab = "home" },
                                                    onNavigateToLogin = { authTab = "login" },
                                                    onRegisterSuccess = { selectedTab = "create" }
                                                )
                                            }
                                        } else {
                                            CreateVideoScreen(
                                                onUploadSuccess = { selectedTab = "video" },
                                                onBack = { selectedTab = "home" }
                                            )
                                        }
                                    } else if (selectedTab == "tracker") {
                                        TrackerScreen()
                                    } else if (selectedTab == "profile") {
                                        ProfileScreen(
                                            onNavigateToTracker = { selectedTab = "tracker" },
                                            onNavigateToSettings = { selectedTab = "settings" }
                                        )
                                    } else if (selectedTab == "settings") {
                                        SettingsScreen(
                                            viewModel = settingsViewModel,
                                            onBack = { selectedTab = "profile" }
                                        )
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text("শীঘ্রই আসছে...", fontSize = 24.sp, color = TextGray)
                                        }
                                    }
                                } else {
                                    PermissionScreen(onRequestPermission = { multiplePermissionsState.launchMultiplePermissionRequest() })
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
fun GlassStatusBarHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xCCFFFFFF), // Very light translucent white for frost effect
                        Color(0x4DE0F2FE)  // Extremely subtle pale sky blue tint (30% opacity)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.6f)
            )
            .statusBarsPadding()
    )
}

@Composable
fun AppBottomNavigation(selectedTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
        NavigationBarItem(
            selected = selectedTab == "home",
            onClick = { onTabSelected("home") },
            icon = { Icon(if (selectedTab == "home") Icons.Default.Home else Icons.Outlined.Home, contentDescription = "Home") },
            label = { Text(LocalAppStrings.current.home, fontWeight = if (selectedTab == "home") FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = PrimaryGreen.copy(alpha = 0.1f),
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            )
        )
        NavigationBarItem(
            selected = selectedTab == "video",
            onClick = { onTabSelected("video") },
            icon = { Icon(if (selectedTab == "video") Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle, contentDescription = "Video") },
            label = { Text(LocalAppStrings.current.video, fontWeight = if (selectedTab == "video") FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = PrimaryGreen.copy(alpha = 0.1f),
                unselectedIconColor = TextGray, 
                unselectedTextColor = TextGray
            )
        )
        NavigationBarItem(
            selected = selectedTab == "create",
            onClick = { onTabSelected("create") },
            icon = {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(if (selectedTab == "create") PrimaryGreen else TextGray.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "তৈরি",
                        tint = if (selectedTab == "create") Color.White else TextGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            label = { Text(LocalAppStrings.current.create, fontWeight = if (selectedTab == "create") FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = PrimaryGreen,
                indicatorColor = Color.Transparent,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            )
        )
        NavigationBarItem(
            selected = selectedTab == "tracker",
            onClick = { onTabSelected("tracker") },
            icon = { Icon(if (selectedTab == "tracker") Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, contentDescription = "Tracker") },
            label = { Text(LocalAppStrings.current.tracker, fontWeight = if (selectedTab == "tracker") FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = PrimaryGreen.copy(alpha = 0.1f),
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            )
        )
        NavigationBarItem(
            selected = selectedTab == "profile",
            onClick = { onTabSelected("profile") },
            icon = { Icon(if (selectedTab == "profile") Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "Profile") },
            label = { Text(LocalAppStrings.current.profile, fontWeight = if (selectedTab == "profile") FontWeight.Bold else FontWeight.Normal) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryGreen,
                selectedTextColor = PrimaryGreen,
                indicatorColor = PrimaryGreen.copy(alpha = 0.1f),
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            )
        )
    }
}

@Composable
fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("লোকেশন পারমিশন", style = MaterialTheme.typography.headlineMedium, color = TextDark, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("অফলাইনে নামাজের সময়সূচি সঠিকভাবে হিসাব করার জন্য লোকেশন পারমিশন প্রয়োজন।", style = MaterialTheme.typography.bodyLarge, color = TextGray, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            shape = RoundedCornerShape(100.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Text("পারমিশন দিন", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        }
    }
}

@Composable
fun HomeScreen(
    state: com.example.viewmodel.ViewState, 
    onToggleAlarm: (String) -> Unit, 
    onNavigateToTracker: () -> Unit,
    onNavigateToQuran: () -> Unit,
    onNavigateToLocation: () -> Unit
) {
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryGreen)
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .clickable { onNavigateToLocation() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = "Location", tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(state.locationName, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Expand", tint = TextDark, modifier = Modifier.size(16.dp))
                }
                Text(state.currentDate, color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(top=2.dp))
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = TextDark)
            }
        }

        // Hero Section
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sunrise
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.WbTwilight, contentDescription = "Sunrise", tint = Color(0xFFF97316), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.prayerTimes?.sunrise?.toBengali() ?: "--", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(if (GlobalLanguage.isEnglish) "Sunrise" else "সূর্যোদয়", color = TextGray, fontSize = 11.sp)
            }

            // Circular Timer
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp).padding(8.dp)) {
                // Background circle (the part to be filled) - User said Black/Green
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.8f),
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round
                )
                // Foreground filling circle
                CircularProgressIndicator(
                    progress = { state.timerProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = PrimaryGreen,
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val nextLabel = if (state.nextPrayerRemaining.startsWith("০০:০০") || state.nextPrayerRemaining.startsWith("00:00")) LocalAppStrings.current.now else state.nextPrayerNameBen
                    Text(nextLabel, color = TextDark, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    Text(state.nextPrayerRemaining, color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(top=4.dp))
                }
            }

            // Sunset
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.WbSunny, contentDescription = "Sunset", tint = Color(0xFFF97316), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text(state.prayerTimes?.maghrib?.toBengali() ?: "--", color = TextDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(if (GlobalLanguage.isEnglish) "Sunset" else "সূর্যাস্ত", color = TextGray, fontSize = 11.sp)
            }
        }

        // Sub info (Sehri / Iftar Countdown)
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            SubInfoItem(if (GlobalLanguage.isEnglish) "Next Tahajjud" else "পরবর্তী তাহাজ্জুদ", "০৩:৩৪ এএম".toBengali())
            SubInfoItem(if (GlobalLanguage.isEnglish) "Next Ishraq" else "পরবর্তী ইশরাক", "০৬:৩০ এএম".toBengali())
            SubInfoItemProgress(
                title = state.specialCountdownLabel, 
                time = state.specialCountdownTime, 
                progress = "${(state.specialCountdownProgress * 100).toInt()}%".toBengali()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Placeholder Banner
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp).padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp)).background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
             Row(verticalAlignment=Alignment.CenterVertically) {
                 Icon(Icons.Default.MenuBook, contentDescription=null, tint=PrimaryGreen, modifier=Modifier.size(40.dp))
                 Spacer(modifier=Modifier.width(16.dp))
                 Column {
                    Text("মাকামাল কুরআনিক মেসেজ", fontWeight=FontWeight.Bold, color=TextDark)
                    Text("অনুবাদ ও তাফসীর জানতে পড়ুন", color=TextGray, fontSize=12.sp)
                 }
             }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Prayer Times List
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(LocalAppStrings.current.prayer_times_title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text(if (GlobalLanguage.isEnglish) "Set Alarm >" else "অ্যালার্ম সেট করুন >", color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(12.dp))

            var expanded by remember { mutableStateOf(false) }

            state.prayerTimes?.let { times ->
                val prayers = listOf(
                    Triple("Fajr", "ফজর", times.fajr), Triple("Sunrise", "সূর্যোদয়", times.sunrise),
                    Triple("Dhuhr", "যোহর", times.dhuhr), Triple("Asr", "আসর", times.asr),
                    Triple("Maghrib", "মাগরিব", times.maghrib), Triple("Isha", "এশা", times.isha)
                )
                
                // Show current/next prayer if not expanded, or all if expanded
                if (!expanded) {
                    val nextP = prayers.find { it.first == state.nextPrayerName } ?: prayers[0]
                    PrayerRow(nextP.second, nextP.third, state.alarms[nextP.first] == true, isActive = true) { onToggleAlarm(nextP.first) }
                } else {
                    prayers.forEach { p ->
                        PrayerRow(p.second, p.third, state.alarms[p.first] == true, isActive = p.first == state.nextPrayerName) { onToggleAlarm(p.first) }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { expanded = !expanded }.background(PrimaryGreen.copy(alpha=0.05f), RoundedCornerShape(20.dp)).padding(vertical=8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (expanded) LocalAppStrings.current.collapse else LocalAppStrings.current.see_all, color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = PrimaryGreen)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Categories Header
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(LocalAppStrings.current.categories, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
            Text(if (GlobalLanguage.isEnglish) "See All >" else "সবগুলো >", color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Grid Categories
        CategoryGrid(onNavigateToTracker, onNavigateToQuran)

        Spacer(modifier = Modifier.height(24.dp))

        // Forbidden Times
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(LocalAppStrings.current.forbidden_times, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
            Icon(Icons.Outlined.Info, contentDescription = "Info", tint=TextGray, modifier=Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            ForbiddenTimeCard(LocalAppStrings.current.sunrise, "০৫:১১ এএম", "০৫:২৩ এএম", Icons.Outlined.WbTwilight)
            ForbiddenTimeCard(LocalAppStrings.current.noon, "১১:৪৯ এএম", "১২:০৪ পিএম", Icons.Outlined.WbSunny)
            ForbiddenTimeCard(LocalAppStrings.current.sunset, "০৬:১০ পিএম", "০৬:২৩ পিএম", Icons.Outlined.WbTwilight)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SubInfoItem(title: String, time: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = TextGray, fontSize = 11.sp, fontWeight=FontWeight.Medium)
        Spacer(modifier=Modifier.height(4.dp))
        Text(time, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SubInfoItemProgress(title: String, time: String, progress: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = TextGray, fontSize = 11.sp, fontWeight=FontWeight.Medium)
        Spacer(modifier=Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
             Text(time, color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
             Spacer(modifier=Modifier.width(4.dp))
             Text(progress, color = PrimaryGreen, fontSize = 10.sp, fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
fun PrayerRow(name: String, time: String, isAlarmOn: Boolean, isActive: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(if(isActive) PrimaryGreen.copy(alpha=0.08f) else Color.Transparent, RoundedCornerShape(8.dp)).padding(horizontal=8.dp, vertical=8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (name == "এশা" || name == "মাগরিব" || name=="ফজর") Icons.Outlined.DarkMode else Icons.Outlined.LightMode, contentDescription = null, tint = TextGray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, color = TextDark, fontSize = 15.sp, fontWeight=if(isActive) FontWeight.Bold else FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(time.toBengali(), color = TextDark, fontSize = 14.sp, fontWeight=if(isActive) FontWeight.Bold else FontWeight.Medium)
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = onToggle, modifier = Modifier.size(24.dp)) {
                 Icon(
                     if (isAlarmOn) Icons.Default.NotificationsActive else Icons.Outlined.Notifications,
                     contentDescription = "Alarm",
                     tint = if (isAlarmOn) PrimaryGreen else TextGray,
                     modifier = Modifier.size(20.dp)
                 )
            }
        }
    }
}

@Composable
fun CategoryGrid(onNavigateToTracker: () -> Unit, onNavigateToQuran: () -> Unit) {
    val items = if (GlobalLanguage.isEnglish) {
        listOf(
            Triple("Al Quran", Icons.Outlined.MenuBook, Color(0xFF10B981)),
            Triple("Hadith", Icons.Outlined.LibraryBooks, Color(0xFF3B82F6)),
            Triple("Tasbih", Icons.Outlined.Album, Color(0xFF8B5CF6)),
            Triple("Qibla", Icons.Outlined.Explore, Color(0xFF10B981)),
            Triple("Dua", Icons.Outlined.WavingHand, Color(0xFFEC4899)),
            Triple("Allah's Names", Icons.Outlined.Star, Color(0xFFF59E0B)),
            Triple("Zakat", Icons.Outlined.MonetizationOn, Color(0xFF10B981)),
            Triple("Calendar", Icons.Outlined.CalendarMonth, Color(0xFF6366F1)),
            Triple("Amal Learning", Icons.Outlined.School, Color(0xFF8B5CF6)),
            Triple("Ramadan", Icons.Outlined.ModeNight, Color(0xFF6366F1)),
            Triple("Islamic Name", Icons.Outlined.People, Color(0xFF3B82F6)),
            Triple("Salah Learning", Icons.Outlined.SelfImprovement, Color(0xFF14B8A6))
        )
    } else {
        listOf(
            Triple("আল কুরআন", Icons.Outlined.MenuBook, Color(0xFF10B981)),
            Triple("হাদিস", Icons.Outlined.LibraryBooks, Color(0xFF3B82F6)),
            Triple("তাসবিহ", Icons.Outlined.Album, Color(0xFF8B5CF6)),
            Triple("কিবলা", Icons.Outlined.Explore, Color(0xFF10B981)),
            Triple("দোয়া", Icons.Outlined.WavingHand, Color(0xFFEC4899)),
            Triple("আল্লাহর নাম", Icons.Outlined.Star, Color(0xFFF59E0B)),
            Triple("যাকাত", Icons.Outlined.MonetizationOn, Color(0xFF10B981)),
            Triple("ক্যালেন্ডার", Icons.Outlined.CalendarMonth, Color(0xFF6366F1)),
            Triple("আমল শিক্ষা", Icons.Outlined.School, Color(0xFF8B5CF6)),
            Triple("রমজান", Icons.Outlined.ModeNight, Color(0xFF6366F1)),
            Triple("ইসলামিক নাম", Icons.Outlined.People, Color(0xFF3B82F6)),
            Triple("নামাজ শিক্ষা", Icons.Outlined.SelfImprovement, Color(0xFF14B8A6))
        )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        for (row in 0..2) {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                for (col in 0..3) {
                    val index = row * 4 + col
                    val item = items[index]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (item.first == "আমল শিক্ষা" || item.first == "তাসবিহ" || item.first == "নামাজ শিক্ষা" || 
                                    item.first == "Amal Learning" || item.first == "Tasbih" || item.first == "Salah Learning") {
                                    onNavigateToTracker()
                                } else if (item.first == "আল কুরআন" || item.first == "Al Quran") {
                                    onNavigateToQuran()
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp).background(item.third.copy(alpha=0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(item.second, contentDescription = item.first, tint = item.third, modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(item.first, color = TextDark, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight=14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ForbiddenTimeCard(title: String, start: String, end: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = title, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(start, color = TextGray, fontSize = 11.sp)
        Text(if (GlobalLanguage.isEnglish) "from" else "থেকে", color = TextGray, fontSize = 10.sp)
        Text(end, color = TextGray, fontSize = 11.sp)
    }
}

@Composable
fun SocialBlockerOverlay(
    platformName: String,
    onDismissToHome: () -> Unit
) {
    val context = LocalContext.current
    var secondsLeft by remember { mutableStateOf(5) }
    
    val hadiths = remember {
        listOf(
            "“রাসূলুল্লাহ (সাঃ) বলেছেন: মানুষের ইসলামের অন্যতম সৌন্দর্য হলো নিরর্থক ও অনর্থক কথা ও কাজ পরিহার করা।” — তিরমিযী",
            "“রাসূলুল্লাহ (সাঃ) বলেছেন: যে ব্যক্তি আল্লাহ ও পরকালের প্রতি ঈমান রাখে, সে যেন ভালো কথা বলে অথবা নীরব থাকে।” — বুখারী",
            "“রাসূলুল্লাহ (সাঃ) বলেছেন: দুটি নিয়ামত এমন রয়েছে, যে দুটিতে অধিকাংশ মানুষ ক্ষতিগ্রস্ত; তা হলো স্বাস্থ্য এবং অবসর সময়।” — বুখারী",
            "“রাসূলুল্লাহ (সাঃ) বলেছেন: নিশ্চয়ই তোমার প্রতিপালকের প্রতি তোমার অবধারিত কতর্ব্য রয়েছে, তোমার নিজের প্রতিও কতর্ব্য রয়েছে।” — বুখারী",
            "“আল্লাহতায়ালা বলেছেন: আর তোমরা অনর্থক ক্রীড়াকৌতুক থেকে নিজেদের দূরে রাখো।” — সূরা আল-মুমিনুন"
        )
    }
    val selectedHadith = remember { hadiths.random() }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            kotlinx.coroutines.delay(1000)
            secondsLeft--
        }
        onDismissToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A).copy(alpha = 0.95f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Halal Circle App Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFE0F5EE), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Halal Circle",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Warning Message
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "অবরুদ্ধ",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "অ্যাপ ব্যবহারে নিষেধাজ্ঞা",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "আপনি এখন $platformName ব্যবহার করতে পারবেন না। আপনার একাগ্রতা এবং আত্মিক উন্নয়ন বজায় রাখতে এই অ্যাপটি সাময়িকভাবে ব্লক করা হয়েছে।",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                // Beautiful Islamic Hadith Section with Rose/Muted Box styling
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAF5FF), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFF3E8FF), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF9333EA),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = selectedHadith,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF581C87),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Countdown Timer Progress Indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFFF43F5E), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = secondsLeft.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = "সেকেন্ডের মধ্যে আপনাকে সড়িয়ে দেওয়া হবে...",
                        fontSize = 13.sp,
                        color = Color(0xFFF43F5E),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Exit immediately button
                Button(
                    onClick = onDismissToHome,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = "হোম স্কিনে ফিরে যান",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
