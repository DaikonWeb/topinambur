package topinambur

import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

internal object Request {
    fun call(
        url: String,
        method: String,
        data: ByteArray,
        headers: Map<String, String>,
        followRedirects: Boolean,
        timeoutMillis: Int
    ): ServerResponse {
        val response = makeRequest(URI(url).toASCIIString(), method, headers, data, followRedirects, timeoutMillis)

        if (followRedirects && response.isRedirectToHttps()) {
            return call(response.location(), method, data, headers, true, timeoutMillis)
        }

        val bytes = response.body()
        return ServerResponse(response.responseCode, bytes, response.headers())
    }

    private fun makeRequest(
        url: String,
        method: String,
        headers: Map<String, String>,
        data: ByteArray,
        followRedirects: Boolean,
        timeoutMillis: Int
    ): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = timeoutMillis
            readTimeout = timeoutMillis
            headers.forEach { setRequestProperty(it.key, it.value) }
            requestMethod = method
            if (method.needsBody && data.isNotEmpty()) {
                doOutput = true
                outputStream.write(data)
            }
            doInput
            instanceFollowRedirects = followRedirects
        }
    }

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

            return when (headerFields["Content-Encoding"]?.first()?.lowercase()) {
                "gzip" -> GZIPInputStream(stream)
                "deflate" -> InflaterInputStream(stream)
                else -> stream
            }
        }

    private fun HttpURLConnection.headers() =
        headerFields.map { entry -> (entry.key ?: "") to entry.value.first() }.toMap()

    private fun HttpURLConnection.location() = getHeaderField("Location")
}