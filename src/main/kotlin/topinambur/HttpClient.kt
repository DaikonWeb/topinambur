package topinambur

import java.io.PrintStream
import java.lang.RuntimeException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

class HttpClient(private val url: String, log: PrintStream? = null) {
    private val curl = Curl(log)
    private val defaultHeaders = mapOf("Accept" to "*/*", "Accept-Encoding" to "gzip, deflate", "User-Agent" to "daikonweb/topinambur")

    fun head(params: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap(), followRedirects: Boolean = true): ServerResponse {
        return call("HEAD", params, "", headers, followRedirects)
    }

    fun get(params: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap(), followRedirects: Boolean = true): ServerResponse {
        return call("GET", params, "", headers, followRedirects)
    }

    fun post(body: String = "", data: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap(), followRedirects: Boolean = true): ServerResponse {
        return callWithBody("POST", body, data, headers, followRedirects)
    }

    fun delete(body: String = "", data: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap(), followRedirects: Boolean = true): ServerResponse {
        return callWithBody("DELETE", body, data, headers, followRedirects)
    }

    fun call(
            method: String = "GET",
            params: Map<String, String> = emptyMap(),
            data: String = "",
            headers: Map<String, String> = emptyMap(),
            followRedirects: Boolean = true
    ): ServerResponse {
        val allHeaders = defaultHeaders + headers
        val url = if (params.isEmpty()) url else "$url?${urlEncode(params)}"
        val response = prepareRequest(url, method, allHeaders, data, followRedirects)

        if (followRedirects && response.isRedirectToHttps()) {
            return HttpClient(response.location()).call(method, params, data, allHeaders, followRedirects)
        }

        return ServerResponse(response.responseCode, response.body())
    }

    private fun callWithBody(
            method: String,
            body: String,
            data: Map<String, String>,
            headers: Map<String, String>,
            followRedirects: Boolean
    ): ServerResponse {
        if (body.isNotEmpty() && data.isNotEmpty()) {
            throw RuntimeException("You can't specify both: data, body")
        }

        return call(method, emptyMap(), if (body.isNotEmpty()) body else urlEncode(data), headers, followRedirects)
    }

    private fun prepareRequest(
            url: String,
            method: String,
            headers: Map<String, String>,
            data: String,
            followRedirects: Boolean
    ): HttpURLConnection {
        val normalizedMethod = method.toUpperCase()

        curl.print(url, normalizedMethod, headers, data, followRedirects)

        return (URL(url).openConnection() as HttpURLConnection).apply {
            headers.forEach { setRequestProperty(it.key, it.value) }
            requestMethod = normalizedMethod
            if (normalizedMethod.needsBody) {
                doOutput = true
                outputStream.write(data.toByteArray())
            }
            instanceFollowRedirects = followRedirects
        }
    }

    private fun HttpURLConnection.location() = getHeaderField("Location")

    private fun HttpURLConnection.isRedirectToHttps(): Boolean {
        return location() != null &&
                location().startsWith("https", true) &&
                responseCode >= 300 &&
                responseCode <= 399
    }

    private fun HttpURLConnection.body(): String {
        return try {
            inputStream.bufferedReader().readText()
        } catch (t: Throwable) {
            ""
        }
    }

    private fun urlEncode(params: Map<String, String>): String {
        return params
                .map { "${it.key}=${URLEncoder.encode(it.value, UTF_8.name())}" }
                .joinToString("&")
    }
}

data class ServerResponse(val statusCode: Int, val body: String)