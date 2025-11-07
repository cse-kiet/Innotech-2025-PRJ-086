package com.example.face_recognition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun VitalResultScreen(viewModel: VitalsViewModel ) {
    val vitals by viewModel.vitals.collectAsState()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (vitals == null) {
            Text("No data available", color = Color.Gray)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Your Vitals", style = MaterialTheme.typography.headlineSmall)

                Spacer(Modifier.height(20.dp))

                Text("Heart Rate: ${vitals!!.heart_rate} bpm", color = Color.Red)
                Text("SpO₂: ${vitals!!.spo2} %", color = Color.Blue)
                Text("Stress: ${vitals!!.stress_level}", color = Color.Magenta)
            }
        }
    }
}

