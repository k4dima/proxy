package com.appsflyer.proxy

import okhttp3.*
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLStreamHandler
import java.util.*
import javax.net.ssl.HttpsURLConnection

class Proxy {
    companion object : HashMap<String, Host>() {
        init {
            URL.setURLStreamHandlerFactory {
                object : URLStreamHandler() {
                    override fun openConnection(url: URL) = object : HttpsURLConnection(url) {
                        val clientBuilder = OkHttpClient.Builder()
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        private val responseLazy = lazy {
                            val host = url.host
                            Request.Builder()
                                .also { builder ->
                                    if (byteArrayOutputStream.size() != 0) byteArrayOutputStream.toByteArray()
                                        .let { RequestBody.create(MediaType.get(contentType), it) }
                                        .let { builder.post(it) }
                                }
                                .url(url)
                                .headers(headers.build())
                                .build()
                                .let { request ->
                                    get(host).mocks
                                        .poll()
                                        ?.request(request)
                                        ?.build()
                                        ?: clientBuilder.build()
                                            .newCall(request)
                                            .execute()
                                }
                                .also { if (containsKey(host)) get(host).subs.offer(it) }
                        }
                        val response by responseLazy
                        val headers = Headers.Builder()
                        override fun connect() {
                            response
                        }

                        override fun disconnect() = Unit
                        override fun usingProxy() = false
                        override fun getResponseCode() = response.code()
                        override fun getInputStream() = response.body()!!.byteStream()
                        override fun setRequestProperty(key: String, value: String) {
                            headers[key] = value
                        }

                        override fun addRequestProperty(key: String, value: String) {
                            headers.add(key, value)
                        }

                        override fun getOutputStream() = byteArrayOutputStream
                        override fun getHeaderField(name: String) =
                            (if (responseLazy.isInitialized()) response.headers() else headers.build())
                                .toMultimap()
                                .mapKeys { it.key.lowercase(Locale.getDefault()) }[name.lowercase()]
                                ?.last()

                        override fun getCipherSuite() = null
                        override fun getLocalCertificates() = null
                        override fun getServerCertificates() = null
                        override fun setInstanceFollowRedirects(followRedirects: Boolean) {
                            clientBuilder.followRedirects(followRedirects)
                                .followSslRedirects(followRedirects)
                        }

                        override fun getResponseMessage() = response.message()
                    }
                }
            }
        }

        override fun get(key: String) = super.get(key) ?: Host().also { put(key, it) }
    }
}