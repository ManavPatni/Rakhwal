package com.mnvpatni.rakhwala

import android.content.Intent
import android.content.RestrictionsManager.RESULT_ERROR
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.mnvpatni.rakhwala.databinding.ActivitySearchDestinationBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SearchDestinationActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchDestinationBinding

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    // Define a launcher for the AutocompleteActivity
    private val autocompleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        handleAutocompleteResult(result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchDestinationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializePlacesAPI()
        binding.searchQuery.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                launchAutocomplete() // Launch autocomplete on single click
                true
            } else {
                false
            }
        }

    }

    private fun initializePlacesAPI() {
        // Initialize Places API with API Key
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_autocomplete_key))
        }
    }

    private fun launchAutocomplete() {
        // Set the fields you want to return, including ADDRESS and LAT_LNG
        val fields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )

        // Launch autocomplete widget
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
            .build(this)
        autocompleteLauncher.launch(intent)
    }

    private fun handleAutocompleteResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            if (data != null) {
                val place = Autocomplete.getPlaceFromIntent(data)

                Log.d(
                    "AddEventActivity",
                    "Place Name: ${place.name}, Place Address: ${place.address}, Lat: ${place.latLng?.latitude}, Lng: ${place.latLng?.longitude}"
                )

                place.latLng?.let {
                    binding.searchQuery.setText(place.name)
                    latitude = it.latitude
                    longitude = it.longitude
                }
                val intent = Intent(this,RouteActivity::class.java)
                    .putExtra("dest_lat",latitude)
                    .putExtra("dest_lon",longitude)
                startActivity(intent)
            } else {
                Log.e("AddEventActivity", "No data received from AutocompleteActivity")
                Toast.makeText(this, "No data received", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == RESULT_ERROR) {
            val status = Autocomplete.getStatusFromIntent(result.data!!)
            Log.e("AddEventActivity", "Error: ${status.statusMessage}")
            Toast.makeText(this, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
        } else if (result.resultCode == RESULT_CANCELED) {
            Log.d("AddEventActivity", "AutocompleteActivity canceled by user")
        } else {
            Log.d("AddEventActivity", "Unknown resultCode: ${result.resultCode}")
        }
    }

}
