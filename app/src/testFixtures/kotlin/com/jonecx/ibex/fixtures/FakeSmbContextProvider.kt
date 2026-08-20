package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.repository.SmbContextProviderContract
import jcifs.CIFSContext

class FakeSmbContextProvider : SmbContextProviderContract {

    private data class Entry(val signature: String, val context: CIFSContext)

    private val contexts = mutableMapOf<String, Entry>()

    // Counts how often factory actually ran, so tests can assert contexts are reused, not rebuilt.
    var buildCount = 0
        private set

    override fun get(host: String): CIFSContext? = contexts[host]?.context

    override fun getOrCreate(host: String, signature: String, factory: () -> CIFSContext): CIFSContext {
        val existing = contexts[host]
        if (existing != null && existing.signature == signature) return existing.context
        val created = factory()
        buildCount++
        contexts[host] = Entry(signature, created)
        return created
    }
}
