package com.verdant.app.ml

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

object ImagePreprocessor {

    /** Crops the largest centred square, then scales it to [width]x[height]. */
    fun centerCropAndScale(source: Bitmap, width: Int, height: Int): Bitmap {
        val side = min(source.width, source.height)
        val x = (source.width - side) / 2
        val y = (source.height - side) / 2
        val cropped = Bitmap.createBitmap(source, x, y, side, side)
        val scaled = Bitmap.createScaledBitmap(cropped, width, height, true)
        if (cropped !== source && cropped !== scaled) cropped.recycle()
        return scaled
    }

    /** Packs RGB pixels as float32, NHWC, normalized with [normalization]. */
    fun toFloatBuffer(bitmap: Bitmap, normalization: Normalization): ByteBuffer {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val buffer = ByteBuffer.allocateDirect(w * h * 3 * 4).order(ByteOrder.nativeOrder())
        val mean = normalization.mean
        val std = normalization.std
        for (p in pixels) {
            buffer.putFloat(((p shr 16 and 0xFF) - mean) / std)
            buffer.putFloat(((p shr 8 and 0xFF) - mean) / std)
            buffer.putFloat(((p and 0xFF) - mean) / std)
        }
        buffer.rewind()
        return buffer
    }

    /** Packs RGB pixels as raw bytes for uint8 / int8 quantized models. */
    fun toByteBuffer(bitmap: Bitmap, signed: Boolean): ByteBuffer {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val buffer = ByteBuffer.allocateDirect(w * h * 3).order(ByteOrder.nativeOrder())
        val offset = if (signed) 128 else 0
        for (p in pixels) {
            buffer.put(((p shr 16 and 0xFF) - offset).toByte())
            buffer.put(((p shr 8 and 0xFF) - offset).toByte())
            buffer.put(((p and 0xFF) - offset).toByte())
        }
        buffer.rewind()
        return buffer
    }
}
