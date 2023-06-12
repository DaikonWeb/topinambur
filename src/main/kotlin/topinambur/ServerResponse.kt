package topinambur

import java.nio.charset.StandardCharsets

class ServerResponse(val statusCode: Int, val bytes: ByteArray, val headers: Map<String, String>) {
    val body: String by lazy { bytes.toString(StandardCharsets.UTF_8) }

    fun header(key: String): String {
        return headers[key] ?: error("Header '$key' not found")
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName}(statusCode=$statusCode, body=$body, headers=$headers)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ServerResponse

        if (statusCode != other.statusCode) return false
        if (headers != other.headers) return false
        if (body != other.body) return false

        return true
    }

    override fun hashCode(): Int {
        var result = statusCode
        result = 31 * result + headers.hashCode()
        result = 31 * result + body.hashCode()
        return result
    }
}