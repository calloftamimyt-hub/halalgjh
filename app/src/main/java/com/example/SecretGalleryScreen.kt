package com.example

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.toBengali
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// Representation of a Hidden Photo item
data class SecretPhotoItem(
    val file: File,
    val name: String,
    val sizeFormatted: String,
    val dateAddedFormatted: String
)

// Extension to map numbers to Bengali digits locally
private fun String.toBnDigits(): String {
    val bndigits = listOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return this.map { if (it.isDigit()) bndigits[it - '0'] else it }.joinToString("")
}

private fun Long.toBnDigits(): String {
    return this.toString().toBnDigits()
}

private fun Int.toBnDigits(): String {
    return this.toString().toBnDigits()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretGalleryScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("secret_gallery_prefs", Context.MODE_PRIVATE) }

    // Lock Screen Configuration
    var savedPin by remember { mutableStateOf(sharedPrefs.getString("gallery_security_pin", "") ?: "") }
    var isUnlocked by remember { mutableStateOf(false) }
    var showPrivacyFullScreen by remember { mutableStateOf(false) }

    // Intercept back actions
    BackHandler {
        if (showPrivacyFullScreen) {
            showPrivacyFullScreen = false
        } else if (isUnlocked) {
            isUnlocked = false // Relock on back
        } else {
            onBack()
        }
    }

    if (showPrivacyFullScreen) {
        SecretGalleryPrivacyScreen(onBack = { showPrivacyFullScreen = false })
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            containerColor = Color(0xFFF1F5F9)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (savedPin.isEmpty()) {
                    // Flow A: Create New PIN Screen + Security Question
                    PinSetupFlow(
                        onPinAndSecuritySet = { newPin, questionIndex, answer ->
                            sharedPrefs.edit().apply {
                                putString("gallery_security_pin", newPin)
                                putInt("gallery_security_question_idx", questionIndex)
                                putString("gallery_security_answer", answer.trim().lowercase())
                            }.apply()
                            savedPin = newPin
                            isUnlocked = true
                            Toast.makeText(context, "গোপন পিন কোড সফলভাবে সেট করা হয়েছে!", Toast.LENGTH_LONG).show()
                        },
                        onBack = onBack
                    )
                } else if (!isUnlocked) {
                    // Flow B: Screen is Locked -> Authenticate PIN Entry
                    PinUnlockFlow(
                        correctPin = savedPin,
                        sharedPrefs = sharedPrefs,
                        onUnlockSuccess = {
                            isUnlocked = true
                            Toast.makeText(context, "গোপন গ্যালারি আনলকড!", Toast.LENGTH_SHORT).show()
                        },
                        onResetPin = {
                            sharedPrefs.edit().remove("gallery_security_pin").apply()
                            savedPin = ""
                            isUnlocked = false
                            Toast.makeText(context, "নিরাপত্তা পিন রিসেট করা হয়েছে। নতুন পিন নির্ধারণ করুন।", Toast.LENGTH_LONG).show()
                        },
                        onBack = onBack
                    )
                } else {
                    // Flow C: Authenticated -> Show Secret Gallery Contents
                    SecretGalleryDashboard(
                        onShowTerms = { showPrivacyFullScreen = true },
                        onLockAgain = { isUnlocked = false },
                        onResetSecurity = {
                            sharedPrefs.edit().remove("gallery_security_pin").apply()
                            savedPin = ""
                            isUnlocked = false
                        },
                        onBack = onBack
                    )
                }
            }
        }
    }
}

