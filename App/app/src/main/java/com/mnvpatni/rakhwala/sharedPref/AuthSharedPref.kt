package com.mnvpatni.rakhwala.sharedPref

import android.content.Context
import android.content.SharedPreferences

class AuthSharedPref(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "AuthSharedPref"
        private const val KEY_IS_SIGNED_IN = "isSignedIn"
        private const val KEY_UID = "uid"
    }

    // Generic method to save a value
    private inline fun <reified T> setValue(key: String, value: T?) {
        sharedPreferences.edit().apply {
            when (value) {
                is String? -> putString(key, value)
                is Boolean -> putBoolean(key, value)
                is Int -> putInt(key, value)
                is Float -> putFloat(key, value)
                is Long -> putLong(key, value)
                else -> throw IllegalArgumentException("Unsupported type")
            }
            apply()
        }
    }

    // Generic method to retrieve a value with a default fallback
    private inline fun <reified T> getValue(key: String, defaultValue: T): T {
        return with(sharedPreferences) {
            when (T::class) {
                String::class -> getString(key, defaultValue as? String) as T
                Boolean::class -> getBoolean(key, defaultValue as Boolean) as T
                Int::class -> getInt(key, defaultValue as Int) as T
                Float::class -> getFloat(key, defaultValue as Float) as T
                Long::class -> getLong(key, defaultValue as Long) as T
                else -> throw IllegalArgumentException("Unsupported type")
            }
        }
    }

    // Sign In Status
    fun setAuthStatus(isSignedIn: Boolean) {
        setValue(KEY_IS_SIGNED_IN, isSignedIn)
    }

    fun isSignedIn(): Boolean {
        return getValue(KEY_IS_SIGNED_IN, false)
    }

    // User Details Setters
    fun setUID(uid: String?) = setValue(KEY_UID, uid)

    // User Details Getters
    fun uid(): String? = getValue(KEY_UID, null)

}