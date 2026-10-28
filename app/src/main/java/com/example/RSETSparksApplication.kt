package com.example

import android.app.Application
import com.example.notifications.Notifications
import com.google.firebase.FirebaseApp

class RSETSparksApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        // FirebaseApp is null when app/google-services.json is missing; the UI then shows a setup error.
        if (FirebaseApp.initializeApp(this) != null) {
            AppCheckInstaller.install()
        }
    }
}
