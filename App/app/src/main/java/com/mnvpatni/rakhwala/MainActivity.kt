package com.mnvpatni.rakhwala

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp
import com.mnvpatni.rakhwala.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseApp.initializeApp(this)

        binding.cvTrackLocation.setOnClickListener { startActivity(Intent(this,TrackLocationActivity::class.java)) }
        binding.cvShareLiveLocation.setOnClickListener { startActivity(Intent(this,ShareLiveLocationActivity::class.java)) }


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

    }
}