package com.example.face_recognition

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import okhttp3.internal.isSensitiveHeader




@Composable
fun HeartCameraScreen(
    navController: NavController,
    viewModel: VitalsViewModel = viewModel()
) {
    val vitals by viewModel.vitals.collectAsState()
    val isMeasuring by viewModel.isMeasuring.collectAsState()

    // 🔹 Navigate only once when vitals are available
    LaunchedEffect(vitals) {
        if (vitals != null) {
            viewModel.stopMeasurement() // stop API + camera
            delay(700) // small delay for UI smoothness
            navController.navigate(Screen.vitalsResult.route) {
                popUpTo("camera") { inclusive = true }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {

        // ✅ Live Camera Preview (only when measuring)
        CameraPreview(
            measuring = isMeasuring,
            onFrameCaptured = { frame ->
                viewModel.sendFrame(frame)
            },
            modifier = Modifier.fillMaxSize()
        )

        // ✅ Overlay UI
        if (isMeasuring) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Measuring your vitals...\nPlease stay still",
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (vitals == null) {
            Text(
                "Preparing camera...",
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }
    }
}




