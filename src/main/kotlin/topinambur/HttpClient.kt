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

class HttpClient(private val url: String, log: PrintStream? = null) {
    private val curl = Curl(log)
    private val defaultHeaders =
        mapOf("Accept" to "*/*", "Accept-Encoding" to "gzip, deflate", "User-Agent" to "daikonweb/topinambur")

    fun head(
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return call("HEAD", params, "", headers, followRedirects, timeoutMillis)
    }

    fun options(
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return call("OPTIONS", params, "", headers, followRedirects, timeoutMillis)
    }

    fun get(
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return call("GET", params, "", headers, followRedirects, timeoutMillis)
    }

    fun post(
        body: String = "",
        data: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return callWithBody("POST", body, data, headers, followRedirects, timeoutMillis)
    }

    fun put(
        body: String = "",
        data: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return callWithBody("PUT", body, data, headers, followRedirects, timeoutMillis)
    }

    fun delete(
        body: String = "",
        data: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        return callWithBody("DELETE", body, data, headers, followRedirects, timeoutMillis)
    }

    fun call(
        method: String = "GET",
        params: Map<String, String> = emptyMap(),
        data: String = "",
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true,
        timeoutMillis: Int = 30000
    ): ServerResponse {
        val allHeaders = defaultHeaders + headers
        val url = if (params.isEmpty()) url else "$url?${urlEncode(params)}"
        val response: HttpURLConnection = prepareRequest(url, method, allHeaders, data, followRedirects, timeoutMillis)

        if (followRedirects && response.isRedirectToHttps()) {
            return HttpClient(response.location()).call(
                method,
                params,
                data,
                allHeaders,
                followRedirects,
                timeoutMillis
            )
        }

        return ServerResponse(response.responseCode, response.body(), response.headers())
    }

    private fun callWithBody(
        method: String,
        body: String,
        data: Map<String, String>,
        headers: Map<String, String>,
        followRedirects: Boolean,
        timeoutMillis: Int
    ): ServerResponse {
        if (body.isNotEmpty() && data.isNotEmpty()) {
            throw RuntimeException("You can't specify both: data, body")
        }

        return call(
            method,
            emptyMap(),
            if (body.isNotEmpty()) body else urlEncode(data),
            headers,
            followRedirects,
            timeoutMillis
        )
    }

    private fun prepareRequest(
        url: String,
        method: String,
        headers: Map<String, String>,
        data: String,
        followRedirects: Boolean,
        timeoutMillis: Int
    ): HttpURLConnection {
        val normalizedMethod = method.toUpperCase()
        val encodedUrl = URI(url).toASCIIString()

        curl.print(encodedUrl, normalizedMethod, headers, data, followRedirects)

        return (URL(encodedUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            headers.forEach { setRequestProperty(it.key, it.value) }
            requestMethod = normalizedMethod
            if (normalizedMethod.needsBody) {
                doOutput = true
                outputStream.write(data.toByteArray())
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

    private fun HttpURLConnection.body(): String {
        return try {
            contentStream.use { it.readBytes() }.toString(UTF_8)
        } catch (t: Throwable) {
            ""
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

    private fun urlEncode(params: Map<String, String>): String {
        return params
            .map { "${it.key}=${URLEncoder.encode(it.value, UTF_8.name())}" }
            .joinToString("&")
    }
}

data class ServerResponse(val statusCode: Int, val body: String, val headers: Map<String, String>) {
    fun header(key: String): String {
        return headers[key] ?: error("Header '$key' not found")
    }
}