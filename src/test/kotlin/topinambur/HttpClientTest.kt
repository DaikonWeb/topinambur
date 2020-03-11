package topinambur

import daikon.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.http.HttpStatus.*
import org.junit.jupiter.api.Test

class HttpClientTest {

    @Test
    fun `GET request`() {
        HttpServer(8080)
                .get("/") { req, res -> res.write(req.param("name")) }
                .start().use {
                    val response = "http://localhost:8080/".get(params = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `POST request`() {
        HttpServer(8080)
                .post("/") { req, res -> res.write(req.body()) }
                .start().use {
                    val response = "http://localhost:8080/".post(data = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("name=Bob")
                }
    }

    @Test
    fun `POST body`() {
        HttpServer(8080)
                .post("/") { req, res -> res.write(req.body()) }
                .start().use {
                    val response = "http://localhost:8080/".post(body = "Bob")
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `POST without body`() {
        HttpServer(8080)
                .post("/") { req, res -> res.write("post response") }
                .start().use {
                    val response = "http://localhost:8080/".post()
                    assertThat(response.body).isEqualTo("post response")
                }
    }

    @Test
    fun `request header`() {
        HttpServer(8080)
                .get("/") { req, res -> res.write(req.header("name")) }
                .start().use {
                    val response = "http://localhost:8080/".get(headers = mapOf("name" to "Bob"))
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `response without body`() {
        HttpServer(8080)
                .get("/") { _, res -> res.status(INTERNAL_SERVER_ERROR_500) }
                .start().use {
                    val response = "http://localhost:8080/".get(params = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(INTERNAL_SERVER_ERROR_500)
                    assertThat(response.body).isEqualTo("")
                }
    }

    @Test
    fun `follow redirect from HTTP to HTTPS`() {
        val response = "http://www.trovaprezzi.it/".get()

        assertThat(response.statusCode).isEqualTo(OK_200)
    }

    @Test
    fun `follow redirects`() {
        HttpServer(8080)
                .get("/bar") { _, res ->
                    res.redirect("/foo")
                }
                .get("/foo") { _, res ->
                    res.write("well done")
                }
                .start().use {
                    val response = "http://localhost:8080/bar".get()
                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("well done")
                }
    }

    @Test
    fun `do not follow redirects`() {
        HttpServer(8080)
                .get("/bar") { _, res ->
                    res.redirect("/foo")
                }
                .get("/foo") { _, res ->
                    res.write("well done")
                }
                .start().use {
                    val response = "http://localhost:8080/bar".get(followRedirects = false)
                    assertThat(response.statusCode).isEqualTo(MOVED_TEMPORARILY_302)
                }
    }

    @Test
    fun `DELETE request`() {
        HttpServer(8080)
                .delete("/") { req, res -> res.write("DELETED ${req.body()}") }
                .start().use {
                    val response = "http://localhost:8080/".delete(body = "Bob")

                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("DELETED Bob")
                }
    }
}
