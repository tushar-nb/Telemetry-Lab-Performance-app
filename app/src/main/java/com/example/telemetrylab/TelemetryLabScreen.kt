package com.example.telemetrylab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TelemetryLabScreen(
    innerPadding: PaddingValues,
    viewModel: TelemetryViewModel
) {
    val isRunning by viewModel.isRunning.collectAsState()
    val computeLoad by viewModel.computeLoad.collectAsState()
    val telemetryData by viewModel.telemetryData.collectAsState()
    val isPowerSaveMode by viewModel.isInPowerSavingMode.collectAsState()
    val scrollingItems by viewModel.scrollingItems.collectAsState()

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isPowerSaveMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(252, 184, 129).copy(alpha = 0.2f))
            ) {
                Text(
                    text = "Power Saving Mode ON",
                    modifier = Modifier.padding(8.dp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(252, 184, 129)
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Compute Load: $computeLoad")
                Slider(
                    value = computeLoad.toFloat(),
                    onValueChange = { viewModel.setComputeLoad(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors().copy(
                        inactiveTrackColor = Color.LightGray
                    )
                )
            }
        }

        // Telemetry Dashboard
        TelemetryDashboard(telemetryData)

        // Scrolling List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Live Activity Feed", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    state = rememberLazyListState(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(scrollingItems) { item ->
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.toggleService() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRunning) "Stop" else "Start")
        }
    }
}

@Composable
fun TelemetryDashboard(telemetryData: TelemetryData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Performance Metrics", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    MetricItem("Frame Latency", "${String.format("%.2f", telemetryData.frameLatency)} ms")
                    MetricItem("Moving Average", "${String.format("%.2f", telemetryData.movingAverage)} ms")
                }
                Column(modifier = Modifier.weight(1f)) {
                    MetricItem("Jank % (30s)", "${String.format("%.1f", telemetryData.jankPercentage)}%")
                    MetricItem("Jank Frames", "${telemetryData.jankFrameCount}/${telemetryData.totalFrames}")
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = FontFamily.Monospace
        )
    }
}