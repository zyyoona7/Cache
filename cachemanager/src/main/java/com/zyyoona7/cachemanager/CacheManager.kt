package com.zyyoona7.cachemanager

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.Serializable

/**
 * 轻量的二级缓存，Bitmap Drawable对象不会缓存到内存中
 *
 * @author   zyyoona7
 * @version  v1.0.0
 * @since    2018/9/21.
 */
object CacheManager {

    private var cachePath: String = Environment.getDownloadCacheDirectory().absolutePath + "/DiskCache"
    private var appVersion: Int = 1
    private var diskMaxSize: Long = DEFAULT_MAX_SIZE
    //内存缓存最大容量 两种模式 MemoryMode.Size文件大小模式，MemoryMode.Count 对象个数
    private var memoryMaxSize: Int = 1024 * 1024
    //内存缓存模式
    var memoryMode: MemoryMode = MemoryMode.Size
        set(value) {
            field = value
            //mode变化时清除内存缓存
            memoryCache.evictAll()
        }

    /**
     * [encryptKey]有值 则所有文件都加密
     */
    var encryptKey: String = ""

    private val diskCache: TimedDiskLruCache by lazy {
        TimedDiskLruCache(File(cachePath), appVersion, diskMaxSize)
    }

    private val memoryCache: TimedLruCache<String, Any> by lazy {
        object : TimedLruCache<String, Any>(memoryMaxSize) {
            override fun sizeOf(key: String, value: Any): Int {
                return memorySizeOf()
            }
        }
    }

    @JvmOverloads
    fun init(cachePath: String, appVersion: Int, diskMaxSize: Long = DEFAULT_MAX_SIZE,
             memoryMode: MemoryMode = MemoryMode.Size, memoryMaxSize: Int = 1024 * 1024,
             encryptKey: String = "") {
        this.cachePath = cachePath
        this.appVersion = appVersion
        this.diskMaxSize = diskMaxSize
        this.memoryMode = memoryMode
        this.memoryMaxSize = memoryMaxSize
        this.encryptKey = encryptKey
    }

    @JvmOverloads
    fun getAsJsonObj(key: String, password: String = encryptKey): JSONObject? {
        val value = getFromMemory<JSONObject>(key)
        if (value != null) {
            return value
        }
        return diskCache.getAsJsonObj(key, password)
    }

    fun put(key: String, jsonObject: JSONObject, password: String = encryptKey) {
        putMemory(key, jsonObject, DEFAULT_LIFE_TIME)
        diskCache.put(key, jsonObject, password)
    }

    @JvmOverloads
    fun put(key: String, jsonObject: JSONObject, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        putMemory(key, jsonObject, lifeTime)
        diskCache.put(key, jsonObject, lifeTime, password)
    }

    @JvmOverloads
    fun getAsJsonArray(key: String, password: String = encryptKey): JSONArray? {
        val value = getFromMemory<JSONArray>(key)
        if (value != null) {
            return value
        }
        return diskCache.getAsJsonArray(key, password)
    }

    fun put(key: String, jsonArray: JSONArray, password: String = encryptKey) {
        putMemory(key, jsonArray, DEFAULT_LIFE_TIME)
        diskCache.put(key, jsonArray, password)
    }

    @JvmOverloads
    fun put(key: String, jsonArray: JSONArray, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        putMemory(key, jsonArray, lifeTime)
        diskCache.put(key, jsonArray, lifeTime, password)
    }

    @JvmOverloads
    fun getAsBitmap(key: String, password: String = encryptKey): Bitmap? {
        if (memoryMode == MemoryMode.Size) {
            val value = getFromMemory<Bitmap>(key)
            if (value != null) {
                return value
            }
        } else {
            //mode 改变，将已有的bitmap移除
            memoryCache.remove(key)
        }
        return diskCache.getAsBitmap(key, password)
    }

    fun put(key: String, bitmap: Bitmap, password: String = encryptKey) {
        if (memoryMode == MemoryMode.Size) {
            putMemory(key, bitmap, DEFAULT_LIFE_TIME)
        }
        diskCache.put(key, bitmap, DEFAULT_LIFE_TIME, password)
    }

