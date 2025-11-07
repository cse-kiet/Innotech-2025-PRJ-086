package com.example.face_recognition

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.video.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

@Composable
fun HeartRateVideoScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val outputFile = remember { File(context.cacheDir, "temp_video.mp4") }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    var recording by remember { mutableStateOf<Recording?>(null) }
    var videoCapture: VideoCapture<Recorder>? = remember { null }

    LaunchedEffect(Unit) {
        val cameraProvider = cameraProviderFuture.get()
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        val preview = androidx.camera.core.Preview.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        preview.setSurfaceProvider(null) // optional if using PreviewView

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            videoCapture
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Button(onClick = {
            val vc = videoCapture ?: return@Button
            if (recording == null) {
                val outputOptions = FileOutputOptions.Builder(outputFile).build()
                val recordingInstance = vc.output
                    .prepareRecording(context, outputOptions)
                    .start(ContextCompat.getMainExecutor(context)) { event ->
                        if (event is VideoRecordEvent.Finalize) {
                            uploadVideoToFastAPI(context, outputFile)
                        }
                    }
                recording = recordingInstance
                Log.d("VideoCapture", "🎥 Recording started")
            } else {
                recording?.stop()
                recording = null
                Log.d("VideoCapture", "🛑 Recording stopped")
            }
        }) {
            Text(if (recording == null) "Start Recording" else "Stop & Analyze")
        }
    }
}

fun uploadVideoToFastAPI(context: Context, file: File) {
    val client = OkHttpClient()
    val requestBody = file.asRequestBody("video/mp4".toMediaType())
    val multipartBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("file", file.name, requestBody)
        .build()

    val request = Request.Builder()
        .url("http://192.168.41.243:8000/predict_video_heart_rate") // ⚠️ replace with your laptop IP
        .post(multipartBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("Upload", "❌ Failed: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val body = response.body?.string()
            Log.d("Upload", "✅ Response: $body")
        }
    })
}








