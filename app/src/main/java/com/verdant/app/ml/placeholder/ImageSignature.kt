package com.verdant.app.ml.placeholder

import android.graphics.Bitmap
import android.graphics.Color

/** A cheap fingerprint of a photo so placeholder results are stable for the same image. */
internal data class ImageSignature(val hash: Int, val brightness: Float, val greenness: Float) {
    companion object {
        fun of(bitmap: Bitmap): ImageSignature {
            val small = Bitmap.createScaledBitmap(bitmap, 8, 8, true)
            var hash = 17
            var brightness = 0f
            var greenness = 0f
            for (y in 0 until 8) for (x in 0 until 8) {
                val p = small.getPixel(x, y)
                val r = Color.red(p); val g = Color.green(p); val b = Color.blue(p)
                hash = 31 * hash + (r shr 4) * 256 + (g shr 4) * 16 + (b shr 4)
                brightness += (r + g + b) / 3f
                greenness += g - (r + b) / 2f
            }
            if (small !== bitmap) small.recycle()
            return ImageSignature(hash and Int.MAX_VALUE, brightness / 64f, greenness / 64f)
        }
    }
}
