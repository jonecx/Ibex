package com.jonecx.ibex.data.crypto

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TinkCryptoManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : CryptoManager {

    private val aead: Aead by lazy {
        AeadConfig.register()
        val keysetHandle: KeysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
        @Suppress("DEPRECATION")
        keysetHandle.getPrimitive(Aead::class.java)
    }

    override fun encrypt(plaintext: String): String {
        if (plaintext.isEmpty()) return ""
        val ciphertext = aead.encrypt(plaintext.toByteArray(Charsets.UTF_8), ASSOCIATED_DATA)
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    override fun decrypt(ciphertext: String): String {
        if (ciphertext.isEmpty()) return ""
        val decoded = Base64.decode(ciphertext, Base64.NO_WRAP)
        val plaintext = aead.decrypt(decoded, ASSOCIATED_DATA)
        return String(plaintext, Charsets.UTF_8)
    }

    companion object {
        private const val KEYSET_NAME = "ibex_smb_keyset"
        private const val PREF_FILE_NAME = "ibex_smb_keyset_prefs"
        private const val MASTER_KEY_URI = "android-keystore://ibex_smb_master_key"
        private val ASSOCIATED_DATA = "smb_credentials".toByteArray(Charsets.UTF_8)
    }
}
