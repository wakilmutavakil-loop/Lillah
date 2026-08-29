package com.lillah.dhikr.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * A chosen cover is copied into the app's own storage rather than referenced by content URI.
 *
 * Content URIs can be revoked, the backing file can be moved or deleted, and a permission grant
 * does not survive a reinstall — none of which suits an app that is meant to work offline and keep
 * looking the way the user left it. The copy is downscaled, so covers cost tens of kilobytes.
 */
class CoverImageStore(private val context: Context) {

    private val directory: File
        get() = File(context.filesDir, "covers").apply { if (!exists()) mkdirs() }

    suspend fun save(collectionId: Long, source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            } ?: return@runCatching null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@runCatching null

            val oriented = applyOrientation(decoded, source)
            // A fresh filename each time defeats any image cache holding the previous cover.
            val target = File(directory, "cover_${collectionId}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(target).use { out ->
                oriented.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            if (oriented !== decoded) decoded.recycle()
            oriented.recycle()
            target.absolutePath
        }.getOrNull()
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() && it.parentFile == directory }?.delete() }
    }

    /** Removes covers left behind by replaced images or deleted collections. */
    suspend fun pruneExcept(keep: Collection<String>) = withContext(Dispatchers.IO) {
        runCatching {
            val kept = keep.filterNotNull().toHashSet()
            directory.listFiles()?.forEach { file ->
                if (file.absolutePath !in kept) file.delete()
            }
        }
        Unit
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        val longest = maxOf(width, height)
        while (longest / sample > MAX_DIMENSION * 2) sample *= 2
        return sample
    }

    private fun applyOrientation(bitmap: Bitmap, source: Uri): Bitmap {
        val rotation = runCatching {
            context.contentResolver.openInputStream(source)?.use { stream ->
                when (
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL,
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)

        val scale = MAX_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height).toFloat()
        if (rotation == 0f && scale >= 1f) return bitmap

        val matrix = Matrix()
        if (rotation != 0f) matrix.postRotate(rotation)
        if (scale < 1f) matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private companion object {
        const val MAX_DIMENSION = 1_400
    }
}
