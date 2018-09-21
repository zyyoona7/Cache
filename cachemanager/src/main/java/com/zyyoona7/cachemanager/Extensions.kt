@file:JvmName("Utils")

package com.zyyoona7.cachemanager

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import com.jakewharton.disklrucache.DiskLruCache
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


/**
 * @author   zyyoona7
 * @version  v1.0.0
 * @since    2018/9/20.
 */

const val DEFAULT_MAX_SIZE: Long = 50 * 1024 * 1024L
const val DEFAULT_LIFE_TIME: Long = -1L
const val AES = "AES"
const val AES_TRANSFORM = "AES/CBC/PKCS5Padding"

/**
 * ByteArray转换成16进制字符串
 * https://stackoverflow.com/a/21178195/8546297
 */
fun ByteArray.toHex(): String {
    val result = StringBuilder()
    forEach {
        result.append(Character.forDigit((it.toInt() shr 4) and 0xF, 16))
        result.append(Character.forDigit(it.toInt() and 0xF, 16))
    }
    return result.toString().toLowerCase()
}

/**
 * md5加密
 *
 * @param salt 盐值
 */
fun String.md5(salt: String = ""): String = hashFunc("MD5", this + salt)

/**
 * hash 函数模板
 *
 * @param algorithmType
 * @param data
 */
private fun hashFunc(algorithmType: String, data: String): String {
    return try {
        val md = MessageDigest.getInstance(algorithmType)
        md.digest(data.toByteArray()).toHex()
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
        ""
    }
}

inline fun <T : Closeable?, R> T.use(block: (T) -> R, errorBlock: (T) -> R): R {
    return try {
        block(this)
    } catch (e: Throwable) {
        errorBlock(this)
    } finally {
        when {
            this == null -> {
            }
            else ->
                try {
                    close()
                } catch (closeException: Throwable) {
                    // cause.addSuppressed(closeException) // ignored here
                }
        }
    }
}

fun Any.logw(tag: String, content: String) {
    Log.w(tag, content)
}

fun Any.logd(tag: String, content: String) {
    Log.d(tag, content)
}

fun DiskLruCache.Snapshot.getLifeTime(): Long {
    val string = getString(1) ?: return -1L
    return string.toLong()
}

fun DiskLruCache.Snapshot.getInputStream(): InputStream? {
    return getInputStream(0)
}

fun DiskLruCache.Editor.setLifTime(lifeTime: Long): DiskLruCache.Editor {
    val timestamp: Long = if (lifeTime == -1L) -1L else System.currentTimeMillis()
    this.set(1, timestamp.toString())
    return this
}

fun InputStream.readTextAndClose(charset: Charset = Charsets.UTF_8): String {
    return this.bufferedReader(charset).use { it.readText() }
}

fun Bitmap.toByteArray(quality: Int = 100): ByteArray {
    val stream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.PNG, quality, stream)
    return stream.toByteArray()
}

fun Bitmap?.toDrawable(resources: Resources = Resources.getSystem()): Drawable? {
    return if (this == null) null else BitmapDrawable(resources, this)
}

//fun ByteArray.toBitmap(): Bitmap {
//    return BitmapFactory.decodeByteArray(this, 0, this.size)
//}

fun ByteArray?.toBitmap(): Bitmap? {
    return if (this == null) null else BitmapFactory.decodeByteArray(this, 0, this.size)
}

fun Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable) {
        if (this.bitmap != null) {
            return this.bitmap
        }
    }
    val bitmap = if (this.intrinsicWidth <= 0 || this.intrinsicHeight <= 0) {
        // Single color bitmap will be created of 1x1 pixel
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(this.intrinsicWidth, this.intrinsicHeight, Bitmap.Config.ARGB_8888)
    }

    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)
    return bitmap
}