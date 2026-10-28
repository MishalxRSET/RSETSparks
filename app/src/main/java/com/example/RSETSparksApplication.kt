package com.example

import android.app.Application
import com.google.firebase.FirebaseApp

class RSETSparksApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // FirebaseApp is null when app/google-services.json is missing; the UI then shows a setup error.
        if (FirebaseApp.initializeApp(this) != null) {
            AppCheckInstaller.install()
        }
    }
}