/**
 * 1. UI Flow for setting up a fresh PIN and Security question
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupFlow(
    onPinAndSecuritySet: (pin: String, qIndex: Int, answer: String) -> Unit,
    onBack: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) } // 1: Enter, 2: Confirm, 3: Security Question Setup
    var errorMessage by remember { mutableStateOf("") }

    val securityQuestions = listOf(
        "আপনার জন্মস্থান কোন জেলায়?",
        "আপনার প্রিয় শৈশব শিক্ষকের নাম কী?",
        "আপনার প্রথম প্রাইমারি স্কুলের নাম কী?",
        "আপনার প্রিয় শখের বা পছন্দের কাজ কী?"
    )

    if (step == 3) {
        SecurityQuestionSetupStep(
            questions = securityQuestions,
            onComplete = { idx, ans ->
                onPinAndSecuritySet(enteredPin, idx, ans)
            },
            onBackToPin = {
                enteredPin = ""
                confirmPin = ""
                step = 1
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC)) // Sleek white theme canvas
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App top navigation back
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে", tint = Color(0xFF0F172A))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Shield design
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color(0xFF10B981).copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (step == 1) "গোপন গ্যালারি পাসকোড সেট করুন" else "পাসকোডটি পুনরায় নিশ্চিত করুন",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (step == 1) "আপনার ব্যক্তিগত ফটো বা নথির সুরক্ষায় একটি ৪-ডিজিটের পিন কোড দিন।" 
                       else "সঠিকতা নিশ্চিত করতে আগের পিন কোডটি পুনরায় টাইপ করুন।",
                fontSize = 13.sp,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Display Dots representing Pin entry length
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val activeLength = if (step == 1) enteredPin.length else confirmPin.length
                for (i in 0 until 4) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < activeLength) Color(0xFF10B981) else Color(0xFFCBD5E1)
                            )
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFF43F5E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Visual Keypad 0-9
            PinVisualKeypad(
                modifier = Modifier.fillMaxWidth(0.85f),
                onNumberClick = { num ->
                    errorMessage = ""
                    if (step == 1) {
                        if (enteredPin.length < 4) {
                            enteredPin += num
                            if (enteredPin.length == 4) {
                                step = 2
                            }
                        }
                    } else {
                        if (confirmPin.length < 4) {
                            confirmPin += num
                            if (confirmPin.length == 4) {
                                if (enteredPin == confirmPin) {
                                    step = 3 // Move to Security question Setup!
                                } else {
                                    errorMessage = "পিন দুটি মেলেনি। পুনরায় চেষ্টা করুন!"
                                    enteredPin = ""
                                    confirmPin = ""
                                    step = 1
                                }
                            }
                        }
                    }
                },
                onDeleteClick = {
                    errorMessage = ""
                    if (step == 1) {
                        if (enteredPin.isNotEmpty()) {
                            enteredPin = enteredPin.dropLast(1)
                        }
                    } else {
                        if (confirmPin.isNotEmpty()) {
                            confirmPin = confirmPin.dropLast(1)
                        } else {
                            // Go back to step 1
                            step = 1
                        }
                    }
                }
            )
        }
    }
}

/**
 * Secondary Setup Sub-step for Security Questions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityQuestionSetupStep(
    questions: List<String>,
    onComplete: (questionIdx: Int, answer: String) -> Unit,
    onBackToPin: () -> Unit
) {
    var selectedIdx by remember { mutableStateOf(0) }
    var answer by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Nav header back to PIN setup
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = onBackToPin,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF1F5F9), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে", tint = Color(0xFF0F172A))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Shield logo
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFF10B981).copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "নিরাপত্তা প্রশ্ন সেট করুন",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "পিন ভুলে গেলে তা পুনরুদ্ধার করার নিরাপদ মাধ্যম হিসেবে নিচের যেকোনো একটি প্রশ্ন বেছে নিয়ে উত্তরটি দিয়ে রাখুন। এটি পিন রিসেট করার নিরাপত্তা বাড়াবে।",
            fontSize = 13.sp,
            color = Color(0xFF475569),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Question Selection dropdown Card
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "একটি প্রশ্ন নির্বাচন করুন",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569)
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = questions[selectedIdx],
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color(0xFF64748B)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
            ) {
                questions.forEachIndexed { idx, q ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = q,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        },
                        onClick = {
                            selectedIdx = idx
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Answer input
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "আপনার উত্তর দিন",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569)
            )

            OutlinedTextField(
                value = answer,
                onValueChange = {
                    answer = it
                    errorMsg = ""
                },
                placeholder = { Text("এখানে আপনার উত্তর লিখুন...", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (errorMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMsg,
                color = Color(0xFFF43F5E),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Save and Complete
        Button(
            onClick = {
                if (answer.trim().isEmpty()) {
                    errorMsg = "দয়া করে প্রশ্নের উত্তরটি নির্দিষ্ট ঘরে লিখুন!"
                } else {
                    onComplete(selectedIdx, answer)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "গোপন গ্যালারি সক্রিয় করুন",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 2. UI Flow to unlock utilizing correct PIN entries
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinUnlockFlow(
    correctPin: String,
    sharedPrefs: android.content.SharedPreferences,
    onUnlockSuccess: () -> Unit,
    onResetPin: () -> Unit,
    onBack: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    val securityQuestions = listOf(
        "আপনার জন্মস্থান কোন জেলায়?",
        "আপনার প্রিয় শৈশব শিক্ষকের নাম কী?",
        "আপনার প্রথম প্রাইমারি স্কুলের নাম কী?",
        "আপনার প্রিয় শখের বা পছন্দের কাজ কী?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)) // Beautiful light theme background
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF1F5F9), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে", tint = Color(0xFF0F172A))
            }

            // Secure PIN Reset
            TextButton(onClick = { showResetDialog = true }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(15.dp))
                    Text("পিন রিসেট", color = Color(0xFFF43F5E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(35.dp))

        // Large Lock animation
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color(0xFFFFB020).copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFFFFB020),
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "গোপন গ্যালারি আনলক করুন",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "এই সুরক্ষাবলয়টি দেখতে আপনার ৪-ডিজিটের সিক্রেট পিন প্রদান করুন।",
            fontSize = 13.sp,
            color = Color(0xFF475569),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Key Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (i < enteredPin.length) Color(0xFFFFB020) else Color(0xFFCBD5E1)
                        )
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFF43F5E),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Visual Keypad 0-9
        PinVisualKeypad(
            modifier = Modifier.fillMaxWidth(0.85f),
            onNumberClick = { num ->
                errorMessage = ""
                if (enteredPin.length < 4) {
                    enteredPin += num
                    if (enteredPin.length == 4) {
                        if (enteredPin == correctPin) {
                            onUnlockSuccess()
                        } else {
                            errorMessage = "ভুল পিন কোড প্রবেশ করানো হয়েছে! পুনরায় চেষ্টা করুন।"
                            enteredPin = ""
                        }
                    }
                }
            },
            onDeleteClick = {
                errorMessage = ""
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                }
            }
        )
    }

    if (showResetDialog) {
        var resetAnswer by remember { mutableStateOf("") }
        var resetErrorMsg by remember { mutableStateOf("") }
        val savedQuestionIdx = remember { sharedPrefs.getInt("gallery_security_question_idx", 0) }
        val savedAnswer = remember { sharedPrefs.getString("gallery_security_answer", "") ?: "" }

        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFFEF3C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                    }
                    Text("পিন রিসেট নিরাপত্তা যাচাই", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "নিরাপত্তার স্বার্থে পিন রিসেট করার পূর্বে আপনার সিকিউরিটি প্রশ্নের সঠিক উত্তর প্রদান করুন।",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )

                    // Display security question
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "প্রশ্নঃ " + (securityQuestions.getOrNull(savedQuestionIdx) ?: securityQuestions[0]),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.padding(14.dp)
                        )
                    }

                    // Answer input
                    OutlinedTextField(
                        value = resetAnswer,
                        onValueChange = {
                            resetAnswer = it
                            resetErrorMsg = ""
                        },
                        placeholder = { Text("উত্তরটি এখানে লিখুন...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFFB020),
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (resetErrorMsg.isNotEmpty()) {
                        Text(
                            text = resetErrorMsg,
                            color = Color(0xFFF43F5E),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetAnswer.trim().lowercase() == savedAnswer.trim().lowercase()) {
                            showResetDialog = false
                            onResetPin()
                        } else {
                            resetErrorMsg = "ভুল উত্তর! দয়া করে সঠিক উত্তর প্রদান করুন।"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("যাচাই ও রিসেট করুন", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("বাতিল", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        )
    }
}

/**
 * Visual key button template used for setup and unlocking keypad
 */
