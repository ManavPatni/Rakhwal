package com.mnvpatni.rakhwala

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.widget.Toast
import java.util.*

class CallListenerService : Service(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var telephonyManager: TelephonyManager

    override fun onCreate() {
        super.onCreate()

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        // Set up the PhoneStateListener to listen for call state changes
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private val phoneStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)

            when (state) {
                TelephonyManager.CALL_STATE_IDLE -> {
                    // Call ended, you can stop the service or perform other actions
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    // Call is answered or in progress, trigger the TTS
                    speakMessage("Hello, this is a test message from Manav.")
                }
            }
        }
    }

    private fun speakMessage(message: String) {
        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val langResult = textToSpeech.setLanguage(Locale.US)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language is not supported or missing data", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources when the service is destroyed
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
