package com.jonecx.ibex.data.crypto

interface CryptoManager {
    fun encrypt(plaintext: String): String
    fun decrypt(ciphertext: String): String
}
