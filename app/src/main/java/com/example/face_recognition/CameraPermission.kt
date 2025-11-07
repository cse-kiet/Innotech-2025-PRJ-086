package com.example.face_recognition

import android.Manifest
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.isGranted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermission(content: @Composable () -> Unit) {
    val permissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)

    if (permissionState.status.isGranted) {
        content()
    } else {
        androidx.compose.material3.Button(onClick = { permissionState.launchPermissionRequest() }) {
            androidx.compose.material3.Text("Grant Camera Permission")
        }
    }
}



