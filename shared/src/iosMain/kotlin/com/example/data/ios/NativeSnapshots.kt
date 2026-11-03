package com.example.data.ios

import cocoapods.FirebaseCore.FIRTimestamp
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.ios
import platform.Foundation.NSNull

/**
 * The document's raw field map from the Firebase iOS SDK, in the same shape the Android SDK returns, so the
 * shared FirestoreMappers can read it. Pending server timestamps come back as null.
 */
internal fun DocumentSnapshot.raw(): Map<String, Any?> =
    ios.data()?.entries?.associate { (key, value) -> key.toString() to value.takeUnless { it is NSNull } }.orEmpty()

/** Converts a Firestore Timestamp (FIRTimestamp) to epoch milliseconds. */
internal fun timestampMillis(value: Any?): Long? =
    (value as? FIRTimestamp)?.let { it.seconds * 1000 + it.nanoseconds / 1_000_000 }
