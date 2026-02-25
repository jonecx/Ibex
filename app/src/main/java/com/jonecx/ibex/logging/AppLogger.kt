package com.jonecx.ibex.logging

interface AppLogger {
    fun initialize()
    fun d(message: String)
    fun i(message: String)
    fun w(message: String)
    fun e(message: String, throwable: Throwable? = null)
}
