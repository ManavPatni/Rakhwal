package com.mnvpatni.rakhwala

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var database: DatabaseReference
    private var accessCode: String? = null

    companion object {
        private const val TAG = "LocationService"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize location client and Firebase reference
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        database = FirebaseDatabase.getInstance().reference

        // Define the location callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null && accessCode != null) {
                    updateLocationInFirebase(location.latitude, location.longitude)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        accessCode = intent?.getStringExtra("ACCESS_CODE")
        if (accessCode != null) {
            startForegroundService()
            startLocationUpdates()
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val channelId = "LocationServiceChannel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Sharing Active")
            .setContentText("Your live location is being shared in the background.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            Log.e(TAG, "Permission denied. Can't request location updates.")
        }
    }

    private fun updateLocationInFirebase(latitude: Double, longitude: Double) {
        val locationData = mapOf(
            "latitude" to latitude,
            "longitude" to longitude
        )

        // Log the coordinates to verify
        Log.d(TAG, "Updating Firebase with coordinates: Latitude = $latitude, Longitude = $longitude")

        // Update Firebase with new location
        database.child("locations").child(accessCode!!).setValue(locationData)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully updated location in Firebase.")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to update location in Firebase", exception)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
