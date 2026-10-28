package com.example

import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** Release builds attest with Play Integrity so only the genuine Play Store app can reach Firebase. */
object AppCheckInstaller {
    fun install() {
        Firebase.appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
