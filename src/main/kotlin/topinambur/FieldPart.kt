package topinambur

import java.nio.charset.StandardCharsets

class FieldPart(private val value: String): Part {
    override fun encode() = "\r\n\r\n${value}\r\n".toByteArray(StandardCharsets.UTF_8)
}