package topinambur

import daikon.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.http.HttpStatus.INTERNAL_SERVER_ERROR_500
import org.eclipse.jetty.http.HttpStatus.OK_200
import org.junit.jupiter.api.Test

class HttpClientTest {

    @Test
    fun `GET request`() {
        HttpServer(8080)
            .get("/") { req, res -> res.write(req.param("name")) }
            .start().use {
                val response = "http://localhost:8080/".getCall(params = mapOf("name" to "Bob"))
                assertThat(response.statusCode).isEqualTo(OK_200)
                assertThat(response.body).isEqualTo("Bob")
            }
    }

    @Test
    fun `POST request`() {
        HttpServer(8080)
            .post("/") { req, res -> res.write(req.body()) }
            .start().use {
                val response = "http://localhost:8080/".postCall(data = mapOf("name" to "Bob"))
                assertThat(response.statusCode).isEqualTo(OK_200)
                assertThat(response.body).isEqualTo("name=Bob")
            }
    }

    @Test
    fun `POST data`() {
        HttpServer(8080)
            .post("/") { req, res -> res.write(req.body()) }
            .start().use {
                val response = "http://localhost:8080/".postCall(data = "Bob")
                assertThat(response.body).isEqualTo("Bob")
            }
    }

    @Test
    fun `request header`() {
        HttpServer(8080)
            .get("/") { req, res -> res.write(req.header("name")) }
            .start().use {
                val response = "http://localhost:8080/".getCall(headers = mapOf("name" to "Bob"))
                assertThat(response.body).isEqualTo("Bob")
            }
    }

    @Test
    fun `response without body`() {
        HttpServer(8080)
            .get("/") { _, res -> res.status(INTERNAL_SERVER_ERROR_500) }
            .start().use {
                val response = "http://localhost:8080/".getCall(params = mapOf("name" to "Bob"))
                assertThat(response.statusCode).isEqualTo(INTERNAL_SERVER_ERROR_500)
                assertThat(response.body).isEqualTo("")
            }
    }
}