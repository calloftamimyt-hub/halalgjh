package com.example

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.GlobalLanguage
import com.example.viewmodel.PrayerViewModel

@Composable
fun OnboardingScreen(
    prayerViewModel: PrayerViewModel,
    settingsViewModel: com.example.viewmodel.SettingsViewModel,
    onComplete: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    
    Box(modifier = Modifier.fillMaxSize().background(BgLight)) {
        // Subtle background decoration
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = PrimaryGreen.copy(alpha = 0.05f),
                radius = size.width,
                center = androidx.compose.ui.geometry.Offset(size.width, 0f)
            )
            drawCircle(
                color = PrimaryGreen.copy(alpha = 0.03f),
                radius = size.width * 0.6f,
                center = androidx.compose.ui.geometry.Offset(0f, size.height)
            )
        }

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                slideOutHorizontally { -it } + fadeOut()
            },
            modifier = Modifier.fillMaxSize(),
            label = "StepAnimation"
        ) { currentStep ->
            when (currentStep) {
                1 -> WelcomeStep(
                    onLogin = { step = 2 },
                    onSignUp = { step = 2 },
                    onGuest = { step = 2 }
                )
                2 -> LanguageStep(
                    settingsViewModel = settingsViewModel,
                    onNext = { step = 3 }
                )
                3 -> MadhabStep(
                    prayerViewModel = prayerViewModel,
                    onNext = { step = 4 }
                )
                4 -> LocationStep(
                    prayerViewModel = prayerViewModel,
                    onComplete = onComplete
                )
            }
        }
    }
}

@Composable
fun WelcomeStep(onLogin: () -> Unit, onSignUp: () -> Unit, onGuest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        // High-quality visual icon area
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = PrimaryGreen.copy(alpha = 0.1f)
            ) {}
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = PrimaryGreen.copy(alpha = 0.15f)
            ) {}
            Icon(
                Icons.Default.CheckCircle, 
                contentDescription = null, 
                tint = PrimaryGreen, 
                modifier = Modifier.size(80.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "স্বাগতম!",
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextDark,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "আপনার আত্মিক উন্নয়নের পথে\nএক ধাপ এগিয়ে",
            textAlign = TextAlign.Center,
            color = TextGray,
            fontSize = 18.sp,
            lineHeight = 26.sp
        )
        
        Spacer(modifier = Modifier.weight(1.2f))
        
        // Buttons at the bottom
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("লগইন", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onSignUp,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, PrimaryGreen)
            ) {
                Text("সাইন আপ", fontSize = 18.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(
                onClick = onGuest,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("গেস্ট ইউজার হিসেবে প্রবেশ করুন", color = TextGray, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun LanguageStep(
    settingsViewModel: com.example.viewmodel.SettingsViewModel,
    onNext: () -> Unit
) {
    val language by settingsViewModel.language.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text("ভাষা নির্বাচন করুন", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Text("Select Language", fontSize = 20.sp, color = TextGray)
        
        Spacer(modifier = Modifier.weight(1f))
        
        LanguageOption(
            title = "বাংলা",
            subtitle = "Bengali",
            isSelected = language == com.example.viewmodel.AppLanguage.BENGALI,
            onClick = { 
                settingsViewModel.setLanguage(com.example.viewmodel.AppLanguage.BENGALI)
                GlobalLanguage.isEnglish = false
            }
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        LanguageOption(
            title = "English",
            subtitle = "ইংরেজি",
            isSelected = language == com.example.viewmodel.AppLanguage.ENGLISH,
            onClick = { 
                settingsViewModel.setLanguage(com.example.viewmodel.AppLanguage.ENGLISH)
                GlobalLanguage.isEnglish = true
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(if (language == com.example.viewmodel.AppLanguage.ENGLISH) "Next" else "পরবর্তী", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LanguageOption(title: String, subtitle: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else Color.White,
        border = if (isSelected) BorderStroke(2.dp, PrimaryGreen) else BorderStroke(1.dp, Color(0xFFEEEEEE)),
        shadowElevation = if (isSelected) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
                Text(subtitle, fontSize = 14.sp, color = TextGray)
            }
            Icon(
                if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) PrimaryGreen else Color(0xFFDDDDDD),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun MadhabStep(prayerViewModel: PrayerViewModel, onNext: () -> Unit) {
    val state by prayerViewModel.state.collectAsState()
    val isEnglish = GlobalLanguage.isEnglish
    val context = LocalContext.current
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            if (isEnglish) "Select Madhab" else "মাযহাব নির্বাচন করুন", 
            fontSize = 28.sp, 
            fontWeight = FontWeight.Bold, 
            color = TextDark,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            if (isEnglish) "This affects Asr prayer time calculation" else "এটি আসরের নামাজের সময় গণনায় ব্যবহৃত হবে", 
            fontSize = 15.sp, 
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        MadhabOption(
            title = if (isEnglish) "Hanafi" else "হানাফী",
            description = if (isEnglish) "Asr starts when shadow is twice the length" else "বাংলাদেশে সবচেয়ে বেশি প্রচলিত",
            isSelected = state.madhab == 2,
            onClick = { prayerViewModel.setMadhab(context, 2) }
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        MadhabOption(
            title = if (isEnglish) "Shafi / Maliki / Hanbali" else "শাফেয়ী / মালিকী / হাম্বলী",
            description = if (isEnglish) "Asr starts when shadow equals the length" else "সাধারণত মধ্যপ্রাচ্যে প্রচলিত",
            isSelected = state.madhab == 1,
            onClick = { prayerViewModel.setMadhab(context, 1) }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(if (isEnglish) "Next" else "পরবর্তী", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MadhabOption(title: String, description: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else Color.White,
        border = if (isSelected) BorderStroke(2.dp, PrimaryGreen) else BorderStroke(1.dp, Color(0xFFEEEEEE)),
        shadowElevation = if (isSelected) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, fontSize = 13.sp, color = TextGray)
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = PrimaryGreen)
            )
        }
    }
}

@Composable
fun LocationStep(prayerViewModel: PrayerViewModel, onComplete: () -> Unit) {
    val context = LocalContext.current
    val isEnglish = GlobalLanguage.isEnglish
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = PrimaryGreen.copy(alpha = 0.1f)
        ) {
            Icon(
                Icons.Default.LocationOn, 
                contentDescription = null, 
                tint = PrimaryGreen, 
                modifier = Modifier.padding(32.dp).fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            if (isEnglish) "Set Your Location" else "লোকেশন সেট করুন", 
            fontSize = 28.sp, 
            fontWeight = FontWeight.Bold, 
            color = TextDark
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            if (isEnglish) "Automatically detect location for accurate prayer times" else "সঠিক নামাজের সময়সূচির জন্য অটোমেটিক লোকেশন সেট করুন", 
            fontSize = 16.sp, 
            color = TextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.weight(1.2f))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { 
                    prayerViewModel.setAutoLocation(context) 
                    onComplete()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MyLocation, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (isEnglish) "Auto Location" else "অটোমেটিক লোকেশন", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(0xFFDDDDDD))
            ) {
                Text(if (isEnglish) "Later" else "পরে করব", color = TextGray, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
