package com.example.face_recognition


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.io.File


@Composable
fun ScanScreen(navController: NavController, viewModel: VitalsViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }

    // Video file setup
    val file = remember { File(context.cacheDir, "scan_video.mp4") }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val launcher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loading = true
            scope.launch {
                try {
                    // Prepare file for upload
                    val requestFile = RequestBody.create("video/mp4".toMediaTypeOrNull(), file)
                    val videoPart = MultipartBody.Part.createFormData("video", file.name, requestFile)
                    val secondsPart = RequestBody.create("text/plain".toMediaTypeOrNull(), "30")

                    // Call Flask API
                    val response = RetrofitClient.api.uploadVideo(videoPart, secondsPart)
                    if (response.isSuccessful) {
                        // Store result in ViewModel for ResultScreen
                        viewModel.setVitalsResult(response.body().toString())
                        navController.navigate(Screen.vitalsResult.route)
                    } else {
                        viewModel.setVitalsResult("Server error: ${response.code()}")
                        navController.navigate(Screen.vitalsResult.route)
                    }
                } catch (e: HttpException) {
                    viewModel.setVitalsResult("Network error: ${e.message()}")
                    navController.navigate(Screen.vitalsResult.route)
                } catch (e: Exception) {
                    viewModel.setVitalsResult("Error: ${e.localizedMessage}")
                    navController.navigate(Screen.vitalsResult.route)
                } finally {
                    loading = false
                }
            }
        }
    }

    // UI
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = {
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, uri)
                }
                launcher.launch(intent)
            }) {
                Text("Start Scan")
            }
        }
    }
}
