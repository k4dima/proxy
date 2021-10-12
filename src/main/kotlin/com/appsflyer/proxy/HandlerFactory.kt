package com.appsflyer.proxy

import java.net.URLStreamHandlerFactory

class HandlerFactory : URLStreamHandlerFactory {
    override fun createURLStreamHandler(protocol: String?) = Handler()
}