package com.example.face_recognition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun SplashScreen(
    onNavigate: (String) -> Unit,
    animationAsset: String = "splash.json"
) {
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        HealLensLottieAnimation(
            assetName = animationAsset,
            modifier = Modifier.size(200.dp),
            loop = false,
            // 🔥 When animation finishes, decide where to go
            onAnimationFinished = {
                if (firebaseUser != null) {
                    onNavigate(Screen.homeScreen.route)
                } else {
                    onNavigate(Screen.LoginScreen.route)
                }
            }
        )
    }
}

