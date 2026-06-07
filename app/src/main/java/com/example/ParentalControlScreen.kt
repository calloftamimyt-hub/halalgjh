package com.example

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGray
import com.example.viewmodel.GlobalLanguage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

// Child Profile Data Model
data class ChildProfile(
    val id: String,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentalControlScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val currentUser = remember { auth.currentUser }
    val currentUserId = currentUser?.uid ?: ""
    val isEnglish = GlobalLanguage.isEnglish

    // SharedPreferences to manage child mode lock status locally
    val sharedPrefs = remember { context.getSharedPreferences("parental_prefs", Context.MODE_PRIVATE) }
    var isChildModeActiveOnDevice by remember { mutableStateOf(sharedPrefs.getBoolean("is_child_mode_active", false)) }

    // Navigation Active Tab: parent or child
    var activeTab by remember { mutableStateOf(if (isChildModeActiveOnDevice) "child" else "parent") }

    // Dialog state to switch back to parent mode via Parental Security PIN
    var showPinValidationDialog by remember { mutableStateOf(false) }
    var pinValueInput by remember { mutableStateOf("") }
    var pinErrorMessage by remember { mutableStateOf("") }

    // Parental configuration states
    var childrenList by remember { mutableStateOf<List<ChildProfile>>(emptyList()) }
    var selectedChild by remember { mutableStateOf<ChildProfile?>(null) }
    var isLoadingChildren by remember { mutableStateOf(false) }

    // Child linking input code
    var pairCodeInput by remember { mutableStateOf("") }
    var isLinking by remember { mutableStateOf(false) }

    // Sync child mode tab state in case local state alters
    LaunchedEffect(isChildModeActiveOnDevice) {
        if (isChildModeActiveOnDevice) {
            activeTab = "child"
        }
    }

    // Fetch parent's linked children
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            isLoadingChildren = true
            db.collection("parental_links")
                .whereEqualTo("parentUid", currentUserId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) {
                        isLoadingChildren = false
                        return@addSnapshotListener
                    }
                    val list = snapshot.documents.mapNotNull { doc ->
                        val childId = doc.getString("childUid") ?: ""
                        val childName = doc.getString("childName") ?: "Child"
                        if (childId.isNotEmpty()) {
                            ChildProfile(id = childId, name = childName)
                        } else null
                    }
                    childrenList = list
                    if (selectedChild == null && list.isNotEmpty()) {
                        selectedChild = list.first()
                    }
                    isLoadingChildren = false
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEnglish) "Parental Control Hub" else "প্যারেন্টাল কন্ট্রোল হাব",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                actions = {
                    // Switch back to parent mode lock button shown on Child devices
                    if (isChildModeActiveOnDevice) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .background(Color(0xFFFFECEB), RoundedCornerShape(20.dp))
                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(20.dp))
                                .clickable {
                                    pinValueInput = ""
                                    pinErrorMessage = ""
                                    showPinValidationDialog = true
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Exit",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isEnglish) "Parent Portal" else "অভিভাবক পোর্টাল",
                                    fontSize = 11.sp,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                modifier = Modifier.background(Color.White)
            )
        },
        containerColor = Color(0xFFF3F4F6)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mode Select Switch Tab row (ONLY shown if Child mode is NOT locked on this phone!)
            if (!isChildModeActiveOnDevice) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    TabButton(
                        title = if (isEnglish) "Parent Mode" else "অভিভাবক মোড",
                        isSelected = activeTab == "parent",
                        onClick = { activeTab = "parent" },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        title = if (isEnglish) "Child Mode" else "শিশু মোড",
                        isSelected = activeTab == "child",
                        onClick = { activeTab = "child" },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Informative header on Child Mode devices
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            tint = Color(0xFF00796B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "Parent Mode is turned OFF from this device." else "এই ফোনে অভিভাবক মোড বন্ধ রয়েছে (চাইল্ড প্রোটেকশন সক্রিয়)।",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00796B)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ParentalContentTransition"
            ) { targetTab ->
                if (targetTab == "parent") {
                    ParentDashboard(
                        currentUserId = currentUserId,
                        childrenList = childrenList,
                        selectedChild = selectedChild,
                        onSelectChild = { selectedChild = it },
                        isLoadingChildren = isLoadingChildren,
                        pairCodeInput = pairCodeInput,
                        onPairCodeInputChange = { pairCodeInput = it },
                        isLinking = isLinking,
                        onLinkChild = {
                            if (currentUserId.isEmpty()) {
                                Toast.makeText(context, if (isEnglish) "Please log in first" else "দয়া করে প্রথমে লগইন করুন", Toast.LENGTH_SHORT).show()
                                return@ParentDashboard
                            }
                            if (pairCodeInput.length != 6) {
                                Toast.makeText(context, if (isEnglish) "Enter 6-digit code!" else "৬ ডিজিটের কোড দিন!", Toast.LENGTH_SHORT).show()
                                return@ParentDashboard
                            }
                            isLinking = true
                            db.collection("pairing_codes")
                                .document(pairCodeInput.trim())
                                .get()
                                .addOnSuccessListener { doc ->
                                    if (doc.exists()) {
                                        val childUid = doc.getString("childUid") ?: ""
                                        val childName = doc.getString("childName") ?: "Child"
                                        if (childUid.isNotEmpty()) {
                                            // Create link
                                            val linkData = mapOf(
                                                "parentUid" to currentUserId,
                                                "childUid" to childUid,
                                                "childName" to childName,
                                                "linkedAt" to System.currentTimeMillis()
                                            )
                                            db.collection("parental_links")
                                                .document("${currentUserId}_${childUid}")
                                                .set(linkData)
                                                .addOnSuccessListener {
                                                    // Initialize control configuration
                                                    db.collection("parental_configs")
                                                        .document(childUid)
                                                        .set(mapOf(
                                                            "screenTimeLimit" to 60.0,
                                                            "safeZoneRadius" to 500.0,
                                                            "blockedContent" to true,
                                                            "childLatitude" to 23.8964,
                                                            "childLongitude" to 90.3984,
                                                            "childAddress" to "Sector 4, Uttara, Dhaka",
                                                            "isScreenOff" to false,
                                                            "activeApp" to "",
                                                            "lockedApps" to mapOf(
                                                                "facebook" to false,
                                                                "youtube" to false,
                                                                "tiktok" to false,
                                                                "instagram" to false,
                                                                "telegram" to false,
                                                                "whatsapp" to false,
                                                                "google_chrome" to false
                                                            ),
                                                            "lastUpdated" to System.currentTimeMillis()
                                                        ))
                                                    
                                                    // Remove pairing code document
                                                    db.collection("pairing_codes").document(pairCodeInput.trim()).delete()
                                                    
                                                    Toast.makeText(context, if (isEnglish) "Device linked successfully!" else "ডিভাইস সফলভাবে লিঙ্ক করা হয়েছে!", Toast.LENGTH_LONG).show()
                                                    pairCodeInput = ""
                                                    isLinking = false
                                                }
                                                .addOnFailureListener {
                                                    Toast.makeText(context, "Linking Failed!", Toast.LENGTH_SHORT).show()
                                                    isLinking = false
                                                }
                                        } else {
                                            Toast.makeText(context, if (isEnglish) "Invalid child ID in code" else "কোডটি সঠিক নয়!", Toast.LENGTH_SHORT).show()
                                            isLinking = false
                                        }
                                    } else {
                                        Toast.makeText(context, if (isEnglish) "Invalid code or expired!" else "কোডটি সঠিক নয় অথবা মেয়াদ শেষ!", Toast.LENGTH_LONG).show()
                                        isLinking = false
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                                    isLinking = false
                                }
                        }
                    )
                } else {
                    ChildSection(
                        currentUserId = currentUserId,
                        isChildModeActiveOnDevice = isChildModeActiveOnDevice,
                        onToggleChildModeActive = { targetState ->
                            isChildModeActiveOnDevice = targetState
                            sharedPrefs.edit().putBoolean("is_child_mode_active", targetState).apply()
                            // Update tab state instantly
                            if (targetState) {
                                activeTab = "child"
                            }
                        }
                    )
                }
            }
        }

        // Parent Verification Security Overlay to switch back to normal modes
        if (showPinValidationDialog) {
            Dialog(onDismissRequest = { showPinValidationDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "PIN Secured",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isEnglish) "Parent Authentication" else "অভিভাবক নিরাপত্তা পিন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnglish) "Enter 4-digit master PIN (Default: 1234) to restore parent controls." 
                                   else "ডিভাইসে অভিভাবক মোড রি-অ্যাক্টিভেট করতে ৪-ডিজিট পিন (ডিফল্ট: 1234) দিন।",
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = pinValueInput,
                            onValueChange = {
                                if (it.length <= 4) {
                                    pinValueInput = it
                                    pinErrorMessage = ""
                                }
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            placeholder = { Text("E.g. 1234") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )

                        if (pinErrorMessage.isNotEmpty()) {
                            Text(
                                text = pinErrorMessage,
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { showPinValidationDialog = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isEnglish) "Cancel" else "বাতিল",
                                    color = TextGray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Button(
                                onClick = {
                                    if (pinValueInput == "1234") {
                                        isChildModeActiveOnDevice = false
                                        sharedPrefs.edit().putBoolean("is_child_mode_active", false).apply()
                                        activeTab = "parent"
                                        showPinValidationDialog = false
                                        Toast.makeText(context, if (isEnglish) "Parent mode re-enabled successfully" else "অভিভাবক মোড পুনরায় সক্রিয় করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        pinErrorMessage = if (isEnglish) "Incorrect PIN Code! Try again." else "ভুল পিন কোড! পুনরায় চেষ্টা করুন।"
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isEnglish) "Verify" else "যাচাই করুন",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryGreen else Color.Transparent,
        label = "tabBgColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF4B5563),
        label = "tabTxtColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = contentColor
        )
    }
}

@Composable
fun ParentDashboard(
    currentUserId: String,
    childrenList: List<ChildProfile>,
    selectedChild: ChildProfile?,
    onSelectChild: (ChildProfile) -> Unit,
    isLoadingChildren: Boolean,
    pairCodeInput: String,
    onPairCodeInputChange: (String) -> Unit,
    isLinking: Boolean,
    onLinkChild: () -> Unit
) {
    val isEnglish = GlobalLanguage.isEnglish
    val db = remember { FirebaseFirestore.getInstance() }

    if (currentUserId.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (isEnglish) "Authentication Required" else "লগইন প্রয়োজন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isEnglish) "Please sign in to access parental controls and track child devices." 
                               else "প্যারেন্টাল কন্ট্রোল এবং চাইল্ড ট্র্যাকিং অ্যাক্সেস করতে অনুগ্রহ করে প্রথমে সাইন ইন করুন।",
                        fontSize = 13.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    if (childrenList.isEmpty()) {
        // Unlinked Setup screen
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Security",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isEnglish) "Safe Kids, Peace Of Mind" else "নিরাপদ শিশু, নিশ্চিন্ত অভিভাবক",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isEnglish) 
                                "Monitor location, limit screen time, block social media apps, and suspend children's screens remotely in real-time."
                                else "সহজেই শিশুর অবস্থান নির্ধারণ করুন, স্ক্রিন টাইম লিমিট নির্ধারণ করুন, যেকোনো পপুলার অ্যাপ রিমোট লক করুন এবং সরাসরি স্ক্রিন লক করুন রিয়েল-টাইমে।",
                            fontSize = 13.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (isEnglish) "Link Child Device" else " his account with secure codes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isEnglish) "Enter the 6-digit pair code shown on your child's phone under 'Child Mode'." 
                                   else "শিশুর ফোন থেকে ' his safety pin' সিলেক্ট করে প্রাপ্ত ৬ ডিজিটের পিন কোডটি নিচে দিন।",
                            fontSize = 13.sp,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = pairCodeInput,
                            onValueChange = { if (it.length <= 6) onPairCodeInputChange(it) },
                            placeholder = { Text(text = "E.g. 748291", fontSize = 14.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = Color(0xFFE5E7EB)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onLinkChild,
                            enabled = !isLinking && pairCodeInput.length == 6,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            if (isLinking) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = if (isEnglish) "Connect & Setup" else "ডিভাইস কানেক্ট করুন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Connected Dashboard
        val childId = selectedChild?.id ?: ""
        var activeAppOnChild by remember { mutableStateOf("") }
        var lockedAppsMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
        var isChildScreenSuspended by remember { mutableStateOf(false) }

        // Start real-time listener for the active child's remote states
        DisposableEffect(childId) {
            if (childId.isEmpty()) return@DisposableEffect onDispose {}
            val docRef = db.collection("parental_configs").document(childId)
            val registration = docRef.addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    activeAppOnChild = snapshot.getString("activeApp") ?: ""
                    isChildScreenSuspended = snapshot.getBoolean("isScreenOff") ?: false
                    lockedAppsMap = snapshot.get("lockedApps") as? Map<String, Boolean> ?: emptyMap()
                }
            }
            onDispose { registration.remove() }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Horizontal children selector
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = if (isEnglish) "Select Connected Child" else "সংযুক্ত শিশু নির্বাচন করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        childrenList.forEach { child ->
                            val isSelected = child.id == selectedChild?.id
                            Card(
                                onClick = { onSelectChild(child) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryGreen.copy(alpha = 0.12f) else Color.White
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) PrimaryGreen else Color(0xFFE5E7EB)
                                ),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .fillMaxHeight()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) PrimaryGreen else Color(0xFFD1D5DB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = child.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = child.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) PrimaryGreen else TextDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedChild != null) {
                // Real-time Active App User Alert & Instantly remote blocking panel!
                if (activeAppOnChild.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                            border = BorderStroke(1.dp, Color(0xFFFFC107)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Notification Alerts",
                                    tint = Color(0xFF856404),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isEnglish) "Device Alert Triggered" else "লাইভ ডিভাইস অ্যালার্ট",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF856404)
                                    )
                                    Text(
                                        text = if (isEnglish) 
                                            "Your child is using Google Chrome or '$activeAppOnChild' right now." 
                                            else "আপনার শিশু এখন জাস্ট Google Chrome বা '$activeAppOnChild' ব্যবহার শুরু করেছে।",
                                        fontSize = 12.sp,
                                        color = Color(0xFF856404),
                                        lineHeight = 16.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFDC3545), RoundedCornerShape(8.dp))
                                        .clickable {
                                            // Instantly update locked status in FB config and clear active App
                                            val key = activeAppOnChild.lowercase().replace(" ", "_")
                                            val updated = lockedAppsMap.toMutableMap()
                                            updated[key] = true
                                            db.collection("parental_configs")
                                                .document(childId)
                                                .update(mapOf(
                                                    "lockedApps" to updated,
                                                    "activeApp" to ""
                                                ))
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "BLOCK NOW" else "ব্লক করুন",
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Remote Live Screen Suspension Control card!
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isChildScreenSuspended) Color(0xFFFFEBEE) else Color.White),
                        border = BorderStroke(1.dp, if (isChildScreenSuspended) Color(0xFFEF4444) else Color(0xFFE5E7EB)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            if (isChildScreenSuspended) Color(0xFFEF4444).copy(alpha = 0.1f) else Color(0xFFFFECEB),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isChildScreenSuspended) Icons.Default.PhoneLocked else Icons.Default.PhonelinkOff,
                                        contentDescription = "Screen Lock",
                                        tint = if (isChildScreenSuspended) Color(0xFFEF4444) else Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "Instant Screen Suspension" else "ইনস্ট্যান্ট রিমোট স্ক্রিন অফ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = if (isChildScreenSuspended) {
                                            if (isEnglish) "Device screen is completely blocked" else "শিশুর ফোনের স্ক্রিন সম্পূর্ণ বন্ধ রয়েছে"
                                        } else {
                                            if (isEnglish) "Turn off screen in real-time" else "একটি ক্লিকেই ডিভাইসটি সাময়িকভাবে লক করুন"
                                        },
                                        fontSize = 12.sp,
                                        color = TextGray
                                    )
                                }
                            }

                            Switch(
                                checked = isChildScreenSuspended,
                                onCheckedChange = { value ->
                                    isChildScreenSuspended = value
                                    db.collection("parental_configs")
                                        .document(childId)
                                        .update("isScreenOff", value)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFEF4444)
                                )
                            )
                        }
                    }
                }

                // Application Lock Control List
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AppBlocking,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isEnglish) "Social & Network Blocker" else "সোশ্যাল এবং ব্রাউজার ব্লকার",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextDark
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))

                            val majorApps = listOf(
                                AppItem("Facebook", "facebook", Icons.Default.ThumbUp),
                                AppItem("YouTube", "youtube", Icons.Default.PlayArrow),
                                AppItem("TikTok", "tiktok", Icons.Default.MusicNote),
                                AppItem("Instagram", "instagram", Icons.Default.CameraAlt),
                                AppItem("Telegram", "telegram", Icons.Default.Send),
                                AppItem("WhatsApp", "whatsapp", Icons.Default.Call),
                                AppItem("Google Chrome", "google_chrome", Icons.Default.Language)
                            )

                            majorApps.forEach { app ->
                                val isLocked = lockedAppsMap[app.key] ?: false
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .background(
                                                    if (isLocked) Color(0xFFFFECEB) else Color(0xFFF3F4F6),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = app.icon,
                                                contentDescription = app.name,
                                                tint = if (isLocked) Color(0xFFEF4444) else TextGray,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = app.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = TextDark
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val updated = lockedAppsMap.toMutableMap()
                                            updated[app.key] = !isLocked
                                            db.collection("parental_configs")
                                                .document(childId)
                                                .update("lockedApps", updated)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Toggle app lock",
                                            tint = if (isLocked) Color(0xFFEF4444) else Color(0xFF9CA3AF)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    ChildLocationTracker(childId = selectedChild.id)
                }

                item {
                    ChildScreenLimits(childId = selectedChild.id)
                }

                item {
                    ChildActivityReports(childId = selectedChild.id)
                }
            }
        }
    }
}

