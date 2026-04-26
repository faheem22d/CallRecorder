package com.callrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var isOutgoing = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_NEW_OUTGOING_CALL) {
            isOutgoing = true
            return
        }

        val state = when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            else -> return
        }

        if (lastState == state) return

        when (state) {
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Call answered — start recording
                val serviceIntent = Intent(context, CallRecorderService::class.java).apply {
                    action = CallRecorderService.ACTION_START
                    putExtra(CallRecorderService.EXTRA_CALLER_NUMBER, "Call")
                }
                context.startForegroundService(serviceIntent)
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended — stop recording
                if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    val serviceIntent = Intent(context, CallRecorderService::class.java).apply {
                        action = CallRecorderService.ACTION_STOP
                    }
                    context.startForegroundService(serviceIntent)
                }
                isOutgoing = false
            }
        }

        lastState = state
    }
}