    @JvmOverloads
    fun put(key: String, bitmap: Bitmap, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        if (memoryMode == MemoryMode.Size) {
            putMemory(key, bitmap, lifeTime)
        }
        diskCache.put(key, bitmap, lifeTime, password)
    }

    @JvmOverloads
    fun getAsDrawable(key: String, password: String = encryptKey): Drawable? {
        if (memoryMode == MemoryMode.Size) {
            val value = getFromMemory<Drawable>(key)
            if (value != null) {
                return value
            }
        } else {
            memoryCache.remove(key)
        }
        return diskCache.getAsDrawable(key, password)
    }

    fun put(key: String, drawable: Drawable, password: String = encryptKey) {
        if (memoryMode == MemoryMode.Size) {
            putMemory(key, drawable, DEFAULT_LIFE_TIME)
        }
        diskCache.put(key, drawable, DEFAULT_LIFE_TIME, password)
    }

    @JvmOverloads
    fun put(key: String, drawable: Drawable, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        if (memoryMode == MemoryMode.Size) {
            putMemory(key, drawable, lifeTime)
        }
        diskCache.put(key, drawable, lifeTime, password)
    }

    @JvmOverloads
    fun getAsString(key: String, password: String = encryptKey): String {
        val value = getFromMemory<String>(key)
        if (value != null) {
            return value
        }
        return diskCache.getAsString(key, password)
    }

    fun put(key: String, string: String, password: String = encryptKey) {
        putMemory(key, string, DEFAULT_LIFE_TIME)
        diskCache.put(key, string, DEFAULT_LIFE_TIME, password)
    }

    @JvmOverloads
    fun put(key: String, string: String, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        putMemory(key, string, lifeTime)
        diskCache.put(key, string, lifeTime, password)
    }

    @JvmOverloads
    fun getAsByteArray(key: String, password: String = encryptKey): ByteArray? {
        val value = getFromMemory<ByteArray>(key)
        if (value != null) {
            return value
        }
        return diskCache.getAsByteArray(key, password)
    }

    fun put(key: String, byteArray: ByteArray, password: String = encryptKey) {
        putMemory(key, byteArray, DEFAULT_LIFE_TIME)
        diskCache.put(key, byteArray, DEFAULT_LIFE_TIME, password)
    }

    @JvmOverloads
    fun put(key: String, byteArray: ByteArray, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        putMemory(key, byteArray, lifeTime)
        diskCache.put(key, byteArray, lifeTime, password)
    }

    @JvmOverloads
    fun <T> getAsSerializable(key: String, password: String = encryptKey): T? {
        val value = getFromMemory<T>(key)
        if (value != null) {
            return value
        }
        return diskCache.getAsSerializable(key, password)
    }

    fun put(key: String, serializable: Serializable, password: String = encryptKey) {
        putMemory(key, serializable, DEFAULT_LIFE_TIME)
        diskCache.put(key, serializable, DEFAULT_LIFE_TIME, password)
    }

    @JvmOverloads
    fun put(key: String, serializable: Serializable, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        putMemory(key, serializable, lifeTime)
        diskCache.put(key, serializable, lifeTime, password)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getFromMemory(key: String): T? {
        try {
            val value = memoryCache.get(key)
            return if (value == null) null else value as T
        } catch (e: ClassCastException) {
            logw("CacheManager", e.toString())
        } catch (e: ClassNotFoundException) {
            logw("CacheManager", e.toString())
        }
        return null
    }

    private fun <T> putMemory(key: String, value: T, lifeTime: Long) {
        if (value != null) {
            memoryCache.put(key, value, lifeTime)
        }
    }

    private fun memorySizeOf(): Int {
        return if (memoryMode == MemoryMode.Size) {
            if (diskSize() > memoryMaxSize) memoryMaxSize else diskSize().toInt()
        } else {
            1
        }
    }

    fun memorySize(): Int {
        return memoryCache.size()
    }

    fun diskSize(): Long {
        return diskCache.size()
    }

    fun memoryMaxSize(): Int {
        return memoryCache.maxSize()
    }

    fun diskMaxSize(): Long {
        return diskCache.maxSize()
    }
}

enum class MemoryMode {
    Size, Count
}
