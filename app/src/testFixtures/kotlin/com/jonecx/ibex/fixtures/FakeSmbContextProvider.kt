package com.jonecx.ibex.fixtures

import com.jonecx.ibex.data.repository.SmbContextProviderContract
import jcifs.CIFSContext

class FakeSmbContextProvider : SmbContextProviderContract {

    private val contexts = mutableMapOf<String, CIFSContext>()

    override fun register(host: String, context: CIFSContext) {
        contexts[host] = context
    }

    override fun get(host: String): CIFSContext? = contexts[host]
}
