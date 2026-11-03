package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.example.platform.downscalePhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.random.Random

/**
 * Runs the real decode → resize → rotate → JPEG pipeline (Robolectric native graphics) through the app's
 * FileProvider, like a camera or gallery pick. SDK 34 takes the ImageDecoder path, SDK 27 the BitmapFactory one.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [27, 34])
class PhotoDownscaleTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val images = File(context.cacheDir, "images").apply { mkdirs() }

  /**
   * FileProvider caches each authority's root directory in a static map, but Robolectric gives every test a
   * new data directory. Clear it so each test resolves its own cache dir (a real app process has just one).
   */
  @Before
  fun clearFileProviderCache() {
    val cache = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
    synchronized(cache.get(null)!!) { (cache.get(null) as MutableMap<*, *>).clear() }
  }

  /** Left half red, right half blue, so a rotation is visible in the output. */
  private fun photo(width: Int, height: Int, orientation: Int? = null, noise: Boolean = false): File {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    if (noise) {
      val random = Random(42)
      val row = IntArray(width)
      for (y in 0 until height) {
        for (x in 0 until width) row[x] = random.nextInt() or 0xFF000000.toInt()
        bitmap.setPixels(row, 0, width, 0, y, width, 1)
      }
    } else {
      Canvas(bitmap).apply {
        drawColor(Color.BLUE)
        drawRect(0f, 0f, width / 2f, height.toFloat(), Paint().apply { color = Color.RED })
      }
    }
    val file = File(images, "source_${width}x$height.jpg")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
    if (orientation != null) {
      ExifInterface(file).apply {
        setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
        saveAttributes()
      }
    }
    return file
  }

  private fun downscale(source: File): File {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", source)
    val out = File(images, "out.jpg")
    downscalePhoto(context.contentResolver, uri, out)
    return out
  }

  private fun decode(file: File): Bitmap = BitmapFactory.decodeFile(file.path)

  private fun isRed(color: Int) = Color.red(color) > 200 && Color.blue(color) < 60
  private fun isBlue(color: Int) = Color.blue(color) > 200 && Color.red(color) < 60

  @Test
  fun `large photo is scaled to 1080 on the long side`() {
    val out = decode(downscale(photo(2400, 1800)))
    assertEquals(1080, out.width)
    assertEquals(810, out.height)
    assertTrue(isRed(out.getPixel(100, 405)))
    assertTrue(isBlue(out.getPixel(980, 405)))
  }

  @Test
  fun `small photo keeps its size`() {
    val out = decode(downscale(photo(640, 480)))
    assertEquals(640, out.width)
    assertEquals(480, out.height)
  }

  @Test
  fun `EXIF rotation is applied so the photo is upright`() {
    // ROTATE_90: the stored image must be turned 90° clockwise, so its left (red) half ends up on top.
    val out = decode(downscale(photo(2400, 1800, orientation = ExifInterface.ORIENTATION_ROTATE_90)))
    assertEquals(810, out.width)
    assertEquals(1080, out.height)
    assertTrue(isRed(out.getPixel(405, 100)))
    assertTrue(isBlue(out.getPixel(405, 980)))
  }

  @Test
  fun `even a worst-case photo stays far below the 5 MB storage limit`() {
    // Random noise is the hardest thing to compress; a real photo comes out much smaller.
    val out = downscale(photo(3000, 2250, noise = true))
    assertTrue("${out.length()} bytes", out.length() < 2 * 1024 * 1024)
  }
}
