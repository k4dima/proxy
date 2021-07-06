package com.appsflyer.proxy

import okhttp3.Response
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class Host {
    internal val subs = LinkedBlockingQueue<Response>()
    internal val mocks = LinkedList<Response.Builder>()
    fun poll(timeout: Long, unit: TimeUnit): Response? = subs.poll(timeout, unit)
    fun offer(response: Response.Builder) = mocks.offer(response)
}