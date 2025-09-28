package com.example.telemetrylab

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TelemetryViewModel(application: Application): AndroidViewModel(application) {
    private val context = application.applicationContext
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val _isRunning by lazy { MutableStateFlow(false) }
    val isRunning = _isRunning.asStateFlow()

    private val _computedLoad by lazy { MutableStateFlow(2) }
    val computeLoad = _computedLoad.asStateFlow()

    private val _telemetryData by lazy { MutableStateFlow(TelemetryData(0f, 0f, 0f, 0, 0)) }
    val telemetryData = _telemetryData.asStateFlow()

    private val _isInPowerSavingMode by lazy { MutableStateFlow(false) }
    val isInPowerSavingMode = _isInPowerSavingMode.asStateFlow()

    private val _scrollingItems by lazy { MutableStateFlow(listOf<String>()) }
    val scrollingItems = _scrollingItems.asStateFlow()

    private val frameLatencies = mutableListOf<Float>()
    private var jankFrames = 0
    private var totalFrames = 0
    private val maxLatencyHistory = 600

    private var telemetryJob: Job? = null
    private var uiUpdateJob: Job? = null

    init {
        checkPowerSaveMode()
    }

    private fun checkPowerSaveMode() {
        _isInPowerSavingMode.value = powerManager.isPowerSaveMode
    }

    private fun getEffectiveFrameRate(): Long = if (_isInPowerSavingMode.value) 10L else 20L

    private fun getEffectiveComputeLoad(): Int = if (_isInPowerSavingMode.value) {
        (_computedLoad.value - 1).coerceAtLeast(1)
    } else {
        _computedLoad.value
    }

    fun setComputeLoad(load: Int) {
        _computedLoad.value = load.coerceIn(1, 5)
    }

    fun toggleService() {
        if (_isRunning.value) {
            stopService()
        } else {
            startService()
        }
    }

    private fun startService() {
        val intent = Intent(context, TelemetryService::class.java).apply {
            putExtra("computeLoad", getEffectiveComputeLoad())
            putExtra("frameRate", getEffectiveFrameRate())
        }
        context.startForegroundService(intent)
        _isRunning.value = true

        startTelemetryCollection()
        startUIUpdates()
    }

    private fun stopService() {
        val intent = Intent(context, TelemetryService::class.java)
        context.stopService(intent)
        _isRunning.value = false

        telemetryJob?.cancel()
        uiUpdateJob?.cancel()
    }

    private fun startTelemetryCollection() {
        telemetryJob = viewModelScope.launch {
            while (isActive) {
                val startTime = System.nanoTime()

                // Simulate frame processing time measurement
                delay(1000 / getEffectiveFrameRate()) // Wait for next frame

                val frameTime = (System.nanoTime() - startTime) / 1_000_000f // Convert to ms

                updateTelemetryData(frameTime)
            }
        }
    }

    private fun startUIUpdates() {
        uiUpdateJob = viewModelScope.launch {
            var counter = 0
            while (isActive) {
                // Add scrolling items to show UI activity
                val newItems = _scrollingItems.value.toMutableList()
                newItems.add(0, "Frame ${++counter} - ${System.currentTimeMillis()}")

                // Keep only last 50 items
                if (newItems.size > 50) {
                    newItems.removeAt(newItems.lastIndex)
                }

                _scrollingItems.value = newItems
                delay(50) // Update UI items at 20Hz
            }
        }
    }

    private fun updateTelemetryData(frameLatency: Float) {
        frameLatencies.add(frameLatency)
        totalFrames++

        // Check for jank (>16.67ms for 60fps)
        if (frameLatency > 16.67f) {
            jankFrames++
        }

        // Keep only last 30 seconds of data
        if (frameLatencies.size > maxLatencyHistory) {
            val removedLatency = frameLatencies.removeAt(0)
            if (removedLatency > 16.67f) {
                jankFrames--
            }
            totalFrames--
        }

        val movingAverage = frameLatencies.average().toFloat()
        val jankPercentage = if (totalFrames > 0) (jankFrames.toFloat() / totalFrames) * 100f else 0f

        _telemetryData.value = TelemetryData(
            frameLatency = frameLatency,
            movingAverage = movingAverage,
            jankPercentage = jankPercentage,
            jankFrameCount = jankFrames,
            totalFrames = totalFrames
        )
    }

    override fun onCleared() {
        super.onCleared()
        telemetryJob?.cancel()
        uiUpdateJob?.cancel()
        if (_isRunning.value) {
            stopService()
        }
    }

}

data class TelemetryData(
    val frameLatency: Float,
    val movingAverage: Float,
    val jankPercentage: Float,
    val jankFrameCount: Int,
    val totalFrames: Int,
)