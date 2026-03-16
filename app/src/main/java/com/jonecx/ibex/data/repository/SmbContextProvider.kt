package com.jonecx.ibex.data.repository

import jcifs.CIFSContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmbContextProvider @Inject constructor() {

    private val contexts = ConcurrentHashMap<String, CIFSContext>()

    fun register(host: String, context: CIFSContext) {
        contexts[host] = context
    }

    fun get(host: String): CIFSContext? = contexts[host]

    companion object {
        fun smbCacheKey(path: String): String =
            path.hashCode().and(Int.MAX_VALUE).toString()
    }
}
