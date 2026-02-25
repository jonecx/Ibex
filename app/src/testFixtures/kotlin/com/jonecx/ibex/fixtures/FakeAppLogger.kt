package com.jonecx.ibex.fixtures

import com.jonecx.ibex.logging.AppLogger

class FakeAppLogger : AppLogger {

    val logs = mutableListOf<Pair<String, String>>()
    var initialized = false

    override fun initialize() {
        initialized = true
    }

    override fun d(message: String) {
        logs.add("DEBUG" to message)
    }

    override fun i(message: String) {
        logs.add("INFO" to message)
    }

    override fun w(message: String) {
        logs.add("WARN" to message)
    }

    override fun e(message: String, throwable: Throwable?) {
        logs.add("ERROR" to message)
    }

    fun reset() {
        logs.clear()
        initialized = false
    }
}
