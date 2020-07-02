package topinambur

import java.nio.charset.StandardCharsets.UTF_8
import kotlin.collections.Map.Entry

class MultipartFormData(private val data: Map<String, Part>) {
    private val boundaryString = "--------------------------1b0caa1adf4aaa9f"
    private val boundary = boundaryString.toByteArray(UTF_8)
    private val type = "multipart/form-data; boundary=${boundaryString.substring(2)}"

    fun contentTypeHeader(): Map<String, String> = mapOf("Content-Type" to type)

    fun body() = boundary + fields() + "--".toByteArray(UTF_8)

    private fun fields() = data.entries.fold(byteArrayOf()) { acc, current -> acc + field(current) }

    private fun field(current: Entry<String, Part>): ByteArray {
        return encodeName(current.key) + current.value.encode() + boundary
    }

    private fun encodeName(key: String): ByteArray {
        return "\r\nContent-Disposition: form-data; name=\"$key\"".toByteArray(UTF_8)
    }
}
