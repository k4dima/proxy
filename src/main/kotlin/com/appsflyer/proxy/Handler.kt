package com.appsflyer.proxy

import java.net.URL
import java.net.URLStreamHandler

class Handler : URLStreamHandler() {
    override fun openConnection(url: URL) = Connection(url)
}