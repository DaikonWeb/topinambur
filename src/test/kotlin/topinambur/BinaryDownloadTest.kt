package topinambur

import daikon.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8

class BinaryDownloadTest {

    @Test
    fun `can download content as byte array`() {
        val byteArray = byteArrayOf(98, 99, 77)
        HttpServer(8080)
            .get("/") { _, res -> res.write(byteArray) }
            .start().use {
                val response = "http://localhost:8080/".http.get()
                assertThat(response.bytes).isEqualTo(byteArray)
            }
    }

    @Test
    fun `can upload file as byte array`() {
        val byteArray = "fileInBinaryFormat".toByteArray(UTF_8)
        var spiedBody = ""

        HttpServer(8080)
            .post("/") { req, _ -> spiedBody = req.body() }
            .start().use {
                "http://localhost:8080/".http.post(
                        data = mapOf("file" to FilePart("a.txt", "plain/text", byteArray))
                )

                assertThat(spiedBody).contains("Content-Disposition: form-data; name=\"file\"; filename=\"a.txt\"")
                assertThat(spiedBody).contains("fileInBinaryFormat")
            }
    }
}