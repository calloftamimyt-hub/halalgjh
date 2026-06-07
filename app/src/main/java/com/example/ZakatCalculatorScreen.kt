package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.TextDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZakatCalculatorScreen(onBack: () -> Unit) {
    var goldSilverValue by remember { mutableStateOf("") }
    var cashValue by remember { mutableStateOf("") }
    var businessValue by remember { mutableStateOf("") }
    var debtsValue by remember { mutableStateOf("") }
    var zakatAmount by remember { mutableStateOf<Double?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("যাকাত ক্যালকুলেটর", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "আপনার সম্পদের বিবরণ দিন (টাকায়)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ZakatInputField("স্বর্ণ ও রৌপ্যের মূল্য", goldSilverValue) { goldSilverValue = it }
            ZakatInputField("নগদ ও ব্যাংকে গচ্ছিত অর্থ", cashValue) { cashValue = it }
            ZakatInputField("ব্যবসার সম্পদ", businessValue) { businessValue = it }
            ZakatInputField("ঋণ বা ধার (বিয়োগ হবে)", debtsValue) { debtsValue = it }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val gold = goldSilverValue.toDoubleOrNull() ?: 0.0
                    val cash = cashValue.toDoubleOrNull() ?: 0.0
                    val business = businessValue.toDoubleOrNull() ?: 0.0
                    val debts = debtsValue.toDoubleOrNull() ?: 0.0

                    val totalAssets = (gold + cash + business) - debts
                    zakatAmount = if (totalAssets > 0) totalAssets * 0.025 else 0.0
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("হিসাব করুন", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            if (zakatAmount != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "আপনার প্রদেয় যাকাত",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "৳ ${String.format("%.2f", zakatAmount)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZakatInputField(label: String, value: String, onValueChange: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                placeholder = { Text("0") },
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
