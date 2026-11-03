package com.example

import com.example.platform.PhotoSizing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PhotoSizingTest {

  @Test
  fun `long side is capped at 1080 and the aspect ratio kept`() {
    assertEquals(1080 to 810, PhotoSizing.targetSize(4000, 3000))
    assertEquals(810 to 1080, PhotoSizing.targetSize(3000, 4000))
    assertEquals(1080 to 1080, PhotoSizing.targetSize(1081, 1081))
    assertEquals(1080 to 608, PhotoSizing.targetSize(12000, 6750)) // 108 MP-class 16:9
  }

  @Test
  fun `small photos are never upscaled`() {
    assertEquals(640 to 480, PhotoSizing.targetSize(640, 480))
    assertEquals(1080 to 720, PhotoSizing.targetSize(1080, 720))
  }

  @Test
  fun `extreme panoramas keep at least one pixel`() {
    assertEquals(1080 to 1, PhotoSizing.targetSize(100_000, 1))
    assertEquals(1080 to 11, PhotoSizing.targetSize(10_000, 100))
  }

  @Test
  fun `empty images are rejected`() {
    assertFailsWith<IllegalArgumentException> { PhotoSizing.targetSize(0, 100) }
    assertFailsWith<IllegalArgumentException> { PhotoSizing.targetSize(-1, -1) }
  }

  @Test
  fun `subsampling stays at or above the target size`() {
    assertEquals(1, PhotoSizing.sampleSize(1000, 800))
    assertEquals(1, PhotoSizing.sampleSize(2159, 1000))
    assertEquals(2, PhotoSizing.sampleSize(4000, 3000))
    assertEquals(4, PhotoSizing.sampleSize(8000, 6000))
    for (side in listOf(1080, 2160, 4031, 9248, 12000)) {
      val sample = PhotoSizing.sampleSize(side, side / 2)
      assertTrue(side / sample >= 1080, "side $side sample $sample")
      assertTrue(side / (sample * 2) < 1080, "side $side sample $sample could be larger")
    }
  }
}
