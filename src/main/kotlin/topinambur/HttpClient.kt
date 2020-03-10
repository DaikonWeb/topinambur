package topinambur

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets.UTF_8

class HttpClient(private val url: String) {
    fun httpCall(
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

    fun getCall(params: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): ServerResponse {
        return httpCall("GET", params, headers)
    }

    fun postCall(data: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): ServerResponse {
        return httpCall("POST", emptyMap(), headers, urlEncode(data))
    }

    fun postCall(data: String = "", headers: Map<String, String> = emptyMap()): ServerResponse {
        return httpCall("POST", emptyMap(), headers, data)
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

fun String.httpCall(
    method: String = "GET",
    params: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    data: String = ""
): ServerResponse {
    return HttpClient(this).httpCall(method, params, headers, data)
}

fun String.getCall(params: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): ServerResponse {
    return HttpClient(this).getCall(params, headers)
}

fun String.postCall(data: Map<String, String> = emptyMap(), headers: Map<String, String> = emptyMap()): ServerResponse {
    return HttpClient(this).postCall(data, headers)
}

fun String.postCall(data: String = "", headers: Map<String, String> = emptyMap()): ServerResponse {
    return HttpClient(this).postCall(data, headers)
}

data class ServerResponse(val statusCode: Int, val body: String)