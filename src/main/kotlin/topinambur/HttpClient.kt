package topinambur

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

class HttpClient(private val url: String) {
    fun call(
        method: String = "GET",
        params: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        data: String = ""
    ): ServerResponse {
        val url = if (params.isEmpty()) url else "$url?${urlEncode(params)}"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            headers.forEach { setRequestProperty(it.key, it.value) }
            requestMethod = method
            if (data.isNotBlank()) {
                doOutput = true
                outputStream.write(data.toByteArray())
            }
        }

        return ServerResponse(connection.responseCode, body(connection))
    }

    fun get(params: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): ServerResponse {
        return call("GET", params, headers)
    }

    fun post(data: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): ServerResponse {
        return call("POST", emptyMap(), headers, urlEncode(data))
    }

    fun post(data: String = "", headers: Map<String, String> = emptyMap()): ServerResponse {
        return call("POST", emptyMap(), headers, data)
    }

    private fun urlEncode(params: Map<String, String>): String {
        return params
            .map { "${it.key}=${URLEncoder.encode(it.value, UTF_8.name())}" }
            .joinToString("&")
    }

    private fun body(connection: HttpURLConnection): String {
        return try {
            connection.inputStream.bufferedReader().readText()
        } catch (t: Throwable) {
            ""
        }
    }
}

fun String.call(
    method: String = "GET",
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    data: String = ""
): ServerResponse {
    return HttpClient(this).call(method, params, headers, data)
}

fun String.get(params: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): ServerResponse {
    return HttpClient(this).get(params, headers)
}

fun String.post(data: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): ServerResponse {
    return HttpClient(this).post(data, headers)
}

fun String.post(data: String = "", headers: Map<String, String> = emptyMap()): ServerResponse {
    return HttpClient(this).post(data, headers)
}

data class ServerResponse(val statusCode: Int, val body: String)