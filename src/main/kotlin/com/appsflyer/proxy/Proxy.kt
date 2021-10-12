package com.appsflyer.proxy

import java.net.URL
import java.util.*

class Proxy {
    companion object : HashMap<String, Host>() {
        init {
            val handlerFactory = HandlerFactory()
            URL.setURLStreamHandlerFactory(handlerFactory)
        }

        override fun get(key: String) = super.get(key) ?: Host().also { put(key, it) }
    }
}