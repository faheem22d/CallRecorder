package com.callrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecorderService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false

    companion object {
        const val ACTION_START = "ACTION_START_RECORDING"
        const val ACTION_STOP = "ACTION_STOP_RECORDING"
        const val EXTRA_CALLER_NUMBER = "EXTRA_CALLER_NUMBER"

        private const val CHANNEL_ID = "call_recorder_channel"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "CallRecorderService"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val callerNumber = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "Unknown"
                if (!isRecording) startRecording(callerNumber)
            }
            ACTION_STOP -> {
                if (isRecording) stopRecording()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startRecording(callerNumber: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeNumber = callerNumber.replace("+", "").replace(" ", "")
            val fileName = "call_${safeNumber}_$timestamp.m4a"

            val dir = MainActivity.getRecordingsDir(this)
            outputFile = File(dir, fileName)

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                // VOICE_CALL captures both sides on some devices (requires system privilege)
                // Fallback: MIC captures only your side on most devices
                setAudioSource(
                    try {
                        MediaRecorder.AudioSource.VOICE_CALL
                    } catch (e: Exception) {
                        Log.w(TAG, "VOICE_CALL not available, using MIC")
                        MediaRecorder.AudioSource.MIC
                    }
                )
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Recording started: ${outputFile?.name}")

            startForeground(NOTIFICATION_ID, buildNotification("🔴 Recording call with $callerNumber"))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            isRecording = false
            stopSelf()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            Log.d(TAG, "Recording saved: ${outputFile?.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}")
            outputFile?.delete() // Delete incomplete file
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Recorder")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Call Recording",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shown while a call is being recorded"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) stopRecording()
    }
}