data class AppItem(
    val name: String,
    val key: String,
    val icon: ImageVector
)

// Child live location map visualization block
@Composable
fun ChildLocationTracker(childId: String) {
    val db = remember { FirebaseFirestore.getInstance() }
    val isEnglish = GlobalLanguage.isEnglish

    var lat by remember { mutableStateOf(23.8964) }
    var lng by remember { mutableStateOf(90.3984) }
    var address by remember { mutableStateOf("Sector 4, Uttara, Dhaka") }
    var safeRadius by remember { mutableStateOf(500.0) }
    var alertOnExit by remember { mutableStateOf(true) }
    var isUpdatingLive by remember { mutableStateOf(false) }

    // Fetch config values in real-time
    LaunchedEffect(childId) {
        db.collection("parental_configs")
            .document(childId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    lat = snapshot.getDouble("childLatitude") ?: 23.8964
                    lng = snapshot.getDouble("childLongitude") ?: 90.3984
                    address = snapshot.getString("childAddress") ?: "Sector 4, Uttara, Dhaka"
                    safeRadius = snapshot.getDouble("safeZoneRadius") ?: 500.0
                    alertOnExit = snapshot.getBoolean("alertOnExit") ?: true
                }
            }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Live Location Tracker" else "লাইভ লোকেশন ট্র্যাকিং",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = TextDark
                    )
                }
                Text(
                    text = if (isEnglish) "Active Secures" else "নিরাপদ জোন সক্রিয়",
                    fontSize = 11.sp,
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Beautiful Custom MAP Graphic representation utilizing Vector Canvas drawing elements
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE2E8F0)),
                contentAlignment = Alignment.Center
            ) {
                // Animated pulse scaling effect for location center
                val transition = rememberInfiniteTransition(label = "pulse")
                val animatedRadiusScaling by transition.animateFloat(
                    initialValue = 0.12f,
                    targetValue = 0.95f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "pulse_scaling"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val mapCenter = Offset(size.width / 2f, size.height / 2f)

                    // Draw safety zone radius representation
                    drawCircle(
                        color = PrimaryGreen.copy(alpha = 0.08f),
                        radius = 70.dp.toPx(),
                        center = mapCenter
                    )
                    drawCircle(
                        color = PrimaryGreen.copy(alpha = 0.22f),
                        radius = 70.dp.toPx(),
                        center = mapCenter,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f))
                    )

                    // Expandable locator signal wave
                    drawCircle(
                        color = Color(0xFF3B82F6).copy(alpha = 0.3f * (1f - animatedRadiusScaling)),
                        radius = 64.dp.toPx() * animatedRadiusScaling,
                        center = mapCenter
                    )

                    // Locator core dot representing the child's position
                    drawCircle(
                        color = Color.White,
                        radius = 11.dp.toPx(),
                        center = mapCenter
                    )
                    drawCircle(
                        color = Color(0xFF3B82F6),
                        radius = 7.dp.toPx(),
                        center = mapCenter
                    )
                }

                // Small informative badges overlay on visual map
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "GPS: $lat, $lng",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = address,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Text(
                        text = if (isEnglish) "Last Updated: Just Now" else "সবশেষ সিঙ্ক: জাস্ট নাও",
                        fontSize = 10.sp,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Configuration for Safe Zone
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEnglish) "Safe Zone Radius" else "নিরাপদ জোন এর ব্যাসার্ধ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextDark
                )
                Text(
                    text = "${safeRadius.toInt()} m",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = PrimaryGreen
                )
            }

            Slider(
                value = safeRadius.toFloat(),
                onValueChange = {
                    safeRadius = it.toDouble()
                },
                onValueChangeFinished = {
                    db.collection("parental_configs")
                        .document(childId)
                        .update("safeZoneRadius", safeRadius)
                },
                valueRange = 100f..1500f,
                colors = SliderDefaults.colors(
                    activeTrackColor = PrimaryGreen,
                    thumbColor = PrimaryGreen
                ),
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Alert Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = if (isEnglish) "Alert on Exit" else "সীমানা অতিক্রমে সতর্কবার্তা",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    Text(
                        text = if (isEnglish) "Get notified if child leaves safe zone" else "সীমানা পার হলে তাৎক্ষণিক সতর্কবার্তা পুশ করা হবে",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
                Switch(
                    checked = alertOnExit,
                    onCheckedChange = {
                        alertOnExit = it
                        db.collection("parental_configs")
                            .document(childId)
                            .update("alertOnExit", alertOnExit)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    isUpdatingLive = true
                    db.collection("parental_configs")
                        .document(childId)
                        .update(
                            mapOf(
                                "childLatitude" to 23.8964 + (Random.nextDouble() - 0.5) * 0.002,
                                "childLongitude" to 90.3984 + (Random.nextDouble() - 0.5) * 0.002,
                                "lastUpdated" to System.currentTimeMillis()
                            )
                        ).addOnSuccessListener {
                            isUpdatingLive = false
                        }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6), contentColor = TextDark),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isUpdatingLive) {
                    CircularProgressIndicator(color = TextDark, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Request Live Coordinate Sync" else "লাইভ জিপিএস স্থানাঙ্ক সিঙ্ক করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// Child device screen limits setup card
@Composable
fun ChildScreenLimits(childId: String) {
    val db = remember { FirebaseFirestore.getInstance() }
    val isEnglish = GlobalLanguage.isEnglish

    var timeLimit by remember { mutableStateOf(60.0) }
    var contentFilters by remember { mutableStateOf(true) }

    LaunchedEffect(childId) {
        db.collection("parental_configs")
            .document(childId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    timeLimit = snapshot.getDouble("screenTimeLimit") ?: 60.0
                    contentFilters = snapshot.getBoolean("blockedContent") ?: true
                }
            }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF3B82F6))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEnglish) "Device Limits" else "স্ক্রিন ব্যবহারের বরাদ্দ সময়সীমা",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextDark
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isEnglish) "Daily Time Budget" else "প্রতিদিনের ব্যবহারের সময়সীমা",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextDark
                )
                Text(
                    text = if (isEnglish) "${timeLimit.toInt()} Minutes" else "${timeLimit.toInt()} মিনিট",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF3B82F6)
                )
            }

            Slider(
                value = timeLimit.toFloat(),
                onValueChange = {
                    timeLimit = it.toDouble()
                },
                onValueChangeFinished = {
                    db.collection("parental_configs")
                        .document(childId)
                        .update("screenTimeLimit", timeLimit)
                },
                valueRange = 15f..240f,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color(0xFF3B82F6),
                    thumbColor = Color(0xFF3B82F6)
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Divider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(vertical = 10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEnglish) "Restricted Entertainment Only" else "রিলস ও বিনোদনমুলক ভিডিও লক করুন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                    Text(
                        text = if (isEnglish) "Toggles block for random scrolling contents on Halal video tab" 
                               else "হালকা বিনোদন ও আসক্তিকর রিলস ভিডিও স্ক্রলিং অপশন বন্ধ করে রাখবে",
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
                Switch(
                    checked = contentFilters,
                    onCheckedChange = {
                        contentFilters = it
                        db.collection("parental_configs")
                            .document(childId)
                            .update("blockedContent", contentFilters)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF3B82F6)
                    )
                )
            }
        }
    }
}

