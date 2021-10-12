package com.appsflyer.proxy

import okhttp3.*
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

class Connection(url: URL) : HttpsURLConnection(url) {
    private val clientBuilder = OkHttpClient.Builder()
    private val byteArrayOutputStream = ByteArrayOutputStream()
    private val responseLazy = lazy {
        val host = url.host
        val requestBuilder = Request.Builder()
        if (byteArrayOutputStream.size() != 0)
            byteArrayOutputStream
                .toByteArray()
                .let { RequestBody.create(MediaType.get(contentType), it) }
                .let { requestBuilder.post(it) }
        val request = requestBuilder
            .url(url)
            .headers(headers.build())
            .build()
        val response = Proxy[host]
            .mocks
            .poll()
            ?.request(request)
            ?.build()
            ?: clientBuilder
                .build()
                .newCall(request)
                .execute()
        if (Proxy.containsKey(host)) Proxy[host].subs.offer(response)
        response
    }
    private val response: Response by responseLazy
    private val headers = Headers.Builder()
    override fun connect() {
        response
    }

    override fun disconnect() = Unit
    override fun usingProxy() = false
    override fun getResponseCode() = response.code()
    override fun getInputStream(): InputStream = response.body()!!.byteStream()
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
        clientBuilder
            .followRedirects(followRedirects)
            .followSslRedirects(followRedirects)
    }

    override fun getResponseMessage(): String = response.message()
}