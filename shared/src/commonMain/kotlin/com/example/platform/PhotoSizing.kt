package com.example.platform

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * One size and quality for profile photos on both platforms. At 1080 px and JPEG quality 80 a photo is
 * typically 100–400 KB, well under the 5 MB limit in storage.rules.
 */
object PhotoSizing {
    const val MAX_SIDE = 1080
    const val JPEG_QUALITY = 80

    /** The size to encode at: long side capped at [maxSide], aspect ratio kept, never upscaled. */
    fun targetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Pair<Int, Int> {
        require(width > 0 && height > 0) { "Image has no size: ${width}x$height" }
        val longSide = max(width, height)
        if (longSide <= maxSide) return width to height
        val scale = maxSide.toDouble() / longSide
        return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
    }

    /**
     * The largest power-of-two subsampling for a first, cheap decode that still leaves the long side at
     * least [maxSide], so the final resize never has to scale up.
     */
    fun sampleSize(width: Int, height: Int, maxSide: Int = MAX_SIDE): Int {
        val longSide = max(width, height)
        var sample = 1
        while (longSide / (sample * 2) >= maxSide) sample *= 2
        return sample
    }
}