// Academic, Quran reading reports under parental tracker
@Composable
fun ChildActivityReports(childId: String) {
    val isEnglish = GlobalLanguage.isEnglish

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = PrimaryGreen)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEnglish) "Islam & Quran Activity Reports" else "ইসলামিক ও কুরআন অ্যাক্টিভিটি রিপোর্ট",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextDark
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val reports = listOf(
                ReportItem("সূরা আল-ফাতিহা তিলাওয়াত সম্পন্ন", "১০ মিনিট আগে", Icons.Filled.MenuBook, PrimaryGreen),
                ReportItem("আজকে জোহর নামাজ আদায় ট্র্যাকড", "২ ঘণ্টা আগে", Icons.Filled.Check, Color(0xFF3B82F6)),
                ReportItem("১টি কারিগরি নৈতিক ভিডিও দেখে ট্র্যাকার পূর্ণ", "৪ ঘণ্টা আগে", Icons.Filled.PlayArrow, Color(0xFFF59E0B)),
                ReportItem("সূরা আল-ইখলাস ৩ বার পাঠ সম্পন্ন", "আজ সকাল", Icons.Filled.Favorite, Color(0xFFEF4444))
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                reports.forEach { report ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(report.color.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = report.icon,
                                contentDescription = null,
                                tint = report.color,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = report.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = TextDark
                            )
                            Text(
                                text = report.time,
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                    }
                }
            }
        }
    }
}

