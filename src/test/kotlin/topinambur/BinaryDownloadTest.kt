package topinambur

import daikon.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

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
}