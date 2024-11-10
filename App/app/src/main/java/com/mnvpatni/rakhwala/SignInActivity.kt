package com.mnvpatni.rakhwala

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.mnvpatni.rakhwala.databinding.ActivitySignInBinding
import com.mnvpatni.rakhwala.sharedPref.AuthSharedPref

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var authSharedPref: AuthSharedPref
    private lateinit var pd: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize shared preferences and Firebase Auth
        authSharedPref = AuthSharedPref(this)
        auth = FirebaseAuth.getInstance()

        pd = ProgressDialog(this)
        pd.setMessage("Please wait..")
        pd.setCancelable(false)

        // Google sign-in initialization
        initGoogleAuth()

        binding.btnGoogle.setOnClickListener { signInGoogle() }

    }


    // Initialize Google Sign-In options
    private fun initGoogleAuth() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }


    // Initiate Google Sign-In
    private fun signInGoogle() {
        pd.show()
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    handleResults(task)
                }
                Activity.RESULT_CANCELED -> {
                    pd.dismiss()
                    showMessage("Google Sign-In was canceled. Please try again.")
                    Log.e("Error", "Sign-in canceled by user or failed.")
                }
                else -> {
                    pd.dismiss()
                    showMessage("Unexpected error during Google Sign-In.")
                    Log.e("Error", "Unexpected result code: ${result.resultCode}")
                }
            }
        }


    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                updateUI(account)
            } else {
                pd.dismiss()
                showMessage("Google Sign-In failed. Account not found.")
            }
        } else {
            pd.dismiss()
            showMessage("Google Sign-In failed: ${task.exception?.message}")
            Log.e("Error", "Error: ${task.exception?.message}")
        }
    }


    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                auth.currentUser?.let { user ->
                    authSharedPref.setAuthStatus(true)
                    authSharedPref.setUID(user.uid)
                    startActivity(Intent(this,MainActivity::class.java))
                    finish()
                }
            } else {
                pd.dismiss()
                showMessage("Firebase Authentication failed: ${it.exception?.message}")
            }
        }
    }


    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

}