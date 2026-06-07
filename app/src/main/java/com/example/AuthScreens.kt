package com.example

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val auth = FirebaseAuth.getInstance()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = BgLight,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            CyberSecurityAnimation()
            
            Spacer(modifier = Modifier.height(30.dp))
            
            Text(
                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Welcome Back" else "স্বাগতম",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            
            Text(
                text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Sign in to your account" else "আপনার অ্যাকাউন্টে লগইন করুন",
                fontSize = 14.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Email Address" else "ইমেইল এড্রেস") },
                modifier = Modifier.fillMaxWidth(0.95f).height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextGray,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = PrimaryGreen
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = if (email.isNotEmpty()) PrimaryGreen else TextGray) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Password" else "পাসওয়ার্ড") },
                modifier = Modifier.fillMaxWidth(0.95f).height(60.dp),
                shape = RoundedCornerShape(30.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark,
                    focusedLabelColor = PrimaryGreen,
                    unfocusedLabelColor = TextGray,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    cursorColor = PrimaryGreen
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, 
                            null,
                            tint = TextGray
                        )
                    }
                },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (password.isNotEmpty()) PrimaryGreen else TextGray) }
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(0.85f),
                    textAlign = TextAlign.Start
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Please fill all fields" else "সবগুলো ঘর পূরণ করুন"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                onLoginSuccess()
                            } else {
                                errorMessage = task.exception?.localizedMessage ?: "Login failed"
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(54.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Login" else "লগইন করুন", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Don't have an account? " else "অ্যাকাউন্ট নেই? ",
                    color = TextGray,
                    fontSize = 14.sp
                )
                Text(
                    text = if (com.example.viewmodel.GlobalLanguage.isEnglish) "Register Now" else "রেজিস্ট্রেশন করুন",
                    color = PrimaryGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }
        }
    }
}


