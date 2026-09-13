package com.example.clinexusapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/** Creates a consistently oriented, square and bandwidth-friendly profile image. */
fun createProfileImagePart(
    context: Context,
    uri: Uri,
    rotationDegrees: Float = 0f,
    zoom: Float = 1f,
    panFractionX: Float = 0f,
    panFractionY: Float = 0f,
): MultipartBody.Part? = runCatching {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val longestSide = maxOf(info.size.width, info.size.height)
        if (longestSide > 1600) {
            val scale = 1600f / longestSide
            decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
        }
    }
    val rotated = if (rotationDegrees % 360f != 0f) {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, android.graphics.Matrix().apply {
            postRotate(rotationDegrees)
        }, true)
    } else decoded
    val baseSide = min(rotated.width, rotated.height)
    val safeZoom = zoom.coerceIn(1f, 4f)
    val side = (baseSide / safeZoom).toInt().coerceAtLeast(1)
    val centeredX = (rotated.width - side) / 2f
    val centeredY = (rotated.height - side) / 2f
    val sourceShift = baseSide / safeZoom
    val cropX = (centeredX - panFractionX * sourceShift)
        .toInt().coerceIn(0, rotated.width - side)
    val cropY = (centeredY - panFractionY * sourceShift)
        .toInt().coerceIn(0, rotated.height - side)
    val square = Bitmap.createBitmap(rotated, cropX, cropY, side, side)
    val output = File(context.cacheDir, "profile_upload_${System.currentTimeMillis()}.jpg")
    FileOutputStream(output).use { square.compress(Bitmap.CompressFormat.JPEG, 86, it) }
    if (square !== rotated) square.recycle()
    if (rotated !== decoded) rotated.recycle()
    decoded.recycle()
    MultipartBody.Part.createFormData("file", output.name, output.asRequestBody("image/jpeg".toMediaType()))
}.getOrNull()
