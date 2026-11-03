package com.example.platform

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Re-encodes the image at [source] as an upright JPEG, at most [PhotoSizing.MAX_SIDE] px on the long side,
 * into [target]. Blocking: call it off the main thread.
 */
fun downscalePhoto(resolver: ContentResolver, source: Uri, target: File) {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        decodeScaled(resolver, source)
    } else {
        decodeScaledLegacy(resolver, source)
    }
    try {
        target.outputStream().use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, PhotoSizing.JPEG_QUALITY, out)) { "JPEG encoding failed" }
        }
    } finally {
        bitmap.recycle()
    }
}

/** ImageDecoder applies the EXIF orientation itself and decodes straight to the target size. */
@RequiresApi(Build.VERSION_CODES.P)
private fun decodeScaled(resolver: ContentResolver, source: Uri): Bitmap =
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, source)) { decoder, info, _ ->
        val (width, height) = PhotoSizing.targetSize(info.size.width, info.size.height)
        decoder.setTargetSize(width, height)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // hardware bitmaps can't always be compressed
    }

/** Android 7–8: subsampled BitmapFactory decode, then one exact resize plus the EXIF rotation. */
private fun decodeScaledLegacy(resolver: ContentResolver, source: Uri): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(source).use { BitmapFactory.decodeStream(it, null, bounds) }
    check(bounds.outWidth > 0 && bounds.outHeight > 0) { "Not an image" }

    val options = BitmapFactory.Options().apply { inSampleSize = PhotoSizing.sampleSize(bounds.outWidth, bounds.outHeight) }
    val sampled = resolver.openInputStream(source).use { BitmapFactory.decodeStream(it, null, options) }
        ?: error("Not an image")
    val orientation = resolver.openInputStream(source)?.use {
        ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

    val (width, height) = PhotoSizing.targetSize(sampled.width, sampled.height)
    val matrix = Matrix().apply {
        postScale(width.toFloat() / sampled.width, height.toFloat() / sampled.height)
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
        }
    }
    val result = Bitmap.createBitmap(sampled, 0, 0, sampled.width, sampled.height, matrix, true)
    if (result !== sampled) sampled.recycle()
    return result
}