@Composable
fun PinVisualKeypad(
    modifier: Modifier = Modifier,
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit
) {
    val items = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { item ->
                    if (item.isEmpty()) {
                        Spacer(modifier = Modifier.size(64.dp))
                    } else if (item == "DEL") {
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "মুছুন", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                                .clickable { onNumberClick(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.toBnDigits(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Authenticated - Full Featured Offline Secret Gallery Management Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretGalleryDashboard(
    onShowTerms: () -> Unit,
    onLockAgain: () -> Unit,
    onResetSecurity: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val filesDir = remember { File(context.filesDir, "secret_gallery") }

    // List of active secret photos retrieved from internal directories
    var hiddenPhotosList by remember { mutableStateOf<List<SecretPhotoItem>>(emptyList()) }
    var totalStorageBytes by remember { mutableStateOf(0L) }
    var isReloading by remember { mutableStateOf(false) }

    // Single Image full screen view trigger
    var selectedPreviewPhoto by remember { mutableStateOf<SecretPhotoItem?>(null) }

    // Permanent delete confirmation dialog
    var targetDeletePhoto by remember { mutableStateOf<SecretPhotoItem?>(null) }

    // Re-load file elements locally
    fun updateSourceList() {
        if (!filesDir.exists()) {
            filesDir.mkdirs()
        }
        val rawFiles = filesDir.listFiles()?.filter { it.isFile && (it.name.endsWith(".png") || it.name.endsWith(".jpg") || it.name.endsWith(".jpeg")) } ?: emptyList()
        val sortedList = rawFiles.sortedByDescending { it.lastModified() }
        
        hiddenPhotosList = sortedList.map { file ->
            val sizeKb = file.length() / 1024
            val displaySize = if (sizeKb > 1024) {
                String.format(Locale.getDefault(), "%.2f MB", sizeKb / 1024.0f)
            } else {
                "${sizeKb} KB"
            }
            val formattedDate = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(file.lastModified()))
            SecretPhotoItem(
                file = file,
                name = file.name,
                sizeFormatted = displaySize,
                dateAddedFormatted = formattedDate
            )
        }
        totalStorageBytes = rawFiles.sumOf { it.length() }
    }

    // Trigger initial fetch
    LaunchedEffect(isReloading) {
        updateSourceList()
        if (isReloading) {
            isReloading = false
        }
    }

    // Modern android system gallery picker launcher
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            isReloading = true
            Toast.makeText(context, "মূল গ্যালারি থেকে ফটোটি সরানো হয়েছে!", Toast.LENGTH_SHORT).show()
        }
    }

    val imageSelectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                if (!filesDir.exists()) {
                    filesDir.mkdirs()
                }
                // Generate secure hashed filename to completely hide origins
                val newFileName = "sec_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png"
                val destinationFile = File(filesDir, newFileName)
                
                FileOutputStream(destinationFile).use { outStream ->
                    inputStream.copyTo(outStream)
                }
                inputStream.close()
                
                // Refresh list
                isReloading = true
                Toast.makeText(context, "ফটোটি গোপন গ্যালারিতে সুরক্ষিত করা হয়েছে!", Toast.LENGTH_SHORT).show()

                // "Hide from Gallery" logic: Ask user to delete the original file
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
                    deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                } else {
                    // For older versions, direct deletion often fails for remote URIs without MANAGE_EXTERNAL_STORAGE,
                    // but we can try basic content resolver delete if it's a MediaStore URI
                    try {
                        contentResolver.delete(uri, null, null)
                    } catch (e: Exception) {
                        // Fail silently or notify user if they want to manually delete
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "ফাইল সুরক্ষিত করতে ত্রুটি পাওয়া গেছেঃ ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Photo restore handler to place image safely back in user public storage directories
    fun executeRestore(photo: SecretPhotoItem) {
        try {
            val file = photo.file
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "restored_${System.currentTimeMillis()}.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HalalCircle_Restored")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                resolver.openOutputStream(imageUri).use { out ->
                    if (out != null) {
                        file.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }

                // Delete local file
                file.delete()
                
                // Scan media to alert device UI update immediately
                MediaScannerConnection.scanFile(context, arrayOf(imageUri.toString()), null, null)
                
                // Refresh source dashboard
                isReloading = true
                selectedPreviewPhoto = null
                Toast.makeText(context, "ফটোটি সফলভাবে আপনার মূল গ্যালারিতে ফিরিয়ে দেওয়া হয়েছে!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "গ্যালারি রেকর্ড প্রস্তুত করা যায়নি।", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "পুনরুদ্ধার করা যায়নিঃ ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Permanent delete handler
    fun executeForceDelete(photo: SecretPhotoItem) {
        try {
            val file = photo.file
            if (file.exists()) {
                file.delete()
            }
            isReloading = true
            selectedPreviewPhoto = null
            Toast.makeText(context, "ফাইলটি চিরতরে ডিলেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "ডিলেট করতে সমস্যা হয়েছেঃ ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // High Quality Header Bar
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
                        contentDescription = "প্রস্থান",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "গোপন গ্যালারি",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                // Info icon for Privacy Terms -> Navigates to full screen policy
                IconButton(
                    onClick = onShowTerms,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFEFF6FF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "নিরাপত্তা নীতি",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Relock Action Button
                IconButton(
                    onClick = onLockAgain,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFFFAF0), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "তালা দিন",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Plus action icon to hide photos
                IconButton(
                    onClick = {
                        imageSelectionLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFECFDF5), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "নতুন ছবি যোগ করুন",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Main Contents Scroller
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Storage Consumption gauge widget
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Circle progress gauge
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            color = Color(0xFFE2E8F0),
                            strokeWidth = 6.dp
                        )
                        val calcRatio = (totalStorageBytes.toFloat() / (200 * 1024 * 1024f)).coerceIn(0.01f, 1.0f)
                        CircularProgressIndicator(
                            progress = { calcRatio },
                            color = Color(0xFF10B981),
                            strokeWidth = 6.dp
                        )
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "গোপন স্টোরেজ ব্যবহার",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        
                        val storageUsedFormatted = if (totalStorageBytes > 1024 * 1024) {
                            String.format(Locale.getDefault(), "%.2f MB", totalStorageBytes / (1024.0 * 1024.0))
                        } else {
                            "${totalStorageBytes / 1024} KB"
                        }

                        Text(
                            text = "মোট সুরক্ষিত স্পেস ব্যবহৃতঃ ${storageUsedFormatted.toBnDigits()}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Text(
                            text = "${hiddenPhotosList.size.toBnDigits()}টি ফাইল সুরক্ষিত আছে",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // 2. Custom Security Banner stating details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0F2FE), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFBAE6FD), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF0369A1),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "আপনার ফাইল সম্পূর্ণ নিরাপদ ও ট্র্যাভেলেবল!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1)
                        )
                        Text(
                            text = "ফাইলগুলো আপনার ফোনেই সম্পূর্ণ সুরক্ষিত সিস্টেমে লুকিয়ে রাখা হয়। কোনো ব্রাউজার বা অন্যান্য গ্যালারি এগুলো স্ক্যান করতে পারবেনা।",
                            fontSize = 11.sp,
                            color = Color(0xFF0284C7),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // 3. Grid representation of Secret Photos
            if (hiddenPhotosList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .background(Color.White, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(Color(0xFFF1F5F9), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoPhotography,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "কোনো গোপন ফটো পাওয়া যায়নি!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "উপরের ডানদিকের প্লাস (+) আইকনে ক্লিক করে আপনার গ্যালারি থেকে যেকোনো পার্সোনাল ফটো লুকিয়ে ফেলুন!",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = {
                                imageSelectionLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("গোপন ফটো যোগ করুন", color = Color.White)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সুরক্ষিত ফাইলসমূহ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "পিন বা প্যাটার্ন দিয়ে ব্লকড",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Grid Layout
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val chunkedList = hiddenPhotosList.chunked(3)
                        chunkedList.forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowItems.forEach { item ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .clickable { selectedPreviewPhoto = item }
                                    ) {
                                        // Read bitmap offline safely
                                        val bitmap = remember(item.file.absolutePath) {
                                            try {
                                                val bytes = item.file.readBytes()
                                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }

                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = item.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color(0xFF94A3B8))
                                            }
                                        }

                                        // Lock badge overlay
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(20.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }

                                        // Size badge on bottom leading
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(4.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = item.sizeFormatted.toBnDigits(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                if (rowItems.size < 3) {
                                    val remaining = 3 - rowItems.size
                                    for (i in 0 until remaining) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Reset options & advice
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "নিরাপত্তা ও কনফিগারেশন সেটিংস",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )

                    Divider(color = Color(0xFFF1F5F9))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResetSecurity() }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFF475569))
                            Text("গ্যালারির সিকিউরিটি পিন পরিবর্তন করুন", fontSize = 12.sp, color = Color(0xFF475569))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }

    // A. Dialog: Advanced High Fidelity Image Preview Overlay containing Restore or Force Delete trigger
    selectedPreviewPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { selectedPreviewPhoto = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "সুরক্ষিত ইমেজ অ্যাকশন",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )

                    IconButton(onClick = { selectedPreviewPhoto = null }) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = Color(0xFF64748B))
                    }
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        val bitmap = remember(photo.file.absolutePath) {
                            try {
                                val bytes = photo.file.readBytes()
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = photo.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(48.dp))
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "ফাইলের নামঃ ${photo.name}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "সংরক্ষিত সাইজঃ ${photo.sizeFormatted.toBnDigits()}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "সংরক্ষণের সময়ঃ ${photo.dateAddedFormatted.toBnDigits()}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { executeRestore(photo) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Unarchive, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("গ্যালারিতে ফেরত নিন (Restore)", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { targetDeletePhoto = photo },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E).copy(alpha = 0.1f)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFF43F5E))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("চিরতরে মুছে ফেলুন (Delete)", color = Color(0xFFF43F5E), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Target Perm Delete confirmation dialog
    targetDeletePhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = { targetDeletePhoto = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                    Text("নিশ্চিত কোড মুছবেন?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে আপনি এই ফাইলের রেকর্ডটি চিরতরে মুছে ফেলতে চান? এটি আর কোনোভাবেই পুনরুদ্ধার করা সম্ভব হবে না।",
                    fontSize = 13.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = targetDeletePhoto
                        targetDeletePhoto = null
                        if (toDel != null) {
                            executeForceDelete(toDel)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("হ্যাঁ, ডিলিট করুন", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { targetDeletePhoto = null }) {
                    Text("বাতিল", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        )
    }
}

/**
 * Clean full-screen page explaining Terms and Privacy Policy (replaces Pop-up dialog)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretGalleryPrivacyScreen(
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "গোপন ও নিরাপত্তা নীতিমালা",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Halal Circle Security & Terms",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "পিছনে", tint = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main offline feature intro Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFECFDF5), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "১০০% অফলাইন গ্যারান্টি",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "আপনার গোপন ফটো ডিভাইস থেকে কোনো সার্ভারে প্রেরিত হয় না। এটি সম্পূর্ণ আপনার ফোনেই সুরক্ষিত থাকে এবং Halal Circle কোনো ইন্টারনেট সংযোগে ডেটা চালান করেনা।",
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // High priority terms & conditions catalog
            val terms = listOf(
                Triple(
                    Icons.Default.Lock,
                    "১. তথ্য ও গোপনীয়তা সুরক্ষা (Information Privacy)",
                    "গোপন গ্যালারি ক্যাটাগরির সমস্ত ইমেজ বা অন্যান্য ফাইল একচেটিয়াভাবে আপনার ডিভাইসের অভ্যন্তরীণ সংরক্ষিত মেমোরিতে (Internal Sandboxed sand-directory) এনক্রিপ্ট/আলাদা করে স্থানান্তরিত করা হয়। কোনো ক্লাউড সার্ভার বা ইন্টারনেটে এই ফাইল আপলোড করা হয় না।"
                ),
                Triple(
                    Icons.Default.HideImage,
                    "২. মিডিয়া সংগ্রহশালা অবরুদ্ধকরণ",
                    "ফাইলগুলো সরিয়ে নেওয়ার সাথে সাথে অ্যান্ড্রয়েডের মিডিয়া-স্ক্যানার সিস্টেমে আর ফাইলগুলো খুজে পায় না, যার জন্য ফোনের সাধারণ গ্যালারি অ্যাপগুলোতে আপনার ছবি আর দেখা যায় না।"
                ),
                Triple(
                    Icons.Default.Security,
                    "৩. পিন এবং নিরাপত্তা রিসেট নীতিমালা",
                    "আপনার ব্যক্তিগত ফাইল রক্ষা করতে ৪-ডিজিটের সিকিউরিটি পিন সেট করা আবশ্যক। পিন পুনরুদ্ধার করতে হলে অবশ্যই আপনার নির্ধারিত নিরাপত্তা প্রশ্নটির সঠিক উত্তর প্রদান করতে হবে। আপনার সেট করা নিরাপত্তা প্রশ্নের উত্তর না জানলে পিন রিসেট করা অসম্ভব, যা আপনার ফাইলগুলোকে রাখে শতভাগ সুরক্ষিত।"
                ),
                Triple(
                    Icons.Default.Storage,
                    "৪. ডিভাইস ডেটা প্রাইভেসী",
                    "Halal Circle অ্যাপটি আনইনস্টল করা হলে কিংবা ডিভাইসের ইন্টার্নাল স্টোরেজ ডাটা ক্লিয়ার করা হলে সিক্রেট ডিরেক্টরির ফটো সমূহ হারিয়ে যেতে পারে। তাই কোনো কারণে অ্যাপ ডিলিট বা ডিভাইস রিসেট করার পূর্বে অবশ্যই ফাইলগুলো রিস্টোর করার জন্য অনুরোধ করা হচ্ছে।"
                )
            )

            terms.forEach { (icon, title, desc) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = desc,
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
