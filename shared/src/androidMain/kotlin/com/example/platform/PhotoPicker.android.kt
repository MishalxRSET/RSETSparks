package com.example.platform

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Camera via the system camera app (ACTION_IMAGE_CAPTURE, so no CAMERA permission) and the Android photo
 * picker (no storage permission). Either way the photo is downscaled (see [PhotoSizing]) into the app cache
 * before [onPicked] gets its content:// URI, so uploads stay small and under the storage.rules limit.
 * Needs the FileProvider declared in the app manifest (cache-path "images/").
 */
@Composable
actual fun rememberPhotoPicker(onPicked: (String) -> Unit, onError: (String) -> Unit): PhotoPicker {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val failed by rememberUpdatedState(onError)
    // A path rather than a Uri in plain state, so it survives the activity being recreated while the camera is open.
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }

    fun prepare(source: Uri, original: File? = null) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { downscaleToCache(context, source) }.also { original?.delete() }
            }
            result.fold(
                onSuccess = { picked(it.toString()) },
                onFailure = { failed("Couldn't use that photo. Try another one.") }
            )
        }
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingCameraPath?.let(::File) ?: return@rememberLauncherForActivityResult
        pendingCameraPath = null
        if (success && file.length() > 0) prepare(context.fileProviderUri(file), original = file) else file.delete()
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) prepare(uri)
    }

    return remember(camera, gallery) {
        object : PhotoPicker {
            override val canTakePhoto = true

            override fun takePhoto() {
                val (file, uri) = runCatching {
                    val file = File.createTempFile(CAMERA_PREFIX, ".jpg", imagesDir(context))
                    file to context.fileProviderUri(file)
                }.getOrElse {
                    failed("Couldn't open the camera: ${it.message}")
                    return
                }
                pendingCameraPath = file.absolutePath
                camera.launch(uri)
            }

            override fun pickFromGallery() {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }
    }
}

private const val CAMERA_PREFIX = "avatar_snap_"
private const val OUTPUT_PREFIX = "profile_"

private fun imagesDir(context: Context) = File(context.cacheDir, "images").apply { mkdirs() }

private fun Context.fileProviderUri(file: File): Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

/** Writes the downscaled copy next to the camera files and returns its FileProvider URI. */
private fun downscaleToCache(context: Context, source: Uri): Uri {
    val dir = imagesDir(context)
    // Earlier picks are superseded by this one (only the latest is ever uploaded).
    dir.listFiles { file -> file.name.startsWith(OUTPUT_PREFIX) }?.forEach { it.delete() }
    val target = File(dir, "$OUTPUT_PREFIX${UUID.randomUUID()}.jpg")
    try {
        downscalePhoto(context.contentResolver, source, target)
    } catch (e: Throwable) {
        target.delete()
        throw e
    }
    return context.fileProviderUri(target)
}
