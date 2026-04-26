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
        // Start foreground immediately to avoid crash
        startForeground(NOTIFICATION_ID, buildNotification("📞 Call Recorder is active"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val number = intent.getStringExtra(EXTRA_CALLER_NUMBER) ?: "Unknown"
                if (!isRecording) startRecording(number)
            }
            ACTION_STOP -> {
                if (isRecording) stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startRecording(callerNumber: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "call_${timestamp}.m4a"
            val dir = MainActivity.getRecordingsDir(this)
            outputFile = File(dir, fileName)

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                // MIC works on all devices including Xiaomi
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Recording started: $fileName")
            startForeground(NOTIFICATION_ID, buildNotification("🔴 Recording call..."))

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            isRecording = false
            outputFile?.delete()
            stopSelf()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            Log.d(TAG, "Recording saved: ${outputFile?.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping: ${e.message}")
            outputFile?.delete()
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Call Recorder")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(intent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Call Recording", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) stopRecording()
    }
}
