package com.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneStateReceiver"
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var isOutgoing = false
        private var savedNumber: String? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Capture outgoing call number
        if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            savedNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            isOutgoing = true
            Log.d(TAG, "Outgoing call to: $savedNumber")
            return
        }

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val state = when (stateStr) {
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            else -> return
        }

        onCallStateChanged(context, state, incomingNumber)
    }

    private fun onCallStateChanged(context: Context, state: Int, number: String?) {
        if (lastState == state) return

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isOutgoing = false
                savedNumber = number
                Log.d(TAG, "Incoming call from: $savedNumber")
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Call answered — start recording
                if (lastState != TelephonyManager.CALL_STATE_IDLE) {
                    // Ongoing call (call waiting etc.)
                }
                Log.d(TAG, "Call answered — starting recording")
                startRecording(context, savedNumber ?: "Unknown")
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended — stop recording
                if (lastState == TelephonyManager.CALL_STATE_OFFHOOK ||
                    lastState == TelephonyManager.CALL_STATE_RINGING
                ) {
                    Log.d(TAG, "Call ended — stopping recording")
                    stopRecording(context)
                }
                isOutgoing = false
                savedNumber = null
            }
        }

        lastState = state
    }

    private fun startRecording(context: Context, callerNumber: String) {
        val intent = Intent(context, CallRecorderService::class.java).apply {
            action = CallRecorderService.ACTION_START
            putExtra(CallRecorderService.EXTRA_CALLER_NUMBER, callerNumber)
        }
        context.startForegroundService(intent)
    }

    private fun stopRecording(context: Context) {
        val intent = Intent(context, CallRecorderService::class.java).apply {
            action = CallRecorderService.ACTION_STOP
        }
        context.startForegroundService(intent)
    }
}
