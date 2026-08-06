package com.jonecx.ibex.data.repository

import jcifs.CIFSContext
import java.util.concurrent.ConcurrentHashMap

interface SmbContextProviderContract {
    fun register(host: String, context: CIFSContext)
    fun get(host: String): CIFSContext?

    companion object {
        fun smbCacheKey(path: String): String =
            path.hashCode().and(Int.MAX_VALUE).toString()
    }
}

class SmbContextProvider() : SmbContextProviderContract {

    private val contexts = ConcurrentHashMap<String, CIFSContext>()

    override fun register(host: String, context: CIFSContext) {
        contexts[host] = context
    }

    override fun get(host: String): CIFSContext? = contexts[host]
}
