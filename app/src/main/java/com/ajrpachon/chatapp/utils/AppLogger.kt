package com.ajrpachon.chatapp.utils

object AppLogger {
    fun d(tag: String, message: String) { println("D/$tag: $message") }
    fun i(tag: String, message: String) { println("I/$tag: $message") }
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        println("W/$tag: $message${throwable?.let { " — ${it.message}" } ?: ""}")
    }
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        System.err.println("E/$tag: $message${throwable?.let { " — ${it.message}" } ?: ""}")
    }
}
