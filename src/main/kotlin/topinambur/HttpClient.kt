package topinambur

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

class HttpClient(private val url: String) {

    fun get(
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true
    ): ServerResponse {
        return call("GET", params, headers, followRedirects = followRedirects)
    }

    fun post(
        data: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true
    ): ServerResponse {
        return call("POST", emptyMap(), headers, urlEncode(data), followRedirects)
    }

    fun post(
        data: String = "",
        headers: Map<String, String> = emptyMap(),
        followRedirects: Boolean = true
    ): ServerResponse {
        return call("POST", emptyMap(), headers, data, followRedirects)
    }

    fun delete(
            data: Map<String, String>,
            headers: Map<String, String>,
            followRedirects: Boolean
    ): ServerResponse {
        return call("DELETE", emptyMap(), headers, urlEncode(data), followRedirects)
    }

    fun delete(
        data: String = "",
        headers: Map<String, String>,
        followRedirects: Boolean
    ): ServerResponse {
        return call("DELETE", emptyMap(), headers, data, followRedirects)
    }

    fun call(
        method: String = "GET",
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        data: String = "",
        followRedirects: Boolean = true
    ): ServerResponse {
        val url = if (params.isEmpty()) url else "$url?${urlEncode(params)}"
        val response = prepareRequest(url, method, headers, data, followRedirects)

        if (followRedirects && response.isRedirectToHttps()) {
            return HttpClient(response.location()).call(method, params, headers, data, followRedirects)
        }

        return ServerResponse(response.responseCode, response.body())
    }

    private fun prepareRequest(
        url: String,
        method: String,
        headers: Map<String, String>,
        data: String,
        followRedirects: Boolean
    ): HttpURLConnection {
        return (URL(url).openConnection() as HttpURLConnection).apply {
            headers.forEach { setRequestProperty(it.key, it.value) }
            requestMethod = method
            if (data.isNotBlank()) {
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

fun String.call(
    method: String = "GET",
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    data: String = "",
    followRedirects: Boolean = true
): ServerResponse {
    return HttpClient(this).call(method, params, headers, data, followRedirects = followRedirects)
}

fun String.get(
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    followRedirects: Boolean = true
): ServerResponse {
    return HttpClient(this).get(params, headers, followRedirects = followRedirects)
}

fun String.post(
    data: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    followRedirects: Boolean = true
): ServerResponse {
    return HttpClient(this).post(data, headers, followRedirects = followRedirects)
}

fun String.post(
    data: String = "",
    headers: Map<String, String> = emptyMap(),
    followRedirects: Boolean = true
): ServerResponse {
    return HttpClient(this).post(data, headers, followRedirects = followRedirects)
}

fun String.delete(
    data: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    followRedirects: Boolean = true
): ServerResponse {
    return HttpClient(this).delete(data, headers, followRedirects = followRedirects)
}

fun String.delete(
    data: String = "",
    headers: Map<String, String> = emptyMap(),
    followRedirects: Boolean = true
): ServerResponse {
    return HttpClient(this).delete(data, headers, followRedirects = followRedirects)
}

data class ServerResponse(val statusCode: Int, val body: String)