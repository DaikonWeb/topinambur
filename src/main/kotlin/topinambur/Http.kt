package topinambur

import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

class Http @JvmOverloads constructor(
    private val baseUrl: String = "",
    headers: Map<String, String> = emptyMap(),
    auth: AuthorizationStrategy = None(),
    followRedirects: Boolean = true,
    timeoutMillis: Int = 30000,
    private val printer: Printer? = null
) {
    private val defaultHeaders = mapOf("Accept" to "*/*", "User-Agent" to "daikonweb/topinambur")
    private val baseHeaders = headers
    private val baseAuth = auth
    private val baseFollowRedirects = followRedirects
    private val baseTimeoutMillis = timeoutMillis

    @JvmOverloads
    fun head(
        url: String,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return call(url, "HEAD", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun options(
        url: String,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return call(url, "OPTIONS", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun get(
        url: String,
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return call(url, "GET", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun post(
        url: String,
        body: Multipart,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        val headersWithType = headers + mapOf("Content-Type" to body.contentType)
        return post(url, body.body(), headersWithType, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun post(
        url: String,
        body: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return post(url, urlEncode(body), headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return post(url, body.toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun post(
        url: String,
        body: ByteArray = "".toByteArray(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return call(url, "POST", emptyMap(), body, headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun put(
        url: String,
        body: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return put(url, urlEncode(body), headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun put(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return put(url, body.toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun put(
        url: String,
        body: ByteArray = "".toByteArray(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return call(url, "PUT", emptyMap(), body, headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun delete(
        url: String,
        body: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return delete(url, urlEncode(body), headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun delete(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return delete(url, body.toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun delete(
        url: String,
        body: ByteArray = "".toByteArray(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        return call(url, "DELETE", emptyMap(), body, headers, auth, followRedirects, timeoutMillis)
    }

    @JvmOverloads
    fun call(
        url: String,
        method: String,
        params: Map<String, String> = emptyMap(),
        data: ByteArray = "".toByteArray(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = baseTimeoutMillis
    ): ServerResponse {
        val encodedUrl = buildUrl(url, params)
        val normalizedMethod = method.uppercase()
        val allHeaders = buildHeaders(headers, auth)
        printer?.print(encodedUrl, normalizedMethod, allHeaders, data.toString(UTF_8), followRedirects, timeoutMillis)
        return Request.call(encodedUrl, normalizedMethod, data, allHeaders, followRedirects, timeoutMillis)
    }

    private fun buildUrl(url: String, params: Map<String, String>): String {
        val fullUrl = if (baseUrl.isEmpty()) url else "${baseUrl.trimEnd('/')}/${url.trimStart('/')}"
        val queryString = URL(fullUrl).query ?: ""
        val fullUrlNoParams = fullUrl.replace("?$queryString", "")
        val encodedParams = encodeQuery(decodeQuery(queryString) + params)
        return "$fullUrlNoParams$encodedParams"
    }

    private fun buildHeaders(headers: Map<String, String>, auth: AuthorizationStrategy): Map<String, String> {
        return defaultHeaders + baseHeaders + headers + buildAuth(auth).toHeader()
    }

    private fun buildAuth(auth: AuthorizationStrategy): AuthorizationStrategy {
        return if (auth is None) baseAuth else auth
    }

    private fun decodeQuery(query: String): Map<String, String> {
        return query.split('&').mapNotNull {
            val parts = it.split('=')
            val name = parts.first()
            val value = parts.drop(1).firstOrNull() ?: ""
            if (name == "") null else Pair(name, value)
        }.toMap()
    }

    private fun encodeQuery(params: Map<String, String>): String {
        return if (params.isNotEmpty()) "?${urlEncode(params)}" else ""
    }

    private fun urlEncode(params: Map<String, String>): String {
        return params
            .map { "${it.key}=${URLEncoder.encode(it.value, UTF_8.name())}" }
            .joinToString("&")
    }

    companion object {
        @JvmField
        val HTTP: Http = Http()
    }
}