package com.example.platform

import androidx.compose.runtime.Composable

/**
 * Picks a profile photo. [onPicked] receives a local URI string (content:// on Android, file:// on iOS) of an
 * upright JPEG already downscaled to [PhotoSizing]; the backend uploads it when the profile is saved.
 */
interface PhotoPicker {
    val canTakePhoto: Boolean
    fun takePhoto()
    fun pickFromGallery()
}

@Composable
expect fun rememberPhotoPicker(onPicked: (String) -> Unit, onError: (String) -> Unit): PhotoPicker
