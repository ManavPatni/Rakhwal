package com.mnvpatni.rakhwala

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mnvpatni.rakhwala.databinding.ActivitySplashScreenBinding
import com.mnvpatni.rakhwala.sharedPref.AuthSharedPref
import java.util.Timer
import kotlin.concurrent.timerTask

class SplashScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    // Auth shared preferences
    private lateinit var authSharedPref: AuthSharedPref

    // Splash timeout in milliseconds
    private val splashTimeOut = 1500L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authSharedPref = AuthSharedPref(this)

        // Navigate based on sign-in status
        navigateBasedOnAuthStatus()

    }

    /**
     * Determines the navigation path based on the user's sign-in status.
     */
    private fun navigateBasedOnAuthStatus() {
        if (!authSharedPref.isSignedIn()) {
            // User is not signed in; navigate to SignInActivity
            exit(SignInActivity::class.java)
        } else {
            // User is signed in; navigate to MainActivity after a delay
            Timer().schedule(timerTask {
                exit(MainActivity::class.java)
            }, splashTimeOut)
        }
    }

    /**
     * Navigates to the specified activity.
     *
     * @param activityClass The target activity class to navigate to.
     */
    private fun exit(activityClass: Class<out Activity>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
        finishAffinity()
    }

}