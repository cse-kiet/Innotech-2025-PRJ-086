package com.example.face_recognition

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.airbnb.lottie.compose.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
@Composable
fun HealLensLottieAnimation(
    assetName: String,
    modifier: Modifier = Modifier,
    loop: Boolean = true,
    onAnimationFinished: (() -> Unit)? = null // callback when animation ends
) {
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.Asset(assetName))
    val composition by compositionResult
    val animatable = rememberLottieAnimatable()

    // Launch the animation once the composition is loaded
    LaunchedEffect(composition) {
        try {
            composition?.let {
                animatable.animate(
                    composition = it,
                    iterations = if (loop) LottieConstants.IterateForever else 1
                )
                // If not looping, trigger the callback when animation finishes
                if (!loop) {
                    onAnimationFinished?.invoke()
                }
            }
        } catch (e: Exception) {
            Log.e("HealLensLottieAnimation", "Error animating Lottie composition", e)
        }
    }

    // UI rendering
    when {
        compositionResult.isFailure -> {
            Text("Error loading animation", color = Color.Red, modifier = Modifier.padding(16.dp))
        }
        composition == null -> {
            Text("", modifier = Modifier.padding(16.dp))
        }
        else -> {
            Box(modifier = modifier) {
                LottieAnimation(
                    composition = composition,
                    progress = { animatable.progress },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}



//@Composable
//fun HealLensLottieAnimation(
//    assetName: String,
//    modifier: Modifier = Modifier,
//    loop: Boolean = true
//) {
//    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(assetName))
//    val animatable = rememberLottieAnimatable()
//
//    LaunchedEffect(composition) {
//        if (composition == null) {
//            println("⚠️ Lottie composition failed to load for asset: $assetName")
//        } else {
//            animatable.animate(
//                composition = composition,
//                iterations = if (loop) LottieConstants.IterateForever else 1
//            )
//        }
//    }
//
//
//    if (composition == null) {
//        Text(
//            "Animation not loaded",
//            modifier = Modifier.padding(16.dp),
//            color = MaterialTheme.colorScheme.onBackground
//        )
//    } else {
//        Box(modifier = modifier) {
//            LottieAnimation(
//                composition = composition,
//                progress = { animatable.progress },
//                modifier = Modifier.fillMaxSize()
//            )
//        }
//    }
//}



