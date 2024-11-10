package com.mnvpatni.rakhwala

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.mnvpatni.rakhwala.adaper.ContactAdapter
import com.mnvpatni.rakhwala.databinding.ActivitySoscontactBinding
import kotlinx.coroutines.launch

class SOSContactActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySoscontactBinding
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private companion object {
        private const val REQUEST_CONTACT_PERMISSION = 100
        private const val REQUEST_SELECT_CONTACT = 101
        private const val REQUEST_SOS_PERMISSION = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySoscontactBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        contactAdapter = ContactAdapter(emptyList()) { contact ->
            lifecycleScope.launch {
                deleteContactFromFirebase(contact)
            }
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SOSContactActivity)
            adapter = contactAdapter
        }

        // Observe Firebase data changes (Real-time data)
        fetchContactsFromFirebase()

        binding.fabAddContact.setOnClickListener {
            checkContactPermission()
        }

        binding.btnSos.setOnClickListener {
            checkLocationPermission()
        }
    }

    private fun checkContactPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_CONTACT_PERMISSION)
        } else {
            openContactPicker()
        }
    }

    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        startActivityForResult(intent, REQUEST_SELECT_CONTACT)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_CONTACT && resultCode == RESULT_OK) {
            data?.data?.let { contactUri ->
                val contactName = getContactName(contactUri)
                val contactPhone = getContactPhone(contactUri)

                if (contactName != null && contactPhone != null) {
                    val contact = SosContact(name = contactName, phoneNumber = contactPhone)
                    lifecycleScope.launch {
                        addContactToFirebase(contact)
                    }
                }
            }
        }
    }

    private fun getContactName(contactUri: Uri): String? {
        var contactName: String? = null
        val cursor: Cursor? = contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (nameIndex != -1) {
                    contactName = it.getString(nameIndex)
                }
            }
        }
        return contactName
    }

    private fun getContactPhone(contactUri: Uri): String? {
        var contactPhone: String? = null
        val idCursor: Cursor? = contentResolver.query(contactUri, null, null, null, null)
        idCursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                if (idIndex != -1 && hasPhoneIndex != -1) {
                    val contactId = it.getString(idIndex)
                    val hasPhone = it.getInt(hasPhoneIndex)
                    if (hasPhone > 0) {
                        val phonesCursor = contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )
                        phonesCursor?.use { phones ->
                            if (phones.moveToFirst()) {
                                val phoneIndex = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (phoneIndex != -1) {
                                    contactPhone = phones.getString(phoneIndex)
                                }
                            }
                        }
                    }
                }
            }
        }
        return contactPhone
    }

    private fun addContactToFirebase(contact: SosContact) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/contacts")

        val contactId = databaseReference.push().key ?: return
        databaseReference.child(contactId).setValue(contact)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    fetchContactsFromFirebase()  // Refresh contacts list
                }
            }
    }

    private fun deleteContactFromFirebase(contact: SosContact) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/contacts")

        databaseReference.orderByChild("name").equalTo(contact.name).get()
            .addOnSuccessListener { snapshot ->
                snapshot.children.forEach { dataSnapshot ->
                    dataSnapshot.ref.removeValue()
                }
                fetchContactsFromFirebase()  // Refresh contacts list
            }
    }

    private fun fetchContactsFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseReference = FirebaseDatabase.getInstance().getReference("users/$userId/contacts")

        databaseReference.get().addOnSuccessListener { snapshot: DataSnapshot ->
            val contacts = mutableListOf<SosContact>()
            snapshot.children.forEach { dataSnapshot ->
                val contact = dataSnapshot.getValue(SosContact::class.java)
                contact?.let { contacts.add(it) }
            }
            contactAdapter.updateContacts(contacts)
        }
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
    }

}
