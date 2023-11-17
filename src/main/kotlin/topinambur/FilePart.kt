package topinambur

import java.nio.charset.StandardCharsets

class FilePart(private val field: String, private val name: String, private val type: String, private val content: ByteArray): Part {
    override fun encode() = encodeName(field) + "; filename=\"${name}\"\r\nContent-Type: ${type}\r\n\r\n".toByteArray(StandardCharsets.UTF_8) +
        content + "\r\n".toByteArray(StandardCharsets.UTF_8)

    private fun encodeName(key: String): ByteArray {
        return "\r\nContent-Disposition: form-data; name=\"$key\"".toByteArray(StandardCharsets.UTF_8)
    }
}
