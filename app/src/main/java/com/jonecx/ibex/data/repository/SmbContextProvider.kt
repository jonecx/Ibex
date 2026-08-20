package com.jonecx.ibex.data.repository

import jcifs.CIFSContext
import java.util.concurrent.ConcurrentHashMap

interface SmbContextProviderContract {
    fun get(host: String): CIFSContext?

    // Reuse the live context for a host, rebuilding only when the connection signature changes.
    fun getOrCreate(host: String, signature: String, factory: () -> CIFSContext): CIFSContext

    companion object {
        fun smbCacheKey(path: String): String =
            path.hashCode().and(Int.MAX_VALUE).toString()
    }
}

class SmbContextProvider() : SmbContextProviderContract {

    private data class Entry(val signature: String, val context: CIFSContext)

    private val contexts = ConcurrentHashMap<String, Entry>()

    override fun get(host: String): CIFSContext? = contexts[host]?.context

    override fun getOrCreate(host: String, signature: String, factory: () -> CIFSContext): CIFSContext =
        contexts.compute(host) { _, existing ->
            if (existing != null && existing.signature == signature) existing else Entry(signature, factory())
        }!!.context
}
