package com.example

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebsiteBlockerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("profile_prefs", Context.MODE_PRIVATE) }

    // State for input URL
    var webUrlInput by remember { mutableStateOf("") }
    
    // Read list of blocked websites
    var blockedWebsitesList by remember {
        mutableStateOf<List<String>>(
            sharedPrefs.getString("blocked_websites_list", "")
                ?.split(",")
                ?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    // Is global website blocking toggle active
    var isWebBlockedEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("web_blocked", false))
    }

    // Permission dialog state
    var showPermissionSettingsDialog by remember { mutableStateOf(false) }
    var showRestrictedHelp by remember { mutableStateOf(false) }

    // Intercept back actions
    BackHandler(onBack = onBack)

    // Helper to check accessibility service
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = android.content.ComponentName(context, SocialAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(expectedComponentName.flattenToString()) == true
    }

    // Helper to persist list
    val saveBlockedWebsites = { list: List<String> ->
        val joined = list.joinToString(",")
        sharedPrefs.edit().putString("blocked_websites_list", joined).apply()
        blockedWebsitesList = list

        // Sync main web_blocked state
        val hasItems = list.isNotEmpty()
        if (hasItems != isWebBlockedEnabled) {
            isWebBlockedEnabled = hasItems
            sharedPrefs.edit().putBoolean("web_blocked", hasItems).apply()
        }
        
        // Start simple foreground service if any block is active
        val isAnyActive = hasItems || sharedPrefs.getBoolean("social_blocked", false)
        if (isAnyActive) {
            SocialBlockerService.startService(context)
        } else {
            SocialBlockerService.stopService(context)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = Color(0xFFF8FAFC), // Ultra modern cool-white background
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "পিছনে যান",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "ওয়েবসাইট ব্লকার",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color(0xFF0F172A)
                    )
                }

                // Help/Info dialog
                var showHelpDialog by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "তথ্য",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (showHelpDialog) {
                    AlertDialog(
                        onDismissRequest = { showHelpDialog = false },
                        shape = RoundedCornerShape(20.dp),
                        containerColor = Color.White,
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF10B981))
                                Text("ওয়েবসাইট ব্লকার নীতি", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Text(
                                "আমাদের ওয়েবসাইট ব্লকার ফিচারটি সম্পূর্ণ লোকাল স্টোরেজে চালিত হয়। আপনার পবিত্র চোখ ও মনকে হারাম ডিস্ট্রাকশন ও আসক্তি থেকে রক্ষা করার জন্য যেকোনো ওয়েবসাইট সহজে ব্লক করতে পারেন। এটি ব্রাউজার সেশন ও লিংক ওপেনিং সনাক্ত করতে ইউজ স্ট্যাটাস পারমিশন ব্যবহার করে। কোনো ডাটা বাহিরে ট্র্যান্সফার করা হয় না।",
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                lineHeight = 20.sp
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { showHelpDialog = false }) {
                                Text("ঠিক আছে", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Welcome Premium Warning/Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                if (isWebBlockedEnabled) Color(0xFFFEF2F2) else Color(0xFFECFDF5),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isWebBlockedEnabled) Icons.Default.Block else Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (isWebBlockedEnabled) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = if (isWebBlockedEnabled) "ওয়েবসাইট ফিল্টার সক্রিয় রয়েছে" else "ওয়েবসাইট ফিল্টার নিষ্ক্রিয়",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1E293B)
                    )

                    Text(
                        text = "নিচের বক্সে আপনার কাঙ্ক্ষিত অশোভন বা বিভ্রান্তিকর ওয়েবসাইট লিংক যুক্ত করুন। যুক্ত করার পর স্বয়ংক্রিয়ভাবে ব্রাউজারে সেই লিংকটি অবরুদ্ধ বা স্ক্রিন লক করা হবে।",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            // URL input box card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "নতুন ওয়েবসাইট যুক্ত করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )

                    OutlinedTextField(
                        value = webUrlInput,
                        onValueChange = { webUrlInput = it },
                        placeholder = { Text("যেমন: example.com বা badsite.com", fontSize = 13.sp) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = Color(0xFF10B981)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (webUrlInput.isBlank()) {
                                Toast.makeText(context, "অনুগ্রহ করে একটি সঠিক ওয়েবসাইট লিংক লিখুন", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Clean the input website format
                            var formattedUrl = webUrlInput.trim().lowercase(Locale.ROOT)
                            formattedUrl = formattedUrl.removePrefix("http://")
                            formattedUrl = formattedUrl.removePrefix("https://")
                            formattedUrl = formattedUrl.removePrefix("www.")
                            
                            if (formattedUrl.isBlank()) {
                                Toast.makeText(context, "অকার্যকর ওয়েবসাইট লিংক!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Check permissions first
                            if (!isAccessibilityServiceEnabled(context)) {
                                showPermissionSettingsDialog = true
                            } else {
                                val currentList = blockedWebsitesList.toMutableList()
                                if (currentList.contains(formattedUrl)) {
                                    Toast.makeText(context, "এই ওয়েবসাইটটি ইতিমধ্যেই ব্লক তালিকায় রয়েছে!", Toast.LENGTH_SHORT).show()
                                } else {
                                    currentList.add(formattedUrl)
                                    saveBlockedWebsites(currentList)
                                    webUrlInput = ""
                                    Toast.makeText(context, "ওয়েবসাইট সফলভাবে স্থানীয় মোবাইলে ব্লক তালিকায় সেভ করা হয়েছে!", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("ব্লকলিস্টে সেভ করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Blocked websites list header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "বর্তমানে ব্লক করা ওয়েবসাইটসমূহ (${blockedWebsitesList.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )

                if (blockedWebsitesList.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            saveBlockedWebsites(emptyList())
                            Toast.makeText(context, "ব্লকলিস্টের সকল ওয়েবসাইট মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("সব পরিষ্কার করুন", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Scrollable list items card
            if (blockedWebsitesList.isEmpty()) {
                // Empty state card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFF1F5F9), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "কোনো ওয়েবসাইট ব্লক করা নেই",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "আপনার চিন্তা ও দৃষ্টির পবিত্রতা বজায় রাখতে ক্ষতিকর বা সময় অপচয়কারী ডোমেইন লিঙ্ক ব্লক করুন।",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        blockedWebsitesList.forEach { site ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFFFEF2F2), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = site,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "লোকাল স্টোরেজে সংরক্ষিত",
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val updated = blockedWebsitesList.toMutableList()
                                        updated.remove(site)
                                        saveBlockedWebsites(updated)
                                        Toast.makeText(context, "$site ব্লকলিস্ট থেকে মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "মুছুন",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            if (blockedWebsitesList.last() != site) {
                                Divider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    }
                }
            }

            // Beautiful Islamic Reminder Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "একটি বরকতময় জ্ঞান",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF14532D)
                        )
                        Text(
                            text = "“রাসূলুল্লাহ (সাঃ) বলেছেন: হে যুবসমাজ! তোমাদের মধ্যে যে ব্যক্তি বিয়ের সামর্থ্য রাখে সে যেন বিয়ে করে ফেলে। কারণ এটি দৃষ্টিকে সংযত রাখে এবং লজ্জাস্থানের হেফাজত করে।” — বুখারী",
                            fontSize = 11.sp,
                            color = Color(0xFF15803D),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Modern styled "Open Settings" or "Cancel" dialogue for needed Usage permissions
    if (showPermissionSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionSettingsDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFEF3C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "অনুমতি প্রয়োজন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ওয়েবসাইট ফিল্টারটি রিয়েল-টাইমে সচল রাখার জন্য আপনার ডিভাইসের 'Accessibility Service' সক্রিয় করা প্রয়োজন।\n\n১. নিচে বাটনে ক্লিক করে সেটিংস-এ যান।\n২. 'Downloaded apps' অথবা 'Installed services' সেকশনে ক্লিক করুন।\n৩. 'Halal Circle' সিলেক্ট করে সার্ভিসটি অন করে দিন।",
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        lineHeight = 20.sp
                    )
                    Divider()
                    Text(
                        text = "নিরবচ্ছিন্ন ব্লকিং নিশ্চিত করতে ব্যাটারি অপ্টিমাইজেশন বন্ধ করার পরামর্শ দেওয়া হলো।",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = { showRestrictedHelp = true },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF10B981))
                            Spacer(Modifier.width(4.dp))
                            Text("'Restricted Setting' সমস্যা হচ্ছে?", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionSettingsDialog = false
                        try {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "ডিভাইস সেটিংস খোলা সম্ভব হচ্ছে না।", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Accessibility অন করুন", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "সেটিংস খোলা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("ব্যাটারি সেটআপ", color = Color(0xFF64748B), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
        )
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
                    Text("৪. 'Allow restricted settings' এ ক্লিক করুন।", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    Text("৫. এখন ফিরে এসে আবার পারমিশনটি দিন।", fontSize = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showRestrictedHelp = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("বুঝেছি", color = Color.White)
                }
            }
        )
    }
}
