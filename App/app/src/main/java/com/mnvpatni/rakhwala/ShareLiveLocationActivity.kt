package com.mnvpatni.rakhwala

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mnvpatni.rakhwala.databinding.ActivityShareLiveLocationBinding
import kotlin.random.Random

class ShareLiveLocationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShareLiveLocationBinding
    private lateinit var accessCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShareLiveLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve or generate the access code
        accessCode = retrieveOrGenerateAccessCode()
        findViewById<TextView>(R.id.accessCodeTextView).text = "Share this code: $accessCode"

        // Start sharing location
        findViewById<Button>(R.id.startSharingButton).setOnClickListener {
            checkLocationPermissions()
        }

        // Stop sharing location
        findViewById<Button>(R.id.stopSharingButton).setOnClickListener {
            stopLocationService()
        }
    }

    private fun retrieveOrGenerateAccessCode(): String {
        val sharedPrefs = getSharedPreferences("com.mnvpatni.rakhwala", Context.MODE_PRIVATE)
        return sharedPrefs.getString("ACCESS_CODE", null)
            ?: Random.nextInt(100000, 999999).toString().also {
                sharedPrefs.edit().putString("ACCESS_CODE", it).apply()
            }
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            startLocationService()
        }
    }

    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        intent.putExtra("ACCESS_CODE", accessCode)
        startService(intent)
        Toast.makeText(this, "Started location sharing", Toast.LENGTH_SHORT).show()
    }

    private fun stopLocationService() {
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
        Toast.makeText(this, "Stopped location sharing", Toast.LENGTH_SHORT).show()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startLocationService()
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_LONG).show()
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationService()
    }
}
