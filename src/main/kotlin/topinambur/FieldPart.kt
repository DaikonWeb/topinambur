package topinambur

import java.nio.charset.StandardCharsets

class FieldPart(private val field: String, private val value: String): Part {
    override fun encode() = encodeName(field) + "\r\n\r\n${value}\r\n".toByteArray(StandardCharsets.UTF_8)

    private fun encodeName(key: String): ByteArray {
        return "\r\nContent-Disposition: form-data; name=\"$key\"".toByteArray(StandardCharsets.UTF_8)
    }
}