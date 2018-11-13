package com.zyyoona7.cache

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.zyyoona7.cachemanager.KCache
import com.zyyoona7.cachemanager.ext.DEFAULT_DISK_MAX_SIZE

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //初始化缓存路径
        KCache.init(filesDir.absolutePath + "/diskCache", 1, DEFAULT_DISK_MAX_SIZE)

//        KCache.getCache().evictAll()

//        KCache.getCache().putString("1234","")
    }

}
