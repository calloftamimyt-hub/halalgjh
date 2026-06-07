package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaCompassScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }
    
    // Qibla Direction for Dhaka is ~292.5 degrees (North-West)
    // Formula changes based on location, assuming Bangladesh average here.
    val qiblaBearing = 292.5f 

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    
                    var degree = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    degree = (degree + 360) % 360
                    
                    // Add slight smoothing
                    azimuth = degree
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("কিবলা কম্পাস", fontWeight = FontWeight.Bold) },
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
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                "কিবলার দিক",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "মক্কার দিক নির্দেশ করছে 🕋",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(60.dp))
            
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Compass Dial rotating based on device azimuth
                CompassDial(azimuth = azimuth, qiblaBearing = qiblaBearing)
            }
            
            Spacer(modifier = Modifier.height(60.dp))
            
            Card(
                modifier = Modifier.padding(horizontal = 32.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "আপনার ফোনটি সমতল স্থানে রাখুন এবং দিক নির্দেশক অনুসরণ করুন।",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun CompassDial(azimuth: Float, qiblaBearing: Float) {
    val compassRotation = -azimuth
    val animatedRotation by animateFloatAsState(targetValue = compassRotation, animationSpec = tween(100), label = "")
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val qiblaColor = Color(0xFF00C853) // Green for Qibla
    
    Canvas(modifier = Modifier.fillMaxSize().rotate(animatedRotation)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2
        
        // Outer ticks
        for (i in 0 until 360 step 15) {
            val angleRad = Math.toRadians(i.toDouble() - 90.0)
            val lineLength = if (i % 90 == 0) 20.dp.toPx() else 10.dp.toPx()
            val strokeW = if (i % 90 == 0) 3.dp.toPx() else 1.dp.toPx()
            
            val startX = center.x + (radius - lineLength) * cos(angleRad).toFloat()
            val startY = center.y + (radius - lineLength) * sin(angleRad).toFloat()
            val endX = center.x + radius * cos(angleRad).toFloat()
            val endY = center.y + radius * sin(angleRad).toFloat()
            
            drawLine(
                color = onSurfaceColor.copy(alpha = 0.5f),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeW
            )
        }
        
        // North Indicator (N)
        val nPath = Path().apply {
            moveTo(center.x - 10.dp.toPx(), center.y - radius + 30.dp.toPx())
            lineTo(center.x, center.y - radius + 10.dp.toPx())
            lineTo(center.x + 10.dp.toPx(), center.y - radius + 30.dp.toPx())
            close()
        }
        drawPath(nPath, color = Color.Red)
        
        // Qibla Indicator
        val qiblaRad = Math.toRadians(qiblaBearing.toDouble() - 90.0)
        val qStartX = center.x + (radius - 50.dp.toPx()) * cos(qiblaRad).toFloat()
        val qStartY = center.y + (radius - 50.dp.toPx()) * sin(qiblaRad).toFloat()
        val qEndX = center.x + radius * cos(qiblaRad).toFloat()
        val qEndY = center.y + radius * sin(qiblaRad).toFloat()
        
        drawLine(
            color = qiblaColor,
            start = Offset(qStartX, qStartY),
            end = Offset(qEndX, qEndY),
            strokeWidth = 6.dp.toPx(),
            cap = Stroke.DefaultCap
        )
        
        // Qibla Dot at the end
        drawCircle(
            color = qiblaColor,
            radius = 6.dp.toPx(),
            center = Offset(qEndX, qEndY)
        )
    }
    
    // Fixed phone indicator inside
    Canvas(modifier = Modifier.size(60.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val path = Path().apply {
            moveTo(center.x, 0f)
            lineTo(size.width, size.height)
            lineTo(center.x, size.height - 15.dp.toPx())
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color = primaryColor)
    }
}
