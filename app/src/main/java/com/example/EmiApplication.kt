package com.example

import android.app.Application
import com.google.firebase.FirebaseApp

class EmiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
