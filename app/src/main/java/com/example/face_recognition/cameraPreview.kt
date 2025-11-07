package com.example.face_recognition

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.core.BaseOptions


import android.util.Size
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors



@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onFaceProcessed: (FloatArray?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    var faceLandmarker by remember { mutableStateOf<FaceLandmarker?>(null) }

    // Initialize FaceMesh
    LaunchedEffect(Unit) {
        try {
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptionsFromAsset("face_landmarker.task")
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)
                .build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            Log.d("FaceMesh", "✅ FaceMesh initialized")
        } catch (e: Exception) {
            Log.e("FaceMesh", "❌ Failed to initialize: ${e.message}")
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysisUseCase = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysisUseCase.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                    coroutineScope.launch {
                        try {
                            val mpImage = MPImage.create(imageProxy)
                            val result = faceLandmarker?.detect(mpImage)
                            withContext(Dispatchers.Main) {
                                processFaceResults(result, onFaceProcessed)
                            }
                        } catch (e: Exception) {
                            Log.e("Analyzer", "Error: ${e.message}")
                        } finally {
                            imageProxy.close()
                        }
                    }
                }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analysisUseCase
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Binding failed: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

private fun processFaceResults(
    result: FaceLandmarkerResult?,
    onFaceProcessed: (FloatArray?) -> Unit
) {
    if (result == null || result.faceLandmarks().isEmpty()) {
        onFaceProcessed(null)
        return
    }
    val landmarks = result.faceLandmarks()[0]
    val forehead = landmarks[10]
    val cheeks = landmarks[234]
    val avg = floatArrayOf(forehead.x, forehead.y, cheeks.x, cheeks.y)
    onFaceProcessed(avg)
}
//fun CameraPreview(
//    measuring: Boolean,
//    onFrameCaptured: (Bitmap) -> Unit,
//    modifier: Modifier = Modifier
//) {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
//    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
//
//    // Initialize camera
//    AndroidView(
//        factory = { ctx ->
//            val previewView = PreviewView(ctx).apply {
//                layoutParams = ViewGroup.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT
//                )
//                scaleType = PreviewView.ScaleType.FILL_CENTER
//            }
//
//            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
//            cameraProviderFuture.addListener({
//                cameraProvider = cameraProviderFuture.get()
//
//                val preview = Preview.Builder().build().apply {
//                    setSurfaceProvider(previewView.surfaceProvider)
//                }
//
//                val imageAnalyzer = ImageAnalysis.Builder()
//                    .setTargetResolution(Size(640, 480))
//                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                    .build()
//                    .also { analysis ->
//                        var lastCaptureTime = 0L
//                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
//                            val currentTime = System.currentTimeMillis()
//                            // Throttle frames (capture 1 every 1.5 sec)
//                            if (measuring && currentTime - lastCaptureTime > 1500) {
//                                imageProxy.toBitmap()?.let { bitmap ->
//                                    onFrameCaptured(bitmap)
//                                }
//                                lastCaptureTime = currentTime
//                            }
//                            imageProxy.close()
//                        }
//                    }
//
//                try {
//                    cameraProvider?.unbindAll()
//                    cameraProvider?.bindToLifecycle(
//                        lifecycleOwner,
//                        CameraSelector.DEFAULT_FRONT_CAMERA,
//                        preview,
//                        imageAnalyzer
//                    )
//                    Log.d("CameraPreview", "Camera started.")
//                } catch (exc: Exception) {
//                    Log.e("CameraPreview", "Binding failed: ${exc.message}")
//                }
//            }, ContextCompat.getMainExecutor(ctx))
//
//            previewView
//        },
//        modifier = modifier
//    )
//
//    // Stop camera when measuring = false
//    LaunchedEffect(measuring) {
//        if (!measuring) {
//            delay(300) // small delay to allow frame processing
//            cameraProvider?.unbindAll()
//            Log.d("CameraPreview", "Camera stopped (measurement complete).")
//        }
//    }
//
//    // Clean up resources when composable is removed
//    DisposableEffect(Unit) {
//        onDispose {
//            cameraExecutor.shutdown()
//            cameraProvider?.unbindAll()
//            Log.d("CameraPreview", "Camera resources released.")
//        }
//    }
//}
///**
// * Converts an ImageProxy frame to Bitmap
// */
//fun ImageProxy.toBitmap(): Bitmap? {
//    val nv21Buffer = yuv420ToNv21(this)
//    val yuvImage = android.graphics.YuvImage(
//        nv21Buffer,
//        android.graphics.ImageFormat.NV21,
//        width, height,
//        null
//    )
//
//    val out = java.io.ByteArrayOutputStream()
//    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, width, height), 85, out)
//    val imageBytes = out.toByteArray()
//    return android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
//}
//
//fun yuv420ToNv21(image: ImageProxy): ByteArray {
//    val yBuffer = image.planes[0].buffer
//    val uBuffer = image.planes[1].buffer
//    val vBuffer = image.planes[2].buffer
//
//    val ySize = yBuffer.remaining()
//    val uSize = uBuffer.remaining()
//    val vSize = vBuffer.remaining()
//
//    val nv21 = ByteArray(ySize + uSize + vSize)
//
//    yBuffer.get(nv21, 0, ySize)
//    val chromaRowStride = image.planes[1].rowStride
//    val chromaRowPadding = chromaRowStride - image.width / 2
//
//    var offset = ySize
//    for (row in 0 until image.height / 2) {
//        for (col in 0 until image.width / 2) {
//            nv21[offset++] = vBuffer.get()
//            nv21[offset++] = uBuffer.get()
//        }
//        vBuffer.position(vBuffer.position() + chromaRowPadding)
//        uBuffer.position(uBuffer.position() + chromaRowPadding)
//    }
//    return nv21
//}

