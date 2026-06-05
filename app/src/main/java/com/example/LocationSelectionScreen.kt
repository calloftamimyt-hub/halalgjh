package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.PrayerViewModel
import com.example.viewmodel.bangladeshDistricts
import com.example.viewmodel.countries
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSelectionScreen(
    viewModel: PrayerViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedCountry by remember { mutableStateOf(state.selectedCountry) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("লোকেশন সেট করুন", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Auto Location Button
            Card(
                onClick = { 
                    viewModel.setAutoLocation(context)
                    onBack()
                },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryGreen)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("স্বয়ংক্রিয় লোকেশন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("আপনার জিপিএস ব্যবহার করে সময় বের করুন", fontSize = 12.sp, color = TextGray)
                    }
                    if (state.isAutoLocation) {
                         Spacer(modifier = Modifier.weight(1f))
                         Icon(Icons.Default.LocationOn, contentDescription = "Active", tint = PrimaryGreen)
                    }
                }
            }

            Text(
                "ম্যানুয়ালি সেট করুন",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                color = TextGray,
                fontSize = 14.sp
            )

            // Country Selector
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Text(
                        "দেশ নির্বাচন করুন",
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
                
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        countries.forEach { country ->
                            val isSelected = selectedCountry == country
                            Surface(
                                onClick = { selectedCountry = country },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else Color.White,
                                border = if (isSelected) BorderStroke(1.dp, PrimaryGreen) else BorderStroke(1.dp, Color(0xFFE5E7EB))
                            ) {
                                Text(
                                    country,
                                    modifier = Modifier.padding(16.dp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryGreen else TextDark
                                )
                            }
                        }
                    }
                }

                if (selectedCountry == "বাংলাদেশ") {
                    item {
                        Text(
                            "জেলা নির্বাচন করুন",
                            modifier = Modifier.padding(16.dp),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                    items(bangladeshDistricts) { district ->
                        val isSelected = !state.isAutoLocation && state.selectedDistrict == district.name
                        Surface(
                            onClick = { 
                                viewModel.setLocationManually(district.name, district.lat, district.lng)
                                onBack()
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PrimaryGreen.copy(alpha = 0.1f) else Color.White,
                            border = if (isSelected) BorderStroke(1.dp, PrimaryGreen) else BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Text(
                                district.name,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PrimaryGreen else TextDark
                            )
                        }
                    }
                }
            }
        }
    }
}
