package topinambur

import java.io.InputStream
import java.io.PrintStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

class Http(
    private val baseUrl: String = "",
    log: PrintStream? = null,
    headers: Map<String, String> = emptyMap(),
    auth: AuthorizationStrategy = None(),
    followRedirects: Boolean = true
) {
    private val curl = Curl(log)
    private val defaultHeaders = mapOf("Accept" to "*/*", "User-Agent" to "daikonweb/topinambur")
    private val baseHeaders = headers
    private val baseAuth = auth
    private val baseFollowRedirects = followRedirects

    fun head(
        url: String = "",
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return call(url, "HEAD", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun options(
        url: String = "",
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return call(url, "OPTIONS", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun get(
        url: String = "",
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return call(url, "GET", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun post(
        url: String = "",
        data: Multipart,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        val headersWithType = headers + mapOf("Content-Type" to data.contentType)
        return call(url, "POST", emptyMap(), data.body(), headersWithType, auth, followRedirects, timeoutMillis)
    }

    fun post(
        url: String = "",
        data: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        val body = urlEncode(data)
        return call(url, "POST", emptyMap(), body.toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun post(
        url: String = "",
        body: String,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return call(url, "POST", emptyMap(), body.toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun put(
        url: String = "",
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return call(url, "PUT", emptyMap(), "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun put(
        url: String = "",
        data: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        val body = urlEncode(data)
        return call(url, "PUT", emptyMap(), body.toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun put(
        url: String = "",
        body: String,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return call(url, "PUT", emptyMap(), body.toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun delete(
        url: String = "",
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return call(url, "DELETE", emptyMap(), "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun delete(
        url: String = "",
        data: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        val body = urlEncode(data)
        return call(url, "DELETE", emptyMap(), body.toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun delete(
        url: String = "",
        body: String,
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return call(url, "DELETE", emptyMap(), body.toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun call(
        url: String = "",
        method: String = "GET",
        params: Map<String, String> = emptyMap(),
        data: ByteArray = "".toByteArray(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = baseFollowRedirects,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return doRequest(build(url), method, params, data, build(headers, auth), followRedirects, timeoutMillis)
    }

    private fun doRequest(
        url: String,
        method: String,
        params: Map<String, String>,
        data: ByteArray,
        headers: Map<String, String>,
        followRedirects: Boolean,
        timeoutMillis: Int
    ): ServerResponse {
        val urlWithParams = if (params.isEmpty()) url else "$url?${urlEncode(params)}"
        val response = prepareRequest(urlWithParams, method, headers, data, followRedirects, timeoutMillis)

        if (followRedirects && response.isRedirectToHttps()) {
            return doRequest(response.location(), method, params, data, headers, true, timeoutMillis)
        }

        val bytes = response.body()
        return ServerResponse(response.responseCode, bytes, response.headers())
    }

    private fun prepareRequest(
        url: String,
        method: String,
        headers: Map<String, String>,
        data: ByteArray,
        followRedirects: Boolean,
        timeoutMillis: Int
    ): HttpURLConnection {
        val normalizedMethod = method.toUpperCase()
        val encodedUrl = URI(url).toASCIIString()

        curl.print(encodedUrl, normalizedMethod, headers, data.toString(UTF_8), followRedirects)

        return (URL(encodedUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            headers.forEach { setRequestProperty(it.key, it.value) }
            requestMethod = normalizedMethod
            if (normalizedMethod.needsBody && data.isNotEmpty()) {
                doOutput = true
                outputStream.write(data)
            }
            doInput
            instanceFollowRedirects = followRedirects
        }
    }

    private fun HttpURLConnection.headers() =
        headerFields.map { entry -> (entry.key ?: "") to entry.value.first() }.toMap()

    private fun HttpURLConnection.location() = getHeaderField("Location")

    private fun HttpURLConnection.isRedirectToHttps(): Boolean {
        return location() != null &&
                location().startsWith("https", true) &&
                responseCode >= 300 &&
                responseCode <= 399
    }

    private fun HttpURLConnection.body(): ByteArray {
        return try {
            contentStream.use { it.readBytes() }
        } catch (t: Throwable) {
            ByteArray(0)
        }
    }

    private val HttpURLConnection.contentStream: InputStream
        get() {
            val stream = try {
                inputStream
            } catch (t: Throwable) {
                errorStream
            }

            return when (headerFields["Content-Encoding"]?.first()?.toLowerCase()) {
                "gzip" -> GZIPInputStream(stream)
                "deflate" -> InflaterInputStream(stream)
                else -> stream
            }
        }

    private fun build(url: String): String {
        return if (baseUrl.isEmpty()) url else "$baseUrl$url"
    }

    private fun build(headers: Map<String, String>, auth: AuthorizationStrategy): Map<String, String> {
        return defaultHeaders + baseHeaders + headers + build(auth).toHeader()
    }

    private fun build(auth: AuthorizationStrategy): AuthorizationStrategy {
        return if (auth is None) baseAuth else auth
    }

    private fun urlEncode(params: Map<String, String>): String {
        return params
            .map { "${it.key}=${URLEncoder.encode(it.value, UTF_8.name())}" }
            .joinToString("&")
    }

    companion object {
        val HTTP: Http = Http()
    }
}