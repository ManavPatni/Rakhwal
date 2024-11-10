package com.mnvpatni.rakhwala

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.mnvpatni.rakhwala.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var audioManager: AudioManager

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private companion object {
        private const val REQUEST_SOS_PERMISSION = 102
    }


    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CALL_PHONE] == true &&
            permissions[Manifest.permission.READ_PHONE_STATE] == true) {
            initiateSosCall()
        } else {
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
        }
    }

    // BroadcastReceiver to listen to phone call state
    private val phoneCallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

            if (TelephonyManager.EXTRA_STATE_OFFHOOK == state) {
                // Phone is picked up (call is answered)
                playMessageOverSpeaker()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase (if used)
        FirebaseApp.initializeApp(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        binding.cvTrackLocation.setOnClickListener {
            startActivity(Intent(this, TrackLocationActivity::class.java))
        }

        binding.cvShareLiveLocation.setOnClickListener {
            startActivity(Intent(this, ShareLiveLocationActivity::class.java))
        }

        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.toString()) {
                "SOS Contact" -> {
                    startActivity(Intent(this,SOSContactActivity::class.java))
                }

                "Profile" -> {
                    //todo
                }
            }
            return@setOnItemSelectedListener true
        }

        // SOS button click listener
        binding.btnSos.setOnClickListener {
            checkLocationPermission()
        }

        // Register the BroadcastReceiver to listen for call state changes
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(phoneCallReceiver, filter)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val languageResult = textToSpeech.setLanguage(Locale.US)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language not supported or missing data", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionsAndInitiateCall() {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            initiateSosCall()
        } else {
            // Request permissions
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE))
        }
    }

    private fun initiateSosCall() {
        val phoneNumber = "tel:9028303891"
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse(phoneNumber)

        try {
            startActivity(callIntent)
            startCallMonitoring()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to make the call", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCallMonitoring() {
        // We don't need to manually monitor the call state with PhoneStateListener anymore.
    }

    private fun playMessageOverSpeaker() {
        // Set the speakerphone on
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        // Message to speak when the call is picked up
        val messageToSpeak = "Hello! This is an automated SOS message."
        textToSpeech.speak(messageToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_SOS_PERMISSION)
        } else {
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val latitude = it.latitude
                val longitude = it.longitude
                val mapsUrl = "https://www.google.com/maps?q=$latitude,$longitude"
                sendSOSMessage(latitude, longitude, mapsUrl)
            } ?: run {
                Toast.makeText(this, "Unable to retrieve location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSOSMessage(latitude: Double, longitude: Double, mapsUrl: String) {
        val message = "HELP ME! I am in danger. Please send help. My location is: $mapsUrl"

        // Fetch contacts from Firebase and send SOS message via SMS
        fetchContactsFromFirebaseForSMS { contacts ->
            contacts.forEach { contact ->
                sendSMS(contact.phoneNumber, message)
            }
        }
    }

    // Add a method to fetch contacts and invoke callback with them
    private fun fetchContactsFromFirebaseForSMS(onContactsFetched: (List<SosContact>) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/contacts")

        databaseReference.get().addOnSuccessListener { snapshot: DataSnapshot ->
            val contacts = mutableListOf<SosContact>()
            snapshot.children.forEach { dataSnapshot ->
                val contact = dataSnapshot.getValue(SosContact::class.java)
                contact?.let { contacts.add(it) }
            }
            onContactsFetched(contacts)
        }
    }

    private fun sendSMS(contactNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(contactNumber, null, message, null, null)
            Toast.makeText(this, "SMS sent successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
        checkPermissionsAndInitiateCall()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the receiver when activity is destroyed
        unregisterReceiver(phoneCallReceiver)

        // Stop Text-to-Speech
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }
}
