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


class HttpClient(private val baseUrl: String = "", log: PrintStream? = null) {
    private val curl = Curl(log)
    private val defaultHeaders = mapOf("Accept" to "*/*", "User-Agent" to "daikonweb/topinambur")

    fun head(
            url: String = "",
            params: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return call(url, "HEAD", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun options(
            url: String = "",
            params: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return call(url, "OPTIONS", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun get(
            url: String = "",
            params: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return call(url, "GET", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun post(
            url: String = "",
            body: String = "",
            data: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return callWithBody(url, "POST", body.toByteArray(), data, headers, auth, followRedirects, timeoutMillis)
    }

    fun post(
            url: String = "",
            data: Map<String, Part>,
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        val formData = MultipartFormData(data)
        return callWithBody(url, "POST", formData.body(), emptyMap(), headers + formData.contentTypeHeader(), auth, followRedirects, timeoutMillis)
    }

    fun put(
            url: String = "",
            body: String = "",
            data: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return callWithBody(url, "PUT", body.toByteArray(), data, headers, auth, followRedirects, timeoutMillis)
    }

    fun delete(
            url: String = "",
            body: String = "",
            data: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return callWithBody(url, "DELETE", body.toByteArray(), data, headers, auth, followRedirects, timeoutMillis)
    }

    fun call(
            url: String = "",
            method: String = "GET",
            params: Map<String, String> = emptyMap(),
            data: ByteArray = byteArrayOf(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return doRequest(build(url), method, params, data, headers, auth, followRedirects, timeoutMillis)
    }

    private fun callWithBody(
        url: String = "",
        method: String,
        body: ByteArray,
        data: Map<String, String>,
        headers: Map<String, String>,
        auth: AuthorizationStrategy,
        followRedirects: Boolean,
        timeoutMillis: Int
    ): ServerResponse {
        if (body.isNotEmpty() && data.isNotEmpty()) {
            throw RuntimeException("You can't specify both: data, body")
        }

        return call(
                url,
                method,
                emptyMap(),
                if (body.isNotEmpty()) body else (urlEncode(data)).toByteArray(),
                headers,
                auth,
                followRedirects,
                timeoutMillis
        )
    }

    private fun doRequest(
        url: String = "",
        method: String = "GET",
        params: Map<String, String> = emptyMap(),
        data: ByteArray = byteArrayOf(),
        headers: Map<String, String> = emptyMap(),
        auth: AuthorizationStrategy = None(),
        followRedirects: Boolean = true,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        val allHeaders = defaultHeaders + headers + auth.toHeader()
        val url = if (params.isEmpty()) url else "$url?${urlEncode(params)}"
        val response: HttpURLConnection = prepareRequest(url, method, allHeaders, data, followRedirects, timeoutMillis)

        if (followRedirects && response.isRedirectToHttps()) {
            return doRequest(response.location(),
                method,
                params,
                data,
                allHeaders,
                auth,
                true,
                timeoutMillis
            )
        }

        val bytes = response.body()
        return ServerResponse(response.responseCode, bytes.toString(UTF_8), bytes, response.headers())
    }

    private fun prepareRequest(
        url: String = "",
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
            if (normalizedMethod.needsBody) {
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
            val stream = try { inputStream } catch (t: Throwable) { errorStream }

            return when (headerFields["Content-Encoding"]?.first()?.toLowerCase()) {
                "gzip" -> GZIPInputStream(stream)
                "deflate" -> InflaterInputStream(stream)
                else -> stream
            }
        }

    private fun build(url: String): String {
        return if (baseUrl.isEmpty()) url else "$baseUrl$url"
    }

    private fun urlEncode(params: Map<String, String>): String {
        return params
            .map { "${it.key}=${URLEncoder.encode(it.value, UTF_8.name())}" }
            .joinToString("&")
    }
}