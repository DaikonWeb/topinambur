package topinambur

import daikon.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.http.HttpStatus.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.SocketTimeoutException
import java.net.URLEncoder.encode
import java.nio.charset.StandardCharsets.UTF_8

class HttpClientTest {

    @Test
    fun `GET request`() {
        HttpServer(8080)
                .get("/") { req, res -> res.write(req.param("name")) }
                .start().use {
                    val response = "http://localhost:8080/".http.get(params = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `POST request`() {
        HttpServer(8080)
                .post("/") { req, res -> res.write(req.body()) }
                .start().use {
                    val response = "http://localhost:8080/".http.post(data = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("name=Bob")
                }
    }

    @Test
    fun `POST body`() {
        HttpServer(8080)
                .post("/") { req, res -> res.write(req.body()) }
                .start().use {
                    val response = "http://localhost:8080/".http.post(body = "Bob")
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `POST without body`() {
        HttpServer(8080)
                .post("/") { _, res -> res.write("post response") }
                .start().use {
                    val response = "http://localhost:8080/".http.post()
                    assertThat(response.body).isEqualTo("post response")
                }
    }

    @Test
    fun `request header`() {
        HttpServer(8080)
                .get("/") { req, res -> res.write(req.header("name")) }
                .start().use {
                    val response = "http://localhost:8080/".http.get(headers = mapOf("name" to "Bob"))
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `response without body`() {
        HttpServer(8080)
                .get("/") { _, res -> res.status(INTERNAL_SERVER_ERROR_500) }
                .start().use {
                    val response = "http://localhost:8080/".http.get(params = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(INTERNAL_SERVER_ERROR_500)
                    assertThat(response.body).isEqualTo("")
                }
    }

    @Test
    fun `follow redirect from HTTP to HTTPS`() {
        val response = "http://www.trovaprezzi.it/".http.get()

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
                    val response = "http://localhost:8080/bar".http.get()
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
                    val response = "http://localhost:8080/bar".http.get(followRedirects = false)
                    assertThat(response.statusCode).isEqualTo(MOVED_TEMPORARILY_302)
                }
    }

    @Test
    fun `DELETE request`() {
        HttpServer(8080)
                .delete("/") { _, res -> res.write("DELETED") }
                .start().use {
                    val response = "http://localhost:8080/".http.delete()

                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("DELETED")
                }
    }

    @Test
    fun `HEAD request`() {
        HttpServer(8080)
                .head("/") { req, res -> res.write(req.param("name")) }
                .start().use {
                    val response = "http://localhost:8080/".http.head(params = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("")
                }
    }

    @Test
    fun `PUT request`() {
        HttpServer(8080)
                .put("/") { req, res -> res.write(req.body()) }
                .start().use {
                    val response = "http://localhost:8080/".http.put(body = "Bob")
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `OPTIONS request`() {
        HttpServer(8080)
                .options("/") { _, _ -> }
                .start().use {
                    val response = "http://localhost:8080/".http.options(params = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("")
                }
    }

    @Test
    fun `call with get request`() {
        HttpServer(8080)
                .get("/") { req, res -> res.write(req.param("name")) }
                .start().use {
                    val response = "http://localhost:8080/".http.call(method = "get", params = mapOf("name" to "Bob"))
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `default headers`() {
        HttpServer(8080)
                .get("/") { req, res ->
                    res.write("${req.header("Accept")}|${req.header("User-Agent")}")
                }
                .start().use {
                    val response = "http://localhost:8080/".http.get()

                    assertThat(response.body).isEqualTo("*/*|daikonweb/topinambur")
                }
    }

    @Test
    fun `overrides default headers`() {
        HttpServer(8080)
                .get("/") { req, res ->
                    res.write("${req.header("Accept")}|${req.header("User-Agent")}")
                }
                .start().use {
                    val response = "http://localhost:8080/".http.get(headers = mapOf("User-Agent" to "Bob"))

                    assertThat(response.body).isEqualTo("*/*|Bob")
                }
    }

    @Test
    fun `responds with headers`() {
        HttpServer(8080)
            .options("/") { _, res ->
                res.header("Allow", "OPTIONS, GET, POST")
            }
            .start().use {
                val response = "http://localhost:8080/".http.options()

                assertThat(response.headers).containsEntry("Allow", "OPTIONS, GET, POST")
            }
    }

    @Test
    fun `response header`() {
        HttpServer(8080)
            .options("/") { _, res ->
                res.header("Allow", "OPTIONS, GET, POST")
            }
            .start().use {
                val response = "http://localhost:8080/".http.options()

                assertThat(response.header("Allow")).isEqualTo("OPTIONS, GET, POST")
            }
    }

    @Test
    fun `response header not found`() {
        HttpServer(8080)
            .options("/") { _, _ -> }
            .start().use {
                val response = "http://localhost:8080/".http.options()

                assertThrows<IllegalStateException> { response.header("Allow") }
            }
    }

    @Test
    fun `response header for empty key`() {
        HttpServer(8080)
                .options("/") { _, _ -> }
                .start().use {
                    val response = "http://localhost:8080/".http.options()

                    assertThat(response.header("")).isEqualTo("HTTP/1.1 200 OK")
                }
    }

    @Test
    fun `response body on 4xx status codes`() {
        HttpServer(8080)
                .get("/") { _, res ->
                    res.status(UNAUTHORIZED_401)
                    res.write("Go Away")
                }
                .start().use {
                    val response = "http://localhost:8080/".http.get()

                    assertThat(response.statusCode).isEqualTo(UNAUTHORIZED_401)
                    assertThat(response.body).isEqualTo("Go Away")
                }
    }

    @Test
    fun `response timeout`() {
        HttpServer(8080)
            .options("/") { _, _ -> Thread.sleep(200)}
            .start().use {
                assertThrows<SocketTimeoutException> {
                    "http://localhost:8080/".http.options(timeoutMillis = 100)
                }
            }
    }

    @Test
    fun `path parameters supports utf8`() {
        HttpServer(8080)
                .get("/:name") { req, res -> res.write(req.param(":name")) }
                .start().use {
                    assertThat("http://localhost:8080/è%24%26".http.get().body).isEqualTo(encode("è$&", UTF_8.name()))
                }
    }

    @Test
    fun `basic auth overrides headers`() {
        HttpServer(8080)
                .basicAuthUser("usr", "pwd")
                .basicAuth("/")
                .get("/") { _, res -> res.status(OK_200) }
                .start().use {
                    val response = "http://localhost:8080/".http.get(auth = Basic("usr", "pwd"), headers = mapOf("Authorization" to "pippo"))

                    assertThat(response.statusCode).isEqualTo(OK_200)
                }
    }
}
