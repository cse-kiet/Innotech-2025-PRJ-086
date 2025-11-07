package com.example.face_recognition

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

@Composable
fun VideoCaptureScreen(onVideoCaptured: (Uri) -> Unit) {
    val context = LocalContext.current
    val videoUri = remember { mutableStateOf<Uri?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { uri ->
        uri?.let {
            videoUri.value = it
            onVideoCaptured(it)
        }
    }

    Button(onClick = { launcher.launch(null) }) {
        Text("Start Scan")
    }
}

suspend fun uploadVideoToServer(context: Context, videoUri: Uri): String {
    val contentResolver = context.contentResolver
    val inputStream = contentResolver.openInputStream(videoUri)
    val file = File(context.cacheDir, "upload.mp4")
    val outputStream = FileOutputStream(file)
    inputStream?.copyTo(outputStream)
    inputStream?.close()
    outputStream.close()

    val requestFile = file.asRequestBody("video/mp4".toMediaTypeOrNull())
    val videoPart = MultipartBody.Part.createFormData("video", file.name, requestFile)
    val seconds = "30".toRequestBody("text/plain".toMediaTypeOrNull())

    val response = RetrofitClient.api.uploadVideo(videoPart, seconds)
    return if (response.isSuccessful) {
        response.body().toString()
    } else {
        "Error: ${response.code()}"
    }
}

