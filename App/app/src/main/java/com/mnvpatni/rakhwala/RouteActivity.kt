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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.mnvpatni.rakhwala.adapter.RoutesAdapter
import com.mnvpatni.rakhwala.databinding.ActivityRouteBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class RouteActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var routesAdapter: RoutesAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var binding: ActivityRouteBinding

    private var latitude = 0.0
    private var longitude = 0.0

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var audioManager: AudioManager

    private companion object {
        private const val REQUEST_SOS_PERMISSION = 102
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.SEND_SMS] == true &&
                permissions[Manifest.permission.CALL_PHONE] == true &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            ) {
                getCurrentLocationSMS()
            } else {
                Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }

    private val phoneCallReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            if (TelephonyManager.EXTRA_STATE_OFFHOOK == state) {
                playMessageOverSpeaker()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            getCurrentLocationSMS()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        textToSpeech = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                textToSpeech.language = Locale.US
            }
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        getCurrentLocation()

        binding.btnSos.setOnClickListener {
            checkAndRequestPermissions()
        }

        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(phoneCallReceiver, filter)
    }

    private fun getCurrentLocation() {
        // Get last known location
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener(this, OnSuccessListener { location ->
            if (location != null) {
                val startLat = location.latitude
                val startLon = location.longitude
                val endLat = intent.getDoubleExtra("dest_lat",0.0)
                val endLon = intent.getDoubleExtra("dest_lon",0.0)

                // Fetch routes with current location
                fetchRoutes(startLat, startLon, endLat, endLon)
            } else {
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getCurrentLocationSMS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                latitude = it.latitude
                longitude = it.longitude
                val mapsUrl = "https://www.google.com/maps?q=$latitude,$longitude"
                sendSOSMessage(latitude, longitude, mapsUrl)
            } ?: run {
                Toast.makeText(this, "Unable to retrieve location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendSOSMessage(latitude: Double, longitude: Double, mapsUrl: String) {
        val message = "HELP ME! I am in danger. Please send help. My location is: $mapsUrl"
        fetchContactsFromFirebaseForSMS { contacts ->
            contacts.forEach { contact ->
                sendSMS(contact.phoneNumber, message)
            }
            initiateSosCall()
        }
    }

    private fun sendSMS(contactNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(contactNumber, null, message, null, null)
            Toast.makeText(this, "SMS sent successfully.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send SMS: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initiateSosCall() {
        val phoneNumber = "tel:9028303891"
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse(phoneNumber)
        }
        try {
            startActivity(callIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to make the call", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playMessageOverSpeaker() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isSpeakerphoneOn = true

        val messageToSpeak = "Hello! This is an automated SOS message."
        textToSpeech.speak(messageToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(phoneCallReceiver)
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    private fun fetchRoutes(startLat: Double, startLon: Double, endLat: Double, endLon: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getRoutes(startLat, startLon, endLat, endLon)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val routes = response.body()?.body ?: emptyList()
                        routesAdapter = RoutesAdapter(routes) { route ->
                            openGoogleMaps(route.end_point.latitude, route.end_point.longitude)
                        }
                        recyclerView.adapter = routesAdapter
                    } else {
                        Toast.makeText(this@RouteActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RouteActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openGoogleMaps(lat: Double, lon: Double) {
        val uri = Uri.parse("google.navigation:q=$lat,$lon")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://com.google.android.apps.maps"))
        startActivity(intent)
    }

    private fun fetchContactsFromFirebaseForSMS(onContactsFetched: (List<SosContact>) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/contacts")

        databaseReference.get().addOnSuccessListener { snapshot: DataSnapshot ->
            val contacts = snapshot.children.mapNotNull { it.getValue(SosContact::class.java) }
            onContactsFetched(contacts)
        }
    }
}