data class ReportItem(
    val title: String,
    val time: String,
    val icon: ImageVector,
    val color: Color
)

// Active Child device pairing & configuration mode section
@Composable
fun ChildSection(
    currentUserId: String,
    isChildModeActiveOnDevice: Boolean,
    onToggleChildModeActive: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val db = remember { FirebaseFirestore.getInstance() }
    val isEnglish = GlobalLanguage.isEnglish

    // Pairing pin state
    var myPairingCode by remember { mutableStateOf("") }
    var screenLimitMinutes by remember { mutableIntStateOf(60) }
    var isGenerating by remember { mutableStateOf(false) }

    // REAL TIME remote flags from Firestore
    var isRemoteScreenSuspended by remember { mutableStateOf(false) }
    var remoteLockedApps by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var currentActiveSimulatedApp by remember { mutableStateOf("") }

    // Simulated Dialog overlays
    var currentBlockedAppPopupTitle by remember { mutableStateOf<String?>(null) }

    // Custom overlay system simulated permission flows status
    var permissionOverlayGranted by remember { mutableStateOf(false) }
    var permissionUsageGranted by remember { mutableStateOf(false) }
    var permissionNotifGranted by remember { mutableStateOf(false) }

    // Active popup notifications trigger models
    var activeShowPermissionRequestDialog by remember { mutableStateOf<String?>(null) }

    // Real-Time listener on Parental Configs in FB for child ID
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            db.collection("parental_configs")
                .document(currentUserId)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null && snapshot.exists()) {
                        screenLimitMinutes = snapshot.getDouble("screenTimeLimit")?.toInt() ?: 60
                        isRemoteScreenSuspended = snapshot.getBoolean("isScreenOff") ?: false
                        remoteLockedApps = snapshot.get("lockedApps") as? Map<String, Boolean> ?: emptyMap()
                        
                        // If parents remotely locked an app that child is currently using, force close it real-time!
                        val lowerActive = currentActiveSimulatedApp.lowercase().replace(" ", "_")
                        if (currentActiveSimulatedApp.isNotEmpty() && remoteLockedApps[lowerActive] == true) {
                            currentBlockedAppPopupTitle = currentActiveSimulatedApp
                            currentActiveSimulatedApp = ""
                        }
                    }
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Simulated permission setup progress block
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "Safeguard Parental Permissions" else "প্যারেন্টাল সিকিউরিটি অনুমোদন",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnglish) 
                                "Required permissions for real-time overlay blockage and system tracking." 
                                else "লাইভ ট্র্যাকিং এবং তাৎক্ষণিক অ্যাপ ব্লক সক্ষম করার অনুমতিসমূহ।",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Permission Item 1: Overlay
                        PermissionRow(
                            title = if (isEnglish) "Draw Over Apps Overlay" else "অন্যান্য অ্যাপের উপরে দেখানোর অনুমতি",
                            isGranted = permissionOverlayGranted,
                            onClick = {
                                if (!permissionOverlayGranted) {
                                    activeShowPermissionRequestDialog = "overlay"
                                }
                            }
                        )

                        // Permission Item 2: Usage statistics detection
                        PermissionRow(
                            title = if (isEnglish) "Device Usage AccessStats" else "ডিভাইস ব্যবহার ডেটা এবং ট্র্যাকিং অ্যাক্সেস",
                            isGranted = permissionUsageGranted,
                            onClick = {
                                if (!permissionUsageGranted) {
                                    activeShowPermissionRequestDialog = "usage"
                                }
                            }
                        )

                        // Permission Item 3: Post Android heads-up notifications
                        PermissionRow(
                            title = if (isEnglish) "Realtime Guardian Alerts Notifications" else " can push alarms as guardian alerts",
                            isGranted = permissionNotifGranted,
                            onClick = {
                                if (!permissionNotifGranted) {
                                    activeShowPermissionRequestDialog = "notif"
                                }
                            }
                        )
                    }
                }
            }

            // Real-Time Local child Mode Active toggle card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isChildModeActiveOnDevice) Color(0xFFE0F2F1) else Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isChildModeActiveOnDevice) PrimaryGreen else Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FamilyRestroom,
                                    contentDescription = null,
                                    tint = if (isChildModeActiveOnDevice) PrimaryGreen else TextGray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isEnglish) "Lock Child Screen Mode" else "ডিভাইসটি লকড শিশু মোড করুন",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = if (isChildModeActiveOnDevice) {
                                            if (isEnglish) "System settings are locked inside child account." 
                                            else "ডিভাইসটি সুরক্ষিত রয়েছে। অভিভাবক পিন ছাড়া বের হওয়া যাবে না।"
                                        } else {
                                            if (isEnglish) "Enables local protection lock instantly." 
                                            else "শিশুর জন্য ফোনের প্যারেন্টাল কন্ট্রোল ট্যাব সম্পূর্ণ বন্ধ করুন।"
                                        },
                                        fontSize = 11.sp,
                                        color = TextGray
                                    )
                                }
                            }

                            Switch(
                                checked = isChildModeActiveOnDevice,
                                onCheckedChange = { value ->
                                    if (value) {
                                        // Request overlay and usage granted prior to lock
                                        permissionOverlayGranted = true
                                        permissionUsageGranted = true
                                        permissionNotifGranted = true
                                        onToggleChildModeActive(true)
                                    } else {
                                        // Emit instructions to use parent PIN dialog from Portal button
                                        Toast.makeText(context, if (isEnglish) "Enter PIN via top-right 'Parent Portal' button" else "প্যারেন্ট পোর্টাল বাটনে চেপে ৪-ডিজিট পিন দিয়ে মোড বদল করুন।", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryGreen
                                )
                            )
                        }
                    }
                }
            }

            // Screen time countdown illustration
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "countdown")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(12000, easing = LinearEasing)
                                ),
                                label = "spinner_rotate"
                            )

                            Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                drawArc(
                                    color = Color(0xFFE2E8F0),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = PrimaryGreen,
                                    startAngle = rotation - 90f,
                                    sweepAngle = 240f,
                                    useCenter = false,
                                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isEnglish) "Screen Time" else "ব্যবহারের সময়",
                                    fontSize = 10.sp,
                                    color = TextGray,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$screenLimitMinutes",
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryGreen
                                )
                                Text(
                                    text = if (isEnglish) "Mins Allocated" else "মিনিট অবশিষ্ট",
                                    fontSize = 10.sp,
                                    color = TextGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isEnglish) "Child Protection Mode Enabled" else "চাইল্ড প্রটেকশন মোড সক্রিয় রয়েছে",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnglish) 
                                "All statistics, location, and educational activities are linked to your parents dashboard automatically." 
                                else "আপনার অবস্থান, প্রতিদিনের কুরআন পাঠ ও সালাতের প্রগ্রেস অভিভাবকের ফোনে স্বয়ংক্রিয়ভাবে সিঙ্ক হচ্ছে।",
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // SIMULATED INSTANT ACTIVE APP LAUNCHER MATRICES
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Launch, contentDescription = null, tint = Color(0xFF3B82F6))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "Installed Apps (Sandbox Simulator)" else "ইনস্টলড অ্যাপস (স্যান্ডবক্স সিমুলেশন)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextDark
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isEnglish) 
                                "Tap an application logo to launch. If locked by parents, block alert popup shows immediately." 
                                else "যেকোনো অ্যাপ চালুর ট্রায়াল করতে স্পর্শ করুন। অভিভাবক লক করে রাখলে সতর্কবার্তা স্ক্রিন দেখা যাবে।",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Sandbox simulation apps mapping to icons
                        val sandboxApps = listOf(
                            AppLauncherItem("Facebook", "facebook", Icons.Default.ThumbUp, Color(0xFF1877F2)),
                            AppLauncherItem("YouTube", "youtube", Icons.Default.PlayArrow, Color(0xFFFF0000)),
                            AppLauncherItem("TikTok", "tiktok", Icons.Default.MusicNote, Color(0xFF010101)),
                            AppLauncherItem("Instagram", "instagram", Icons.Default.CameraAlt, Color(0xFFE1306C)),
                            AppLauncherItem("Telegram", "telegram", Icons.Default.Send, Color(0xFF0088CC)),
                            AppLauncherItem("WhatsApp", "whatsapp", Icons.Default.Call, Color(0xFF25D366)),
                            AppLauncherItem("Google Chrome", "google_chrome", Icons.Default.Language, Color(0xFF4285F4))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sandboxApps.forEach { app ->
                                val isLockedByRemote = remoteLockedApps[app.key] ?: false
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                                        .border(
                                            1.dp,
                                            if (isLockedByRemote) Color(0xFFEF4444).copy(alpha = 0.4f) else Color(0xFFE5E7EB),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (isLockedByRemote) {
                                                currentBlockedAppPopupTitle = app.name
                                            } else {
                                                currentActiveSimulatedApp = app.name
                                                // Sync child's active app to Google Firebase configs so parent receives real-time Alerts!
                                                if (currentUserId.isNotEmpty()) {
                                                    db.collection("parental_configs")
                                                        .document(currentUserId)
                                                        .update("activeApp", app.name)
                                                }
                                            }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(app.accentColor.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = app.icon,
                                                    contentDescription = null,
                                                    tint = app.accentColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = app.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = TextDark
                                                )
                                                Text(
                                                    text = if (isLockedByRemote) {
                                                        if (isEnglish) "Status: App Locked Remotely" else "স্ট্যাটাস: অভিভাবক দ্বারা ব্লকড"
                                                    } else {
                                                        if (isEnglish) "Status: Safe to access" else "স্ট্যাটাস: ব্যবহারের অনুমতি আছে"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = if (isLockedByRemote) Color(0xFFEF4444) else PrimaryGreen
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = if (isLockedByRemote) Icons.Default.Lock else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = if (isLockedByRemote) Color(0xFFEF4444) else Color(0xFF9CA3AF)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Generate pairing PIN
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEnglish) "Device Pairing Code" else "ডিভাইস পেয়ারিং কোড",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isEnglish) "To pair with parent's phone, generate code and type it into parent's app." 
                                   else "অভিভাবকের ফোনের সাথে লিঙ্ক করতে নিচের বাটনে চাপ দিয়ে প্রাপ্ত কোডটি টাইপ করতে বলুন।",
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (myPairingCode.isNotEmpty()) {
                            Text(
                                text = myPairingCode.chunked(3).joinToString(" "),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                letterSpacing = 4.sp,
                                modifier = Modifier
                                    .background(PrimaryGreen.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (currentUserId.isEmpty()) {
                                    Toast.makeText(context, if (isEnglish) "Authenticate first" else "প্রথমে লগইন করুন", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isGenerating = true
                                val pinCode = (100000..999999).random().toString()
                                val docData = mapOf(
                                    "childUid" to currentUserId,
                                    "childName" to (FirebaseAuth.getInstance().currentUser?.displayName ?: "Deen Child"),
                                    "timestamp" to System.currentTimeMillis()
                                )
                                db.collection("pairing_codes")
                                    .document(pinCode)
                                    .set(docData)
                                    .addOnSuccessListener {
                                        myPairingCode = pinCode
                                        isGenerating = false
                                    }
                                    .addOnFailureListener {
                                        isGenerating = false
                                    }
                            },
                            enabled = !isGenerating,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = if (myPairingCode.isEmpty()) {
                                        if (isEnglish) "Generate One-Time Pair Code" else "ওয়ান-টাইম পেয়ারিং কোড তৈরি করুন"
                                    } else {
                                        if (isEnglish) "Generate New Code" else "নতুন কোড তৈরি করুন"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. REAL-TIME PARENTAL SCREEN SUSPENSION IMMERSIVE BLACK OVERLAY
        if (isRemoteScreenSuspended) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070B0E))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneLocked,
                        contentDescription = "Screen Suspended",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = if (isEnglish) "Device Suspended By Parents" else "ডিভাইসের স্ক্রিন বন্ধ রাখা হয়েছে",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isEnglish) 
                            "In order to help you focus on outer physical activities, prayer or studies, screen is locked remotely in real-time."
                            else "আপনার পড়াশোনা, খেলাধুলা কিংবা সালাতে মন দিতে অভিভাবক দূর থেকে এই ফোনের লকড মোডে রেখেছে।",
                        fontSize = 14.sp,
                        color = Color(0xFF9EABB8),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // 2. SIMULATED FULLSCREEN LAUNCHED APP WINDOW WITH TOP DOCK CONTROLLER
        if (currentActiveSimulatedApp.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3F4F6))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SettingsCell,
                                contentDescription = null,
                                tint = PrimaryGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Simulating: $currentActiveSimulatedApp",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }

                        // Close simulated window button
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFECEB), RoundedCornerShape(12.dp))
                                .clickable {
                                    currentActiveSimulatedApp = ""
                                    // Flush active App to empty value in firestore databases
                                    if (currentUserId.isNotEmpty()) {
                                        db.collection("parental_configs")
                                            .document(currentUserId)
                                            .update("activeApp", "")
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "CLOSE APP" else "অ্যাপ বন্ধ করুন",
                                fontSize = 10.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Simulated application workspace views depending on choice
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleFilled,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Browsing Content Safely...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEnglish) 
                                    "Google Chrome & installed apps activities are simulated. Parents can block this immediately in real-time."
                                    else "সিমুলেশন চলছে। অভিভাবক চাইলে তার ডিভাইস থেকে ইনস্ট্যান্ট এটি ব্লক করতে পারবে।",
                                fontSize = 12.sp,
                                color = TextGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 3. APPLICATION BLOCKED POPUP DIALOG OVERLAY (REAL-TIME NOTIF)
        currentBlockedAppPopupTitle?.let { appName ->
            Dialog(onDismissRequest = { currentBlockedAppPopupTitle = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Blocked",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isEnglish) "$appName is Blocked!" else "$appName লক করা রয়েছে!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isEnglish) 
                                "This application is restricted by your parents under parental controls. Please focus on study, recitation or outer sports."
                                else "সংযমের স্বার্থে এবং সালাত ও অধ্যয়নে মনোযোগ দিতে এই অ্যাপটি ব্লক রাখা হয়েছে। অভিভাবককে কথা বলতে বলুন।",
                            fontSize = 13.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { currentBlockedAppPopupTitle = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isEnglish) "Okay" else "ঠিক আছে",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. ACTION INTERACTIVE POPUPS FOR PERMISSIONS ENFORCEMENT ON CHILD DEV
        activeShowPermissionRequestDialog?.let { permType ->
            Dialog(onDismissRequest = { activeShowPermissionRequestDialog = null }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Permission Alert",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (permType == "overlay") {
                                if (isEnglish) "Request Overlay Drawing Permission" else "অ্যাপস ওভারলে আঁকার অনুমতি"
                            } else if (permType == "usage") {
                                if (isEnglish) "Request Device Usage Activity Access" else "ডিভাইস ব্যবহার ট্র্যাক করার অনুমতি"
                            } else {
                                if (isEnglish) "Request Post Floating Notifications Alert" else "সামনে পপআপ নোটিফিকেশনের অনুমতি"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (permType == "overlay") {
                                if (isEnglish) "Allow app-overlay drawings to show blocked screens dynamically when you use blocked assets." 
                                else "ব্লকড অ্যাপ চালানোর চেষ্টা করলে স্ক্রিন তাৎক্ষণিক লক করার জন্য এই অনুমতি প্রয়োজন।"
                            } else if (permType == "usage") {
                                if (isEnglish) "Allow system statistics updates in real-time to check app launching status values." 
                                else "ডিভাইসের অ্যাপস চালু সংক্রান্ত লাইভ স্ট্যাটাস পরীক্ষা করতে এই অ্যাক্টিভিটি অনুমোদন দিন।"
                            } else {
                                if (isEnglish) "Allow parent alert push system configurations on background intervals." 
                                else "অভিভাবককে ব্যাকগ্রাউন্ডে সতর্কবার্তা পাঠাতে নোটিফিকেশন সুবিধা সক্রিয় করুন।"
                            },
                            fontSize = 12.sp,
                            color = TextGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { activeShowPermissionRequestDialog = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isEnglish) "Deny" else "প্রত্যাখ্যান",
                                    color = TextGray
                                )
                            }
                            Button(
                                onClick = {
                                    if (permType == "overlay") permissionOverlayGranted = true
                                    else if (permType == "usage") permissionUsageGranted = true
                                    else permissionNotifGranted = true
                                    
                                    activeShowPermissionRequestDialog = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isEnglish) "Allow / Grant" else "অনুমোদন করুন",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRow(
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    val isEnglish = GlobalLanguage.isEnglish
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF10B981) else Color(0xFF9CA3AF),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
        }

        Box(
            modifier = Modifier
                .background(
                    if (isGranted) Color(0xFFE6F4EA) else Color(0xFFEBF5FF),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isGranted) {
                    if (isEnglish) "GRANTED" else "অনুমোদিত"
                } else {
                    if (isEnglish) "ALLOW" else "অনুমতি দিন"
                },
                fontSize = 10.sp,
                color = if (isGranted) Color(0xFF10B981) else Color(0xFF3B82F6),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

data class AppLauncherItem(
    val name: String,
    val key: String,
    val icon: ImageVector,
    val accentColor: Color
)
