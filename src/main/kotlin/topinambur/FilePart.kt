package topinambur

import java.nio.charset.StandardCharsets

class FilePart(private val name: String, private val type: String, private val content: ByteArray): Part {
    override fun encode() = "; filename=\"${name}\"\r\nContent-Type: ${type}\r\n\r\n".toByteArray(StandardCharsets.UTF_8) +
        content + "\r\n".toByteArray(StandardCharsets.UTF_8)
}
