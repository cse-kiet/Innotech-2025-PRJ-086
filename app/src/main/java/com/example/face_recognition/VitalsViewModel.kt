package com.example.face_recognition



import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class VitalsViewModel : ViewModel() {
    private val _vitals = MutableStateFlow<VitalsResponse?>(null)
    val vitals: StateFlow<VitalsResponse?> = _vitals

    private val _isMeasuring = MutableStateFlow(true)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring

    private val tempResults = mutableListOf<VitalsResponse>()
    private val _vitalsResult = mutableStateOf<String?>(null)
    val vitalsResult: StateFlow<String?> = _vitalsResult as StateFlow<String?>

    fun setVitalsResult(result: String) {
        _vitalsResult.value = result
    }

//    fun sendFrame(bitmap: Bitmap) {
//        if (!_isMeasuring.value) return  // don’t send if stopped
//
//        viewModelScope.launch {
//            try {
//                val file = File.createTempFile("frame", ".jpg")
//                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, file.outputStream())
//                val requestFile = file.asRequestBody("image/jpeg".toMediaType())
//                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
//
//                val response = RetrofitClient.instance.predictVitals(body)
//                if (response.isSuccessful) {
//                    response.body()?.let { result ->
//                        if (result.status == "success") {
//                            tempResults.add(result)
//                            if (tempResults.size >= 4) {
//                                val avg = averageVitals(tempResults)
//                                _vitals.value = avg
//                                stopMeasurement() // ✅ stop after enough readings
//                            }
//                        }
//                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }
//    }
//
//    private fun averageVitals(list: List<VitalsResponse>): VitalsResponse {
//        val hr = list.mapNotNull { it.heart_rate }.average()
//        val spo2 = list.mapNotNull { it.spo2 }.average()
//        val resp = list.mapNotNull { it.respiratory_rate }.average()
//        val stress = list.mapNotNull { it.stress_level }
//            .groupingBy { it }
//            .eachCount()
//            .maxByOrNull { it.value }?.key ?: "Normal"
//
//        return VitalsResponse(hr, spo2, resp, stress, "success")
//    }
//
//    fun stopMeasurement() {
//        _isMeasuring.value = false
//    }
}

