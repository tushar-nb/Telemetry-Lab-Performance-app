package com.example.telemetrylab

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TelemetryService: Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val notificationId= 1
    private val channelId = "TelemetryLabChannel"

    private var computeJob: Job? = null
    private var computeLoad = 2
    private var frameRate = 20L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        computeLoad = intent?.getIntExtra("computeLoad", 2) ?: 2
        frameRate = intent?.getLongExtra("frameRate", 20L) ?: 20L

        val notification = createNotification()
        startForeground(notificationId, notification)
        startComputeWork()
        return START_STICKY
    }

    private fun startComputeWork() {
        computeJob = serviceScope.launch {
            while (isActive) {
                val startTime = System.nanoTime()

                // Perform CPU-bound convolution on background thread
                withContext(Dispatchers.Default) {
                    performConvolution(computeLoad)
                }

                val processingTime = (System.nanoTime() - startTime) / 1_000_000

                // Maintain target frame rate
                val targetFrameTime = 1000L / frameRate
                val remainingTime = targetFrameTime - processingTime

                if (remainingTime > 0) {
                    delay(remainingTime)
                }
            }
        }
    }

    private fun performConvolution(iterations: Int) {
        val array = Array(256) { FloatArray(256) { Math.random().toFloat() } }
        val kernel = arrayOf(
            floatArrayOf(-1f, -1f, -1f),
            floatArrayOf(-1f, 8f, -1f),
            floatArrayOf(-1f, -1f, -1f)
        )

        repeat(iterations) {
            convolve2D(array, kernel)
        }
    }

    private fun convolve2D(input: Array<FloatArray>, kernel: Array<FloatArray>) {
        val rows = input.size
        val cols = input[0].size
        val result = Array(rows) { FloatArray(cols) }

        for (i in 1 until rows - 1) {
            for (j in 1 until cols - 1) {
                var sum = 0f
                for (ki in 0..2) {
                    for (kj in 0..2) {
                        sum += input[i - 1 + ki][j - 1 + kj] * kernel[ki][kj]
                    }
                }
                result[i][j] = sum
            }
        }

        // Copy result back to input for next iteration
        for (i in result.indices) {
            for (j in result[i].indices) {
                input[i][j] = result[i][j]
            }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Telemetry Lab")
            .setContentText("Background compute processing active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Telemetry Lab Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background processing for telemetry data"
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        computeJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}