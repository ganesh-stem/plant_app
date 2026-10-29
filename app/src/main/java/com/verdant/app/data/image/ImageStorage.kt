package com.verdant.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.verdant.app.ml.ModelConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class ImageImportException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Owns every photo file the app creates.
 *  - Camera captures go to `cache/captures` (temporary).
 *  - Imported photos are upright-rotated, downscaled and saved to `files/scans`.
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scansDir: File get() = File(context.filesDir, "scans").apply { mkdirs() }
    private val capturesDir: File get() = File(context.cacheDir, "captures").apply { mkdirs() }

    fun newCaptureFile(): File = File(capturesDir, "capture_${System.currentTimeMillis()}.jpg")

    /**
     * Copies [uri] (camera file or photo-picker content URI) into private storage.
     * Returns the saved file and a bitmap ready for inference.
     */
    suspend fun import(uri: Uri): Pair<File, Bitmap> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw ImageImportException("Couldn't open the photo")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ImageImportException("Unsupported image format")

        val maxEdge = ModelConfig.STORED_IMAGE_MAX_EDGE
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxEdge || bounds.outHeight / (sample * 2) >= maxEdge) sample *= 2

        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw ImageImportException("Couldn't decode the photo")

        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)

        val upright = scaleAndRotate(decoded, maxEdge, rotation)
        val file = File(scansDir, "${UUID.randomUUID()}.jpg")
        try {
            file.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        } catch (e: IOException) {
            file.delete()
            throw ImageImportException("Not enough storage to save the photo", e)
        }
        file to upright
    }

    /** Deletes a temporary camera capture once nothing needs it any more. */
    fun discardCapture(uri: Uri) {
        if (uri.scheme != "file") return
        val path = uri.path ?: return
        val file = File(path)
        if (file.parentFile?.absolutePath == capturesDir.absolutePath) file.delete()
    }

    private fun scaleAndRotate(source: Bitmap, maxEdge: Int, rotation: Int): Bitmap {
        val longest = maxOf(source.width, source.height)
        val scale = if (longest > maxEdge) maxEdge.toFloat() / longest else 1f
        if (scale == 1f && rotation == 0) return source
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation.toFloat())
        }
        val result = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (result !== source) source.recycle()
        return result
    }

    suspend fun delete(path: String) = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.parentFile?.absolutePath == scansDir.absolutePath) file.delete()
    }

    /** Removes photos that no longer belong to any scan (e.g. after a crash mid-analysis). */
    suspend fun deleteOrphans(keep: Set<String>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        // Skip very recent files so an in-flight analysis is never affected.
        scansDir.listFiles()
            ?.filter { it.absolutePath !in keep && now - it.lastModified() > 3_600_000 }
            ?.forEach { it.delete() }
        capturesDir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }
            ?.forEach { it.delete() }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        scansDir.listFiles()?.forEach { it.delete() }
        capturesDir.listFiles()?.forEach { it.delete() }
    }
}
