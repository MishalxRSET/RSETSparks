package com.example.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.writeToURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.math.max
import kotlin.math.min

/**
 * Gallery via PHPickerViewController (no photo-library permission needed) and camera via
 * UIImagePickerController (needs NSCameraUsageDescription in Info.plist). Photos are scaled to at most
 * 1080 px and saved as JPEG in the temp directory; [onPicked] gets its file:// URL.
 */
@Composable
actual fun rememberPhotoPicker(onPicked: (String) -> Unit, onError: (String) -> Unit): PhotoPicker {
    val host = LocalUIViewController.current
    val picked = rememberUpdatedState(onPicked)
    val failed = rememberUpdatedState(onError)
    return remember(host) { IosPhotoPicker(host, { picked.value(it) }, { failed.value(it) }) }
}

private class IosPhotoPicker(
    private val host: UIViewController,
    private val onPicked: (String) -> Unit,
    private val onError: (String) -> Unit
) : PhotoPicker {

    override val canTakePhoto: Boolean =
        UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)

    // UIKit holds delegates weakly, so keep them alive here.
    private val cameraDelegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
        override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
            picker.dismissViewControllerAnimated(true, null)
            val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
            if (image != null) save(image) else onError("Couldn't read the photo.")
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            picker.dismissViewControllerAnimated(true, null)
        }
    }

    private val galleryDelegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
        override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
            picker.dismissViewControllerAnimated(true, null)
            val result = didFinishPicking.firstOrNull() as? PHPickerResult ?: return
            result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data: NSData?, _ ->
                dispatch_async(dispatch_get_main_queue()) {
                    val image = data?.let { UIImage(data = it) }
                    if (image != null) save(image) else onError("Couldn't read the selected photo.")
                }
            }
        }
    }

    override fun takePhoto() {
        val picker = UIImagePickerController()
        picker.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        picker.delegate = cameraDelegate
        host.presentViewController(picker, animated = true, completion = null)
    }

    override fun pickFromGallery() {
        val config = PHPickerConfiguration()
        config.filter = PHPickerFilter.imagesFilter
        config.selectionLimit = 1
        val picker = PHPickerViewController(configuration = config)
        picker.delegate = galleryDelegate
        host.presentViewController(picker, animated = true, completion = null)
    }

    private fun save(image: UIImage) {
        val (width, height) = image.size.useContents { width to height }
        val scale = min(1.0, MAX_SIDE / max(width, height))
        val resized = if (scale < 1.0) {
            UIGraphicsImageRenderer(size = CGSizeMake(width * scale, height * scale)).imageWithActions { _ ->
                image.drawInRect(CGRectMake(0.0, 0.0, width * scale, height * scale))
            }
        } else {
            image
        }
        val jpeg = UIImageJPEGRepresentation(resized, 0.8) ?: return onError("Couldn't prepare the photo.")
        val url = NSURL.fileURLWithPath("${NSTemporaryDirectory()}avatar_${NSUUID().UUIDString}.jpg")
        if (jpeg.writeToURL(url, atomically = true)) onPicked(url.absoluteString ?: return) else onError("Couldn't save the photo.")
    }

    private companion object {
        const val MAX_SIDE = 1080.0
    }
}
