package com.example.noti251022.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import java.io.File

object KeyStoreUtils {
    private const val KEY_ALIAS_PREFIX = "tg_key_"
    private const val KS_PROVIDER = "AndroidKeyStore"
    private const val AES_MODE = "AES/GCM/NoPadding"

    fun storeValue(context: Context, key: String, value: String) {
        val alias = KEY_ALIAS_PREFIX + key
        val ks = KeyStore.getInstance(KS_PROVIDER)
        ks.load(null)
        if (!ks.containsAlias(alias)) {
            val builder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KS_PROVIDER
            )
            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        }
        val keyObj = ks.getKey(alias, null) as SecretKey

        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, keyObj)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val data = Base64.encodeToString(iv + encrypted, Base64.DEFAULT)

        // 파일 저장 예시 (내부 파일, 필요 시 SharedPreferences 등 변경 가능)
        val file = File(context.filesDir, alias)
        file.writeText(data)
    }

    fun loadValue(context: Context, key: String): String? {
        val alias = KEY_ALIAS_PREFIX + key
        val ks = KeyStore.getInstance(KS_PROVIDER)
        ks.load(null)
        val keyObj = ks.getKey(alias, null) as? SecretKey ?: return null

        val file = File(context.filesDir, alias)
        if (!file.exists()) return null
        val data = Base64.decode(file.readText(), Base64.DEFAULT)
        val iv = data.sliceArray(0..11) // 12 bytes
        val encrypted = data.sliceArray(12 until data.size)

        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keyObj, spec)
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
