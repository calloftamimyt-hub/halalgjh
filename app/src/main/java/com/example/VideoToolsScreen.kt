package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen
import com.example.viewmodel.GlobalLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoToolsScreen() {
    val context = LocalContext.current
    var selectedTool by remember { mutableStateOf("aspect") } // "aspect", "tags", "srt"

    // Hashtags Generator States
    var selectedTagCategory by remember { mutableStateOf("বয়ান (Bayan)") }
    var generatedTags by remember { mutableStateOf("") }

    // Caption Helper States
    var captionInputText by remember { mutableStateOf("") }
    var generatedSrt by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(top = 95.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Screen Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (GlobalLanguage.isEnglish) "Creator Tools" else "ক্রিয়েটর টুলস",
                    color = Color(0xFF111827),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (GlobalLanguage.isEnglish) "Optimize, format, and enhance your Islamic media posts" else "আপনার ডেকোরেশন, সাইজ এবং সঠিক অপ্টিমাইজেশন পরিচালনা করুন",
                    color = Color(0xFF4B5563),
                    fontSize = 13.sp
                )
            }
        }

        // Horizontal Category Tab Bar selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val toolsOptions = listOf(
                    Triple("aspect", if (GlobalLanguage.isEnglish) "Aspect Ratio" else "অ্যাসপেক্ট রেশিও", Icons.Default.AspectRatio),
                    Triple("tags", if (GlobalLanguage.isEnglish) "Hashtag Generator" else "হ্যাশট্যাগ মেকার", Icons.Default.Tag),
                    Triple("srt", if (GlobalLanguage.isEnglish) "Subtitle Formatter" else "সাবটাইটেল মেকার", Icons.Default.ClosedCaption)
                )

                toolsOptions.forEach { opt ->
                    val isSelected = selectedTool == opt.first
                    Surface(
                        onClick = { selectedTool = opt.first },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) PrimaryGreen else Color(0xFFF3F4F6),
                        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = opt.third,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color(0xFF4B5563),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = opt.second,
                                color = if (isSelected) Color.White else Color(0xFF111827),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Conditional display based on active tool selection
        item {
            AnimatedContent(
                targetState = selectedTool,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "tools_transition"
            ) { targetTool ->
                when (targetTool) {
                    "aspect" -> {
                        AspectSimulatorCard()
                    }
                    "tags" -> {
                        Surface(
                            color = Color(0xFFF9FAFB),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = if (GlobalLanguage.isEnglish) "Islamic Hashtag Generator" else "ইসলামিক হ্যাশট্যাগ জেনারেটর",
                                    color = Color(0xFF111827),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (GlobalLanguage.isEnglish) "Select video topic to build trending Dawah tags:" else "ভাইরাল ইসলাম প্রচার ভিত্তিক ট্যাগ পেতে ক্যাটাগরি বেছে নিন:",
                                    color = Color(0xFF4B5563),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val tagsCategories = listOf("বয়ান (Bayan)", "তিলাওয়াত (Recitation)", "নাশিদ (Nasheed)", "শিক্ষা (Education)", "হাদিস (Hadith)")
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    tagsCategories.forEach { cat ->
                                        val active = selectedTagCategory == cat
                                        Surface(
                                            onClick = { selectedTagCategory = cat },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (active) PrimaryGreen.copy(alpha = 0.15f) else Color.Transparent,
                                            border = BorderStroke(1.dp, if (active) PrimaryGreen else Color(0xFFD1D5DB))
                                        ) {
                                            Text(
                                                text = cat,
                                                color = if (active) PrimaryGreen else Color(0xFF4B5563),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        generatedTags = when (selectedTagCategory) {
                                            "বয়ান (Bayan)" -> "#IslamicBayan #Waz #Dawah #IslamicLecture #Knowledge #HalalVlog #CallToSunnah"
                                            "তিলাওয়াত (Recitation)" -> "#QuranRecitation #BeautifulQuran #HolyQuran #IslamicVideos #Tranquility #QuranTilawat"
                                            "নাশিদ (Nasheed)" -> "#Nasheed #HalalMusic #VocalOnly #IslamicSong #Peaceful #DeenNasheed"
                                            "শিক্ষা (Education)" -> "#IslamicEducation #MadrasahKids #HalalKnowledge #IslamicHistory #LearnQuran"
                                            "হাদিস (Hadith)" -> "#HadithToday #SahihBukhari #SunnahWay #ProphetMuhammad #WayOfLife #HalalCoterie"
                                            else -> "#HalalCircle #IslamicVideos"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (GlobalLanguage.isEnglish) "Generate Tags" else "ট্যাগ তৈরি করুন",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                if (generatedTags.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Surface(
                                        color = Color(0xFFF3F4F6),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = generatedTags,
                                                color = PrimaryGreen,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(generatedTags))
                                                    Toast.makeText(context, if (GlobalLanguage.isEnglish) "Tags copied!" else "কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E7EB)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF111827), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (GlobalLanguage.isEnglish) "Copy" else "কপি করুন",
                                                    color = Color(0xFF111827),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "srt" -> {
                        Surface(
                            color = Color(0xFFF9FAFB),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = if (GlobalLanguage.isEnglish) "Quick Subtitle Formatter (SRT)" else "SRT সাবটাইটেল ফরমেটার",
                                    color = Color(0xFF111827),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (GlobalLanguage.isEnglish) "Type/Paste text below to generate cleanly indexed SRT blocks with timestamps:" else "নিচে আপনার টেক্সট লিখুন, দ্রুত সময়ের সাবটাইটেল কোড ব্লকে রূপান্তর করতে:",
                                    color = Color(0xFF4B5563),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = captionInputText,
                                    onValueChange = { captionInputText = it },
                                    label = { Text(if (GlobalLanguage.isEnglish) "Type lyrics or Waz lines..." else "গানের লিরিক্স অথবা কথা লিখুন...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimaryGreen,
                                        focusedLabelColor = PrimaryGreen,
                                        unfocusedTextColor = Color(0xFF111827),
                                        focusedTextColor = Color(0xFF111827),
                                        unfocusedLabelColor = Color(0xFF4B5563),
                                        unfocusedBorderColor = Color(0xFFD1D5DB)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        if (captionInputText.isBlank()) {
                                            Toast.makeText(context, if (GlobalLanguage.isEnglish) "Enter text first!" else "কিছু লিখুন!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val lines = captionInputText.split("\n").filter { it.isNotBlank() }
                                            val srtBuilder = StringBuilder()
                                            lines.forEachIndexed { idx, line ->
                                                val startSec = idx * 4
                                                val endSec = startSec + 3
                                                val startTime = String.format("00:00:%02d,000", startSec)
                                                val endTime = String.format("00:00:%02d,000", endSec)
                                                srtBuilder.append("${idx + 1}\n")
                                                srtBuilder.append("$startTime --> $endTime\n")
                                                srtBuilder.append("$line\n\n")
                                            }
                                            generatedSrt = srtBuilder.toString()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.BuildCircle, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (GlobalLanguage.isEnglish) "Convert to SRT Code" else "কনভার্ট করুন",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                if (generatedSrt.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Surface(
                                        color = Color(0xFF1F2937),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, Color(0xFF374151)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = generatedSrt,
                                                color = Color(0xFFFBBF24),
                                                fontSize = 12.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState())
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(generatedSrt))
                                                    Toast.makeText(context, if (GlobalLanguage.isEnglish) "SRT Copied!" else "SRT কোড কপিড!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.align(Alignment.End)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (GlobalLanguage.isEnglish) "Copy SRT" else "কোড কপি করুন",
                                                    color = Color.White,
                                                    fontSize = 11.sp
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
        }
    }
}

@Composable
fun AspectSimulatorCard() {
    var ratioIndex by remember { mutableStateOf(0) } // 0 -> 9:16, 1 -> 1:1, 2 -> 16:9
    val aspectSpecs = listOf(
        Triple("৯:১৬ (Portrait)", 0.56f, if (GlobalLanguage.isEnglish) "Best for Reels / Waz Highlights" else "মোবাইল রিলস ও টিকটকের জন্য শ্রেষ্ঠ"),
        Triple("১:১ (Square)", 1.0f, if (GlobalLanguage.isEnglish) "Best for General Social Feed" else "ফেসবুক ও সোশ্যাল ফিড পোস্টের শ্রেষ্ঠ"),
        Triple("১৬:৯ (Landscape)", 1.77f, if (GlobalLanguage.isEnglish) "Best for Full Lectures & Bayans" else "বড় বয়ান এবং তিলাওয়াত ভিডিওর শ্রেষ্ঠ")
    )

    Surface(
        color = Color(0xFFF9FAFB),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (GlobalLanguage.isEnglish) "Interactive Aspect Ratio Simulator" else "অ্যাসপেক্ট রেশিও সিমুলেটর",
                color = Color(0xFF111827),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Box Simulating aspect bounds
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 240.dp)
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Interactive inner active bounds box
                val currentSpec = aspectSpecs[ratioIndex]
                val currentWidth = if (currentSpec.second <= 1f) (200.dp * currentSpec.second) else 200.dp
                val currentHeight = if (currentSpec.second > 1f) (200.dp / currentSpec.second) else 200.dp

                Box(
                    modifier = Modifier
                        .size(width = currentWidth, height = currentHeight)
                        .background(PrimaryGreen.copy(alpha = 0.15f))
                        .border(2.dp, PrimaryGreen, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Crop, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentSpec.first.substringBefore(" ("),
                            color = Color(0xFF111827),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = aspectSpecs[ratioIndex].first,
                color = Color(0xFF111827),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = aspectSpecs[ratioIndex].third,
                color = Color(0xFF4B5563),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                aspectSpecs.forEachIndexed { idx, item ->
                    val isSelected = ratioIndex == idx
                    Button(
                        onClick = { ratioIndex = idx },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) PrimaryGreen else Color(0xFFF3F4F6)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.first.substringBefore(" ("),
                            color = if (isSelected) Color.White else Color(0xFF4B5563),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
