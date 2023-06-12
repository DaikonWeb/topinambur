package topinambur

import daikon.core.HttpStatus.OK_200
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ServerResponseTest {
    @Test
    fun `to String`() {
        val response = ServerResponse(OK_200, "text".toByteArray(), mapOf("1" to "2"))

        assertThat(response.toString()).isEqualTo("ServerResponse(statusCode=200, body=text, headers={1=2})")
    }

    @Test
    fun equality() {
        val response1 = ServerResponse(OK_200, "text".toByteArray(), mapOf("1" to "2"))
        val response2 = ServerResponse(OK_200, "text".toByteArray(), mapOf("1" to "2"))

        assertThat(response1).isEqualTo(response2)
    }

    @Test
    fun `hash code`() {
        val response = ServerResponse(OK_200, "text".toByteArray(), mapOf("1" to "2"))

        assertThat(response.hashCode()).isEqualTo(3748946)
    }
}