package com.zyyoona7.cachemanager

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.jakewharton.disklrucache.DiskLruCache
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 * 带过期时间和加/解密的DiskLruCache
 *
 * @author   zyyoona7
 * @version  v1.0.0
 * @since    2018/9/20.
 */
open class TimedDiskLruCache @JvmOverloads constructor(dictionary: File, appVersion: Int,
                                                       maxSize: Long = DEFAULT_MAX_SIZE) {

    private val mTag: String = "TimedDiskLruCache"
    private var mDiskLruCache: DiskLruCache = DiskLruCache.open(dictionary,
            appVersion, 2, maxSize)
    /**
     * [encryptKey]有值 则所有文件都加密
     */
    var encryptKey: String = ""

    /**
     * 获取JSONObject类型数据
     *
     * @param key
     * @param password 加密Key
     */
    @JvmOverloads
    fun getAsJsonObj(key: String, password: String = encryptKey): JSONObject? {
        return try {
            JSONObject(getAsString(key, password))
        } catch (e: JSONException) {
            logw(mTag, e.toString())
            null
        }
    }

    /**
     * 保存JSONObject类型数据
     *
     * @param key
     * @param jsonObject
     * @param password 加密Key
     */
    fun put(key: String, jsonObject: JSONObject, password: String = encryptKey) {
        put(key, jsonObject, DEFAULT_LIFE_TIME, password)
    }

    /**
     * 保存JSONObject类型数据
     *
     * @param key
     * @param jsonObject
     * @param lifeTime 有效时间
     * @param password 加密Key
     */
    @JvmOverloads
    fun put(key: String, jsonObject: JSONObject, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        put(key, jsonObject.toString(), lifeTime, password)
    }

    /**
     * 获取JSONArray类型数据
     *
     * @param key
     */
    @JvmOverloads
    fun getAsJsonArray(key: String, password: String = encryptKey): JSONArray? {
        return try {
            JSONArray(getAsString(key, password))
        } catch (e: JSONException) {
            logw(mTag, e.toString())
            null
        }
    }

    /**
     * 保存JSONArray类型数据
     *
     * @param key
     * @param jsonArray
     * @param password 加密Key
     */
    fun put(key: String, jsonArray: JSONArray, password: String = encryptKey) {
        put(key, jsonArray, DEFAULT_LIFE_TIME, password)
    }

    /**
     * 保存JSONArray类型数据
     *
     * @param key
     * @param jsonArray
     * @param lifeTime 有效时间
     * @param password 加密Key
     */
    @JvmOverloads
    fun put(key: String, jsonArray: JSONArray, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        put(key, jsonArray.toString(), lifeTime, password)
    }

    /**
     * 获取Bitmap类型数据
     *
     * @param key
     */
    @JvmOverloads
    fun getAsBitmap(key: String, password: String = encryptKey): Bitmap? {
        return getAsByteArray(key, password).toBitmap()
    }

    /**
     * 保存Bitmap类型数据
     *
     * @param key
     * @param bitmap
     * @param password 加密Key
     */
    fun put(key: String, bitmap: Bitmap, password: String = encryptKey) {
        put(key, bitmap, DEFAULT_LIFE_TIME, password)
    }

    /**
     * 保存Bitmap类型数据
     *
     * @param key
     * @param bitmap
     * @param lifeTime 有效时间
     * @param password 加密Key
     */
    @JvmOverloads
    fun put(key: String, bitmap: Bitmap, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        put(key, bitmap.toByteArray(), lifeTime, password)
    }

    /**
     * 获取Drawable类型数据
     *
     * @param key
     */
    @JvmOverloads
    fun getAsDrawable(key: String, password: String = encryptKey): Drawable? {
        return getAsBitmap(key, password).toDrawable()
    }

    /**
     * 保存Drawable类型数据
     *
     * @param key
     * @param drawable
     * @param password 加密Key
     */
    fun put(key: String, drawable: Drawable, password: String = encryptKey) {
        put(key, drawable, DEFAULT_LIFE_TIME, password)
    }

    /**
     * 保存Drawable类型数据
     *
     * @param key
     * @param drawable
     * @param lifeTime 有效时间
     * @param password 加密Key
     */
    @JvmOverloads
    fun put(key: String, drawable: Drawable, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        put(key, drawable.toBitmap(), lifeTime, password)
    }

    /**
     * 获取String类型数据
     *
     * @param key
     * @param password
     */
    @JvmOverloads
    fun getAsString(key: String, password: String = encryptKey): String {
        try {
            val snapshot = get(key) ?: return ""
            val lifeTime = snapshot.getLifeTime()
            if (lifeTime == -1L || System.currentTimeMillis() < lifeTime) {
                val inputStream = handleOrDecrypt(snapshot, password) ?: return ""
                return inputStream.readTextAndClose()
            } else {
                //remove key
                remove(key)
                return ""
            }
        } catch (e: IOException) {
            logw(mTag, e.toString())
            return ""
        }
    }

    /**
     * 保存String类型数据
     *
     * @param key
     * @param string
     * @param password 加密Key
     */
    fun put(key: String, string: String, password: String = encryptKey) {
        put(key, string, DEFAULT_LIFE_TIME, password)
    }

    /**
     * 保存String类型数据
     *
     * @param key
     * @param string
     * @param lifeTime 有效时间
     * @param password 加密Key
     */
    @JvmOverloads
    fun put(key: String, string: String, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        try {
            val editor = mDiskLruCache.edit(key.md5()) ?: return
            val outputStream: OutputStream = handleOrEncrypt(editor, password) ?: return

            if (writeToString(string, outputStream)) {
                editor.setLifTime(lifeTime)
                        .commit()
            } else {
                editor.abort()
            }
        } catch (e: IOException) {
            logw(mTag, e.toString())
        }
    }

    /**
     * 获取ByteArray类型数据
     *
     * @param key
     */
    @JvmOverloads
    fun getAsByteArray(key: String, password: String = encryptKey): ByteArray? {
        try {
            val snapshot = get(key) ?: return null
            val lifeTime = snapshot.getLifeTime()
            if (lifeTime == DEFAULT_LIFE_TIME || System.currentTimeMillis() < lifeTime) {
                val byteArrayOutputStream = ByteArrayOutputStream()
                val inputStream = handleOrDecrypt(snapshot, password) ?: return null
                inputStream.use { input ->
                    byteArrayOutputStream.use {
                        input.copyTo(it, 512)
                    }
                }
                return byteArrayOutputStream.toByteArray()
            } else {
                remove(key)
                return null
            }
        } catch (e: IOException) {
            logw(mTag, e.toString())
            return null
        }
    }

    /**
     * 保存ByteArray类型数据
     *
     * @param key
     * @param byteArray
     * @param password 加密Key
     */
    fun put(key: String, byteArray: ByteArray, password: String = encryptKey) {
        put(key, byteArray, DEFAULT_LIFE_TIME, password)
    }

    /**
     * 保存ByteArray类型数据
     *
     * @param key
     * @param byteArray
     * @param lifeTime 有效时间
     * @param password 加密Key
     */
    @JvmOverloads
    fun put(key: String, byteArray: ByteArray, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        try {
            val editor = mDiskLruCache.edit(key.md5()) ?: return
            val outputStream = handleOrEncrypt(editor, password) ?: return
            if (writeToBytes(byteArray, outputStream)) {
                editor.setLifTime(lifeTime)
                        .commit()
            } else {
                editor.abort()
            }
        } catch (e: IOException) {
            logw(mTag, e.toString())
        }
    }

    /**
     * 获取Serializable类型的数据
     *
     * @param key
     */
    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    fun <T> getAsSerializable(key: String, password: String = encryptKey): T? {
        try {
            val snapshot = get(key) ?: return null
            val lifeTime = snapshot.getLifeTime()
            if (lifeTime == DEFAULT_LIFE_TIME || System.currentTimeMillis() < lifeTime) {
                val inputStream = handleOrDecrypt(snapshot, password) ?: return null
                return ObjectInputStream(inputStream)
                        .readObject() as T
            } else {
                remove(key)
                return null
            }
        } catch (e: IOException) {
            logw(mTag, e.toString())
        } catch (classNotFound: ClassNotFoundException) {
            logw(mTag, classNotFound.toString())
        } catch (castException: ClassCastException) {
            logw(mTag, castException.toString())
        }
        return null
    }


    /**
     * 保存Serializable类型的数据
     *
     * @param key
     * @param serializable
     * @param password 加密key
     */
    fun put(key: String, serializable: Serializable, password: String = encryptKey) {
        put(key, serializable, DEFAULT_LIFE_TIME, password)
    }

    /**
     * 保存Serializable类型的数据
     *
     * @param key
     * @param serializable
     * @param lifeTime 有效时间
     * @param password 加密key
     */
    @JvmOverloads
    fun put(key: String, serializable: Serializable, lifeTime: Long = DEFAULT_LIFE_TIME,
            password: String = encryptKey) {
        try {
            val editor = mDiskLruCache.edit(key.md5()) ?: return
            val outputStream = handleOrEncrypt(editor, password) ?: return
            if (writeToSerializable(serializable, outputStream)) {
                editor.setLifTime(lifeTime)
                        .commit()
            } else {
                editor.abort()
            }
        } catch (e: IOException) {
            logw(mTag, e.toString())
        }
    }

    /**
     * 通用的get
     *
     * @param key
     */
    fun get(key: String): DiskLruCache.Snapshot? {
        return try {
            mDiskLruCache.get(key.md5())
        } catch (e: IOException) {
            logw(mTag, e.toString())
            null
        }
    }

    /**
     * 移除数据
     *
     * @param key
     */
    fun remove(key: String): Boolean {
        return try {
            mDiskLruCache.remove(key.md5())
        } catch (e: IOException) {
            logw(mTag, e.toString())
            false
        }
    }

    /**
     * 删除所有缓存
     */
    fun evictAll() {
        mDiskLruCache.delete()
    }

    /**
     * 缓存大小
     */
    fun size(): Long {
        return mDiskLruCache.size()
    }

    /**
     * 最大缓存
     */
    fun maxSize(): Long {
        return mDiskLruCache.maxSize
    }

    /**
     * OutputStream写入String类型数据
     */
    private fun writeToString(value: String, os: OutputStream): Boolean {
        BufferedWriter(OutputStreamWriter(os))
                .use({
                    it.write(value)
                    return true
                }, {
                    return false
                })
    }

    /**
     * OutputStream写入ByteArray
     */
    private fun writeToBytes(byteArray: ByteArray, os: OutputStream): Boolean {
        os.use({
            it.write(byteArray)
            it.flush()
            return true
        }) {
            return false
        }
    }

    /**
     * OutputStream写入Serializable
     *
     * @param serializable serializable
     * @param os outputStream
     */
    private fun writeToSerializable(serializable: Serializable, os: OutputStream): Boolean {
        ObjectOutputStream(os).use({
            it.writeObject(serializable)
            it.flush()
            return true
        }) {
            return false
        }
    }

    /**
     * 如果password不为空则加密
     *
     * @param editor editor
     * @param password 加密Key
     */
    private fun handleOrEncrypt(editor: DiskLruCache.Editor, password: String): OutputStream? {
        var outputStream = editor.newOutputStream(0) ?: return null
        //加密
        if (password.isNotEmpty()) {
            val encryptCipher = initEncryptCipher(password)
            outputStream = CipherOutputStream(outputStream, encryptCipher)
        }
        return outputStream
    }

    /**
     * 如果password不为空则解密
     *
     * @param snapshot snapshot
     * @param password 加密Key
     */
    private fun handleOrDecrypt(snapshot: DiskLruCache.Snapshot, password: String): InputStream? {
        //解密
        var inputStream = snapshot.getInputStream(0) ?: return null
        if (password.isNotEmpty()) {
            val decryptCipher = initDecryptCipher(password)
            inputStream = CipherInputStream(inputStream, decryptCipher)
        }
        return inputStream
    }

    /**
     * 初始化加密cipher
     *
     * @param key 加密Key
     */
    private fun initEncryptCipher(key: String): Cipher? {
        return try {
            val secretKey = generateSecretKey(key)
            val encryptCipher = Cipher.getInstance(AES_TRANSFORM)
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey,
                    IvParameterSpec(ByteArray(encryptCipher.blockSize)))
            encryptCipher
        } catch (e: Exception) {
            logw(mTag, e.toString())
            null
        }
    }

    /**
     * 初始化解密cipher
     *
     * @param key 加密Key
     */
    private fun initDecryptCipher(key: String): Cipher? {
        return try {
            val secretKey = generateSecretKey(key)
            val decryptCipher = Cipher.getInstance(AES_TRANSFORM)
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKey,
                    IvParameterSpec(ByteArray(decryptCipher.blockSize)))
            decryptCipher
        } catch (e: Exception) {
            logw(mTag, e.toString())
            null
        }
    }

    /**
     * 生成SecretKey
     *
     * @param key 加密Key
     */
    private fun generateSecretKey(key: String): SecretKeySpec {
        return SecretKeySpec(
                InsecureSHA1PRNGKeyDerivator.deriveInsecureKey(key.toByteArray(), 32),
                AES)
    }

}