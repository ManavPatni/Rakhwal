package com.mnvpatni.rakhwala

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.LocationPuck2D
import com.mapbox.maps.plugin.locationcomponent.location

class TrackLocationActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var accessCodeInput: EditText
    private lateinit var trackButton: Button
    private lateinit var database: DatabaseReference
    private var accessCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_location)

        // Initialize Mapbox map
        mapView = findViewById(R.id.mapView)
        mapboxMap = mapView.getMapboxMap()
        initLocationLayer()

        // Initialize Firebase database reference
        database = FirebaseDatabase.getInstance().getReference("locations")

        // Initialize views
        accessCodeInput = findViewById(R.id.accessCodeInput)
        trackButton = findViewById(R.id.trackButton)

        // Handle track location button click
        trackButton.setOnClickListener {
            accessCode = accessCodeInput.text.toString().trim()
            if (accessCode.isNotEmpty()) {
                trackLocationFromFirebase(accessCode)
            } else {
                Toast.makeText(this, "Please enter an access code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fetch location data from Firebase
    private fun trackLocationFromFirebase(accessCode: String) {
        database.child(accessCode).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                if (snapshot.exists()) {
                    val lat = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0
                    val lng = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0
                    val location = Point.fromLngLat(lng, lat)

                    // Move the camera to the new location
                    val cameraOptions = CameraOptions.Builder()
                        .center(location)
                        .zoom(15.0)
                        .build()
                    mapboxMap.setCamera(cameraOptions)

                    // Add a marker for the location
                    // Inside the onCreate method or in the initLocationLayer function
                    mapboxMap.loadStyleUri(Style.SATELLITE_STREETS) { style ->
                        // Load the custom marker icon
                        style.addImage(
                            "location-pin-icon",
                            BitmapFactory.decodeResource(
                                resources,
                                R.drawable.ic_location_pin
                            ) // Replace with your icon name
                        )
                    }

// Updated code in `trackLocationFromFirebase`
                    mapboxMap.getStyle { style ->
                        val sourceId = "marker-source"
                        val feature = Feature.fromGeometry(location) // Wrap the Point in a Feature

                        // Check if the source already exists
                        if (style.getSource(sourceId) != null) {
                            // Update the existing source with the new location
                            (style.getSourceAs<GeoJsonSource>(sourceId))?.feature(feature)
                        } else {
                            // Add the source if it doesn't exist
                            val geoJsonSource = geoJsonSource(sourceId) {
                                feature(feature)
                            }
                            style.addSource(geoJsonSource)

                            // Add the symbol layer if it doesn’t already exist
                            if (style.getLayer("marker-layer") == null) {
                                style.addLayer(
                                    symbolLayer("marker-layer", sourceId) {
                                        iconImage("location-pin-icon") // Use the icon loaded above
                                        iconAllowOverlap(true)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Toast.makeText(
                        this@TrackLocationActivity,
                        "No location data found for this access code",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Toast.makeText(
                    this@TrackLocationActivity,
                    "Error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // Initialize Mapbox Location Component
    private fun initLocationLayer() {
        mapView.location.apply {
            // Configure the location component
            locationPuck = LocationPuck2D()
            enabled = true
        }
    }

    // Lifecycle methods for Mapbox MapView
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }


    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
