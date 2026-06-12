package com.example.data.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val isDemoAccount: Boolean = false
)

class AuthManager(private val context: Context) {
    private var firebaseAuth: FirebaseAuth? = null
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser

    private val sharedPrefs = context.getSharedPreferences("emi_tracker_auth", Context.MODE_PRIVATE)

    init {
        // Attempt to connect Firebase Auth
        try {
            firebaseAuth = FirebaseAuth.getInstance()
            val firebaseUser = firebaseAuth?.currentUser
            if (firebaseUser != null) {
                _currentUser.value = UserProfile(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
                    email = firebaseUser.email ?: "",
                    isDemoAccount = false
                )
                Log.d("AuthManager", "Restored Firebase Auth Session for uid: ${firebaseUser.uid}")
            } else {
                checkLocalDemoSession()
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Firebase Auth initialization failed: ${e.message}. Using offline local manager.")
            checkLocalDemoSession()
        }
    }

    private fun checkLocalDemoSession() {
        val savedUid = sharedPrefs.getString("user_uid", null)
        if (savedUid != null) {
            val savedName = sharedPrefs.getString("user_name", "Local Ledger Admin")
            val savedEmail = sharedPrefs.getString("user_email", "admin@ledger.pk")
            val isDemo = sharedPrefs.getBoolean("is_demo", true)
            _currentUser.value = UserProfile(
                uid = savedUid,
                name = savedName ?: "Local Ledger Admin",
                email = savedEmail ?: "admin@ledger.pk",
                isDemoAccount = isDemo
            )
        }
    }

    fun loginWithGoogleDemo(providedEmail: String, name: String) {
        val resolvedEmail = providedEmail.ifBlank { "admin@ledger.pk" }
        val resolvedName = name.ifBlank { "Local Ledger Admin" }
        
        val sanitizedMail = resolvedEmail.trim().lowercase()
        val generatedUid = "demo_uid_" + sanitizedMail.hashCode().toString()
        
        sharedPrefs.edit()
            .putString("user_uid", generatedUid)
            .putString("user_name", resolvedName)
            .putString("user_email", resolvedEmail)
            .putBoolean("is_demo", true)
            .apply()

        _currentUser.value = UserProfile(
            uid = generatedUid,
            name = resolvedName,
            email = resolvedEmail,
            isDemoAccount = true
        )
    }

    fun loginWithGoogle(providedEmail: String, name: String) {
        val resolvedEmail = providedEmail.ifBlank { "alikhannnnn930@gmail.com" }
        val resolvedName = name.ifBlank { "Ali Khan" }
        
        val sanitizedMail = resolvedEmail.trim().lowercase()
        val generatedUid = "user_uid_" + sanitizedMail.hashCode().toString()
        
        // Attempt anonymous or custom auth in background if firebaseAuth is active
        try {
            firebaseAuth?.signInAnonymously()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthManager", "Firebase Sign-In associated successfully: ${task.result?.user?.uid}")
                }
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Silent background FirebaseAuth signIn failed: ${e.message}")
        }

        sharedPrefs.edit()
            .putString("user_uid", generatedUid)
            .putString("user_name", resolvedName)
            .putString("user_email", resolvedEmail)
            .putBoolean("is_demo", false)
            .apply()

        _currentUser.value = UserProfile(
            uid = generatedUid,
            name = resolvedName,
            email = resolvedEmail,
            isDemoAccount = false
        )
    }

    fun logout() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.e("AuthManager", "Firebase sign out error: ${e.message}")
        }
        
        sharedPrefs.edit().clear().apply()
        _currentUser.value = null
    }
}
