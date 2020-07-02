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

interface Part {
    fun encode(): ByteArray
}

class FilePart(private val name: String, private val type: String, private val content: ByteArray): Part {
    override fun encode() = "; filename=\"${name}\"\r\nContent-Type: ${type}\r\n\r\n".toByteArray(UTF_8) +
        content + "\r\n\r\n".toByteArray(UTF_8)
}

class FieldPart(private val value: String): Part {
    override fun encode() = "\r\n\r\n${value}\r\n".toByteArray(UTF_8)
}

class MultipartFormData(private val data: Map<String, Part>) {
    private val boundaryString = "--------------------------1b0caa1adf4aaa9f"
    private val boundary = boundaryString.toByteArray(UTF_8)
    private val type = "multipart/form-data; boundary=${boundaryString.substring(2)}"

    fun contentTypeHeader(): Map<String, String> = mapOf("Content-Type" to type)

    fun body() = boundary +
            data.entries.fold(byteArrayOf()) { acc, current -> acc + encodeKey(current.key) + current.value.encode() + boundary } +
            "--".toByteArray(UTF_8)

    private fun encodeKey(key: String): ByteArray {
        return "\r\nContent-Disposition: form-data; name=\"$key\"".toByteArray(UTF_8)
    }
}


class HttpClient(private val url: String, log: PrintStream? = null) {
    private val curl = Curl(log)
    private val defaultHeaders = mapOf("Accept" to "*/*", "User-Agent" to "daikonweb/topinambur")

    fun head(
            params: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return call("HEAD", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun options(
            params: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return call("OPTIONS", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun get(
            params: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return call("GET", params, "".toByteArray(), headers, auth, followRedirects, timeoutMillis)
    }

    fun post(
            body: String = "",
            data: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return callWithBody("POST", body.toByteArray(), data, headers, auth, followRedirects, timeoutMillis)
    }

    fun post(
            data: Map<String, Part>,
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        val formData = MultipartFormData(data)
        return callWithBody("POST", formData.body(), emptyMap(), headers + formData.contentTypeHeader(), auth, followRedirects, timeoutMillis)
    }

    fun put(
            body: String = "",
            data: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return callWithBody("PUT", body.toByteArray(), data, headers, auth, followRedirects, timeoutMillis)
    }

    fun delete(
            body: String = "",
            data: Map<String, String> = emptyMap(),
            headers: Map<String, String> = emptyMap(),
            auth: AuthorizationStrategy = None(),
            followRedirects: Boolean = true,
            timeoutMillis: Int = 30000
    ): ServerResponse {
        return callWithBody("DELETE", body.toByteArray(), data, headers, auth, followRedirects, timeoutMillis)
    }

    fun call(
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
            return HttpClient(response.location()).call(
                    method,
                    params,
                    data,
                    allHeaders,
                    auth,
                    followRedirects,
                    timeoutMillis
            )
        }

        val bytes = response.body()
        return ServerResponse(response.responseCode, bytes.toString(UTF_8), bytes, response.headers())
    }

    private fun callWithBody(
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
                method,
                emptyMap(),
                if (body.isNotEmpty()) body else (urlEncode(data)).toByteArray(),
                headers,
                auth,
                followRedirects,
                timeoutMillis
        )
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

    private fun urlEncode(params: Map<String, String>): String {
        return params
            .map { "${it.key}=${URLEncoder.encode(it.value, UTF_8.name())}" }
            .joinToString("&")
    }
}