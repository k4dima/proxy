package com.appsflyer.proxy

import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLStreamHandler
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import javax.net.ssl.HttpsURLConnection

class Proxy {
    companion object : HashMap<String, LinkedBlockingQueue<Pair<Request, Response>>>() {
        @JvmStatic
        val subs = mutableMapOf<String, (Request, Response) -> Unit>()
        val mocks = mutableMapOf<String, (Request) -> Response>()

        init {
            URL.setURLStreamHandlerFactory {
                object : URLStreamHandler() {
                    override fun openConnection(url: URL) = object : HttpsURLConnection(url) {
                        val clientBuilder = OkHttpClient.Builder()
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        val response by lazy {
                            Request.Builder()
                                .also {
                                    if (byteArrayOutputStream.size() != 0) it.post(
                                        byteArrayOutputStream.toByteArray()
                                            .toRequestBody(contentType = contentType.toMediaType())
                                    )
                                }
                                .url(url)
                                .headers(headers.build())
                                .build()
                                .let { request ->
                                    mocks[url.host]?.invoke(request) ?: clientBuilder.build()
                                        .newCall(request)
                                        .execute()
                                }
                                .also { subs[url.host]?.invoke(it.request, it) }
                        }
                        val headers = Headers.Builder()
                        override fun connect() {
                            response
                        }

                        override fun disconnect() = Unit
                        override fun usingProxy() = false
                        override fun getResponseCode() = response.code
                        override fun getInputStream() = response.body!!.byteStream()
                        override fun setRequestProperty(key: String, value: String) {
                            headers[key] = value
                        }

                        override fun addRequestProperty(key: String, value: String) {
                            headers.add(key, value)
                        }

                        override fun getOutputStream() = byteArrayOutputStream
                        override fun getHeaderField(name: String) = headers[name]
                            ?: headers.build()
                                .toMultimap()
                                .mapKeys { it.key.toLowerCase(Locale.getDefault()) }[name]
                                ?.first()

                        override fun getCipherSuite() = null
                        override fun getLocalCertificates() = null
                        override fun getServerCertificates() = null
                        override fun setInstanceFollowRedirects(followRedirects: Boolean) {
                            clientBuilder.followRedirects(followRedirects)
                                .followSslRedirects(followRedirects)
                        }

                        override fun getResponseMessage() = response.message
                    }
                }
            }
        }

        override fun get(key: String) = super.get(key) ?: LinkedBlockingQueue<Pair<Request, Response>>()
            .apply {
                subs[key] = { request, response -> offer(Pair(request, response)) }
                put(key, this)
            }
    }
}