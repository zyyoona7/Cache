package com.zyyoona7.cachemanager

import android.os.Environment
import android.support.v4.util.SimpleArrayMap
import com.gojuno.koptional.Optional
import com.zyyoona7.cachemanager.cache.CacheManager
import com.zyyoona7.cachemanager.cache.DiskCache
import com.zyyoona7.cachemanager.cache.MemoryCache
import com.zyyoona7.cachemanager.ext.DEFAULT_DISK_MAX_SIZE
import com.zyyoona7.cachemanager.ext.DEFAULT_MEMORY_MAX_SIZE
import com.zyyoona7.cachemanager.rx.RxKCache
import io.reactivex.ObservableTransformer
import java.io.File

/**
 * @author   zyyoona7
 * @version  v1.0.0
 * @since    2018/9/28.
 */
class KCache{

    companion object {
        private val mMemoryCacheMap: SimpleArrayMap<String, MemoryCache> = SimpleArrayMap()
        private val mDiskCacheMap: SimpleArrayMap<String, DiskCache> = SimpleArrayMap()
        private val mCacheMap: SimpleArrayMap<String, CacheManager> = SimpleArrayMap()
        private var mCachePath: String = Environment.getDownloadCacheDirectory().absolutePath + "/DiskCache"
        private var mAppVersion: Int = 1
        private var mDiskMaxSize: Long = DEFAULT_DISK_MAX_SIZE

        /**
         * 初始化默认的缓存参数
         */
        @JvmStatic
        fun init(cachePath: String, appVersion: Int, diskMaxSize: Long) {
            mCachePath = cachePath
            mAppVersion = appVersion
            mDiskMaxSize = diskMaxSize
        }

        /**
         * 获取或者创建CacheManager对象
         */
        @JvmStatic
        @JvmOverloads
        fun getCache(memoryCache: MemoryCache = getMemoryCache(),
                     diskCache: DiskCache = getDiskCache(), encryptPwd: String = ""): CacheManager {
            val cacheKey = "$memoryCache._$diskCache"
            val cacheManager = mCacheMap[cacheKey]
            return if (cacheManager == null) {
                val newCacheManager = CacheManager(memoryCache, diskCache, encryptPwd)
                mCacheMap.put(cacheKey, newCacheManager)
                newCacheManager
            } else cacheManager
        }

        /**
         * 获取或者创建MemoryCache对象
         */
        @JvmOverloads
        @JvmStatic
        fun getMemoryCache(memoryMaxSize: Int = DEFAULT_MEMORY_MAX_SIZE,
                           mode: MemoryCache.SizeMode = MemoryCache.SizeMode.Size): MemoryCache {
            return getMemoryCache(memoryMaxSize.toString(), memoryMaxSize, mode)
        }

        /**
         * 获取或者创建MemoryCache对象
         */
        @JvmStatic
        fun getMemoryCache(cacheKey: String, memoryMaxSize: Int = DEFAULT_MEMORY_MAX_SIZE,
                           mode: MemoryCache.SizeMode = MemoryCache.SizeMode.Size): MemoryCache {
            val memoryCache = mMemoryCacheMap[cacheKey]
            return if (memoryCache == null) {
                val newMemoryCache = MemoryCache(memoryMaxSize, mode)
                mMemoryCacheMap.put(cacheKey, newMemoryCache)
                newMemoryCache
            } else memoryCache
        }

        /**
         * 获取或者创建DiskCache对象
         */
        @JvmStatic
        @JvmOverloads
        fun getDiskCache(diskCachePath: String = mCachePath, appVersion: Int = mAppVersion,
                         diskMaxSize: Long = mDiskMaxSize): DiskCache {
            val cacheKey = "${diskCachePath}_$diskMaxSize"
            val diskCache = mDiskCacheMap[cacheKey]
            return if (diskCache == null) {
                val newDiskCache = DiskCache(File(diskCachePath), appVersion, diskMaxSize)
                mDiskCacheMap.put(cacheKey, newDiskCache)
                newDiskCache
            } else diskCache
        }

        /**
         * Rx支持
         */
        @JvmOverloads
        @JvmStatic
        fun toObservable(cacheManager: CacheManager = getCache()): RxKCache {
            return RxKCache(cacheManager)
        }
    }
}


/**
 * 将Optional类型转换成 T 类型如果为null 返回 [defaultValue]
 */
fun <T : Any> mapTo(defaultValue: T): ObservableTransformer<Optional<T>, T> {
    return ObservableTransformer { it ->
        it.map {
            it.toNullable() ?: defaultValue
        }
    }
}