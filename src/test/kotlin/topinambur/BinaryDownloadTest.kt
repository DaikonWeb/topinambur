package topinambur

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.jupiter.api.Test
import topinambur.Http.Companion.HTTP
import javax.servlet.MultipartConfigElement
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class BinaryDownloadTest {

    @Test
    fun `can download content as byte array`() {
        val byteArray = byteArrayOf(98, 99, 77)

        FileMirrorServer().respondWith(byteArray).start().use {
            val response = HTTP.post("http://localhost:8080/", body = "")

            assertThat(response.bytes).isEqualTo(byteArray)
        }
    }

    @Test
    fun `can upload a file as byte array`() {
        FileMirrorServer().start().use { server ->
            HTTP.post(
                url = "http://localhost:8080/",
                body = Multipart(
                    mapOf(
                        "file" to FilePart("a.txt", "plain/text", byteArrayOf(112, 124, 111, 54)),
                        "field" to FieldPart("value")
                    )
                )
            )

            assertThat(server.receivedFiles()).isEqualTo(
                listOf(
                    ReceivedFile("a.txt", "plain/text", byteArrayOf(112, 124, 111, 54)),
                    ReceivedFile("field", null, "value".toByteArray())
                )
            )
        }
    }
}

class FileMirrorServer(port: Int = 8080) : AutoCloseable {
    private val server = Server(port)
    private val servlet = FileMirrorServlet()

    fun start(): FileMirrorServer {
        val handler = ServletContextHandler()
        val servletHolder = ServletHolder(servlet)
        servletHolder.registration.setMultipartConfig(MultipartConfigElement("/tmp"))
        handler.addServlet(servletHolder, "/*")
        server.handler = handler
        server.start()
        return this
    }

    fun receivedFiles() = servlet.receivedFiles()

    override fun close() {
        server.stop()
    }

    fun respondWith(byteArray: ByteArray): FileMirrorServer {
        servlet.respondWith(byteArray)
        return this
    }
}

class FileMirrorServlet : HttpServlet() {

    private val receivedFiles = mutableListOf<ReceivedFile>()
    private var respondWith = byteArrayOf()

    override fun doPost(request: HttpServletRequest?, response: HttpServletResponse?) {
        try {
            request?.parts?.forEach {
                val file = it.inputStream.readAllBytes()
                receivedFiles.add(ReceivedFile(it.submittedFileName ?: it.name, it.contentType, file))
            }
        } catch (t: Throwable) {
        }

        response?.outputStream?.write(respondWith)
    }

    fun receivedFiles() = receivedFiles.toList()
    fun respondWith(byteArray: ByteArray) {
        respondWith = byteArray
    }

}

data class ReceivedFile(val name: String?, val type: String?, val content: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReceivedFile

        if (name != other.name) return false
        if (type != other.type) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }
}