@Composable
fun CyberSecurityAnimation() {
    var rotation by remember { mutableStateOf(0f to 0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "ring_anim")
    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart), label = "rot"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer(
                rotationX = rotation.first,
                rotationY = rotation.second,
                cameraDistance = 12f
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    rotation = (rotation.first - dragAmount.y / 2f) to (rotation.second + dragAmount.x / 2f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp).graphicsLayer(rotationZ = rotationAnim)) {
            drawCircle(color = Color(0x4D6366F1), style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
        }
        Canvas(modifier = Modifier.size(160.dp).graphicsLayer(rotationZ = -rotationAnim)) {
            drawArc(color = Color(0x996366F1), startAngle = 0f, sweepAngle = 180f, useCenter = false, style = Stroke(width = 6f))
            drawArc(color = Color(0x9938BDF8), startAngle = 180f, sweepAngle = 180f, useCenter = false, style = Stroke(width = 6f))
        }
        Box(modifier = Modifier.size(110.dp).background(Color.White, CircleShape).border(2.dp, Color(0x1A6366F1), CircleShape), contentAlignment = Alignment.Center) {
             Icon(Icons.Default.Fingerprint, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(50.dp))
        }
        val laserTransition = rememberInfiniteTransition(label = "laser")
        val laserOffset by laserTransition.animateFloat(initialValue = -60f, targetValue = 60f, animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "laser")
        Box(modifier = Modifier.size(180.dp, 4.dp).offset(y = laserOffset.dp).background(Color(0xFF38BDF8)).graphicsLayer(shadowElevation = 10f))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val auth = FirebaseAuth.getInstance()

    Scaffold(
        containerColor = BgLight,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status bar animation
            CyberSecurityAnimation()
            Spacer(modifier = Modifier.height(40.dp))

            AnimatedContent(targetState = step, label = "step_anim") { targetStep ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    when(targetStep) {
                        0 -> {
                            Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Enter your name" else "আপনার নাম লিখুন", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = firstName, onValueChange = { firstName = it },
                                label = { Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "First Name" else "নামের প্রথম অংশ") },
                                modifier = Modifier.fillMaxWidth(0.95f).height(60.dp),
                                shape = RoundedCornerShape(30.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextDark,
                                    unfocusedTextColor = TextDark,
                                    focusedLabelColor = PrimaryGreen,
                                    unfocusedLabelColor = TextGray,
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = PrimaryGreen
                                ),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = if (firstName.isNotEmpty()) PrimaryGreen else TextGray) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = lastName, onValueChange = { lastName = it },
                                label = { Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Last Name" else "শেষ অংশ") },
                                modifier = Modifier.fillMaxWidth(0.95f).height(60.dp),
                                shape = RoundedCornerShape(30.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextDark,
                                    unfocusedTextColor = TextDark,
                                    focusedLabelColor = PrimaryGreen,
                                    unfocusedLabelColor = TextGray,
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = PrimaryGreen
                                ),
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = if (lastName.isNotEmpty()) PrimaryGreen else TextGray) }
                            )
                        }
                        1 -> {
                            Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Enter email & password" else "ইমেইল ও পাসওয়ার্ড লিখুন", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = email, onValueChange = { email = it },
                                label = { Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Email" else "ইমেইল") },
                                modifier = Modifier.fillMaxWidth(0.95f).height(60.dp),
                                shape = RoundedCornerShape(30.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextDark,
                                    unfocusedTextColor = TextDark,
                                    focusedLabelColor = PrimaryGreen,
                                    unfocusedLabelColor = TextGray,
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = PrimaryGreen
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = if (email.isNotEmpty()) PrimaryGreen else TextGray) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = password, onValueChange = { password = it },
                                label = { Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Password" else "পাসওয়ার্ড") },
                                modifier = Modifier.fillMaxWidth(0.95f).height(60.dp),
                                shape = RoundedCornerShape(30.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextDark,
                                    unfocusedTextColor = TextDark,
                                    focusedLabelColor = PrimaryGreen,
                                    unfocusedLabelColor = TextGray,
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = PrimaryGreen
                                ),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, 
                                            null,
                                            tint = TextGray
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (password.isNotEmpty()) PrimaryGreen else TextGray) }
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = confirmPassword, onValueChange = { confirmPassword = it },
                                label = { Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Confirm Password" else "পাসওয়ার্ড নিশ্চিত") },
                                modifier = Modifier.fillMaxWidth(0.95f).height(60.dp),
                                shape = RoundedCornerShape(30.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextDark,
                                    unfocusedTextColor = TextDark,
                                    focusedLabelColor = PrimaryGreen,
                                    unfocusedLabelColor = TextGray,
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = PrimaryGreen
                                ),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, 
                                            null,
                                            tint = TextGray
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = if (confirmPassword.isNotEmpty()) PrimaryGreen else TextGray) }
                            )
                        }
                        2 -> {
                            Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Enter phone number" else "ফোন নাম্বারটি লিখুন", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedTextField(
                                value = mobile, onValueChange = { mobile = it },
                                label = { Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Mobile Number" else "মোবাইল নাম্বার") },
                                modifier = Modifier.fillMaxWidth(0.95f).height(60.dp),
                                shape = RoundedCornerShape(30.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextDark,
                                    unfocusedTextColor = TextDark,
                                    focusedLabelColor = PrimaryGreen,
                                    unfocusedLabelColor = TextGray,
                                    focusedBorderColor = PrimaryGreen,
                                    unfocusedBorderColor = TextGray.copy(alpha = 0.5f),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    cursorColor = PrimaryGreen
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = if (mobile.isNotEmpty()) PrimaryGreen else TextGray) }
                            )
                        }
                    }
                }
            }
            
            errorMessage?.let {
                Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp).fillMaxWidth(0.8f))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { if (step > 0) step-- else onBack() }) {
                    Text(if (com.example.viewmodel.GlobalLanguage.isEnglish) "Cancel" else "বাতিল", color = TextGray)
                }
                Button(
                    onClick = {
                        if (step < 2) {
                            step++
                        } else {
                            // Perform Register Logic
                            if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || mobile.isBlank() || password.isBlank()) {
                                errorMessage = "Please fill all fields"
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                                return@Button
                            }
                            isLoading = true
                            errorMessage = null
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = task.result?.user
                                        val profileUpdates = UserProfileChangeRequest.Builder()
                                            .setDisplayName("$firstName $lastName")
                                            .build()
                                        
                                        user?.updateProfile(profileUpdates)
                                            ?.addOnCompleteListener { profileTask ->
                                                isLoading = false
                                                if (profileTask.isSuccessful) {
                                                    onRegisterSuccess()
                                                } else {
                                                    onRegisterSuccess()
                                                }
                                            }
                                    } else {
                                        isLoading = false
                                        errorMessage = task.exception?.localizedMessage ?: "Registration failed"
                                    }
                                }
                        }
                    },
                    modifier = Modifier.height(54.dp).padding(horizontal = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (step < 2) (if (com.example.viewmodel.GlobalLanguage.isEnglish) "Next" else "পরবর্তী") else (if (com.example.viewmodel.GlobalLanguage.isEnglish) "Register" else "রেজিস্ট্রেশন"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}
