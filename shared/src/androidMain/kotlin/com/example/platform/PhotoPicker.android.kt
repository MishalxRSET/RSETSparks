package com.example.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Camera via the system camera app (ACTION_IMAGE_CAPTURE, so no CAMERA permission) and the Android photo
 * picker (no storage permission). Needs the FileProvider declared in the app manifest.
 */
@Composable
actual fun rememberPhotoPicker(onPicked: (String) -> Unit, onError: (String) -> Unit): PhotoPicker {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) onPicked(uri.toString())
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri.toString())
    }

    return remember(camera, gallery) {
        object : PhotoPicker {
            override val canTakePhoto = true

            override fun takePhoto() {
                val uri = runCatching {
                    val dir = File(context.cacheDir, "images").apply { mkdirs() }
                    val file = File.createTempFile("avatar_snap_", ".jpg", dir)
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }.getOrElse {
                    onError("Couldn't open the camera: ${it.message}")
                    return
                }
                pendingCameraUri = uri
                camera.launch(uri)
            }

            override fun pickFromGallery() {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }
}
