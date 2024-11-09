package com.mnvpatni.rakhwala

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.mnvpatni.rakhwala.databinding.ActivityShareLiveLocationBinding
import kotlin.random.Random

class ShareLiveLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareLiveLocationBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var accessCode: String
    private lateinit var database: DatabaseReference
    private var isSharingLocation = false

    // Location request and callback
    private val locationRequest = LocationRequest.create().apply {
        interval = 10000 // Update location every 10 seconds
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            if (isSharingLocation) {
                val location = locationResult.lastLocation
                location?.let {
                    updateLocationInFirebase(it.latitude, it.longitude)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareLiveLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase database reference
        database = FirebaseDatabase.getInstance().reference

        // Retrieve or generate access code
        accessCode = retrieveOrGenerateAccessCode()
        findViewById<TextView>(R.id.accessCodeTextView).text = "Share this code: $accessCode"

        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Button to start location sharing
        findViewById<Button>(R.id.startSharingButton).setOnClickListener {
            checkLocationPermissionAndStartService()
        }

        // Button to stop location sharing
        findViewById<Button>(R.id.stopSharingButton).setOnClickListener {
            stopLocationUpdates()
        }
    }

    // Retrieve or generate a 6-digit random access code
    private fun retrieveOrGenerateAccessCode(): String {
        val sharedPrefs = getSharedPreferences("com.mnvpatni.rakhwala", Context.MODE_PRIVATE)
        val savedAccessCode = sharedPrefs.getString("ACCESS_CODE", null)
        return if (savedAccessCode != null) {
            savedAccessCode
        } else {
            val newAccessCode = Random.nextInt(100000, 999999).toString()
            sharedPrefs.edit().putString("ACCESS_CODE", newAccessCode).apply()
            newAccessCode
        }
    }

    // Check location permission and start location updates
    private fun checkLocationPermissionAndStartService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startLocationUpdates()
        }
    }

    // Start location updates
    private fun startLocationUpdates() {
        isSharingLocation = true
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
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
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        Toast.makeText(this, "Started sharing location", Toast.LENGTH_SHORT).show()
    }

    // Stop location updates
    private fun stopLocationUpdates() {
        isSharingLocation = false
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Toast.makeText(this, "Stopped sharing location", Toast.LENGTH_SHORT).show()
    }

    // Permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startLocationUpdates()
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
            }
        }

    // Update location in Firebase
    private fun updateLocationInFirebase(latitude: Double, longitude: Double) {
        val locationData = mapOf(
            "latitude" to latitude,
            "longitude" to longitude
        )
        database.child("locations").child(accessCode).setValue(locationData)
            .addOnSuccessListener {
                // Successfully updated location
            }
            .addOnFailureListener {
                // Handle any errors
                Toast.makeText(this, "Failed to update location", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }
}
