package topinambur

import java.nio.charset.StandardCharsets.UTF_8

class Multipart(private vararg val parts: Part) {
    private val boundaryString = "--------------------------1b0caa1adf4aaa9f"
    private val boundary = boundaryString.toByteArray(UTF_8)
    val contentType = "multipart/form-data; boundary=${boundaryString.substring(2)}"

    fun body() = boundary + fields() + "--".toByteArray(UTF_8)

    private fun fields() = parts.fold(byteArrayOf()) { acc, current -> acc + current.encode() + boundary }

}
