package topinambur

import daikon.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.eclipse.jetty.http.HttpStatus.*
import org.junit.jupiter.api.Test
import topinambur.Http.Companion.HTTP
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.net.SocketTimeoutException
import java.net.URLEncoder.encode
import java.nio.charset.StandardCharsets.UTF_8

class HttpTest {

    @Test
    fun `GET request`() {
        HttpServer(8080)
                .get("/") { req, res -> res.write(req.param("name")) }
                .start().use {
                    val response = HTTP.get("http://localhost:8080/", params = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `POST request`() {
        HttpServer(8080)
                .post("/") { req, res -> res.write(req.body()) }
                .start().use {
                    val response = HTTP.post("http://localhost:8080/", data = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("name=Bob")
                }
    }

    @Test
    fun `POST body`() {
        HttpServer(8080)
                .post("/") { req, res -> res.write(req.body()) }
                .start().use {
                    val response = HTTP.post("http://localhost:8080/", body = "Bob")
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `POST without body`() {
        HttpServer(8080)
                .post("/") { _, res -> res.write("post response") }
                .start().use {
                    val response = HTTP.post("http://localhost:8080/", body = "")
                    assertThat(response.body).isEqualTo("post response")
                }
    }

    @Test
    fun `request header`() {
        HttpServer(8080)
                .get("/") { req, res -> res.write(req.header("name")) }
                .start().use {
                    val response = HTTP.get("http://localhost:8080/", headers = mapOf("name" to "Bob"))
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `response without body`() {
        HttpServer(8080)
                .get("/") { _, res -> res.status(INTERNAL_SERVER_ERROR_500) }
                .start().use {
                    val response = HTTP.get("http://localhost:8080/", params = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(INTERNAL_SERVER_ERROR_500)
                    assertThat(response.body).isEqualTo("")
                }
    }

    @Test
    fun `follow redirect from HTTP to HTTPS`() {
        val http = Http(followRedirects = false)
        val response = http.get("http://www.trovaprezzi.it/", followRedirects = true)

        assertThat(response.statusCode).isEqualTo(OK_200)
    }

    @Test
    fun `do not follow redirect from HTTP to HTTPS`() {
        val http = Http(followRedirects = false)
        val response = http.get("http://www.trovaprezzi.it/")

        assertThat(response.statusCode).isEqualTo(MOVED_PERMANENTLY_301)
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
                    val response = HTTP.get("http://localhost:8080/bar")
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
                    val response = HTTP.get("http://localhost:8080/bar", followRedirects = false)
                    assertThat(response.statusCode).isEqualTo(MOVED_TEMPORARILY_302)
                }
    }

    @Test
    fun `DELETE request`() {
        HttpServer(8080)
            .delete("/") { _, res -> res.write("DELETED") }
            .start().use {
                val response = HTTP.delete("http://localhost:8080/", )

                assertThat(response.statusCode).isEqualTo(OK_200)
                assertThat(response.body).isEqualTo("DELETED")
            }
    }

    @Test
    fun `DELETE with body request`() {
        HttpServer(8080)
            .delete("/") { req, res -> res.write("DELETE ${req.body()}") }
            .start().use {
                val response = HTTP.delete("http://localhost:8080/", body = "ME")

                assertThat(response.body).isEqualTo("DELETE ME")
            }
    }

    @Test
    fun `DELETE with data request`() {
        HttpServer(8080)
            .delete("/") { req, res -> res.write("DELETE ${req.param("text")}") }
            .start().use {
                val response = HTTP.delete("http://localhost:8080/", data = mapOf("text" to "hello"))

                assertThat(response.body).isEqualTo("DELETE hello")
            }
    }

    @Test
    fun `HEAD request should ignore the body response`() {
        HttpServer(8080)
            .head("/") { _, res -> res.write("IGNORE") }
            .start().use {
                val response = HTTP.head("http://localhost:8080/", )

                assertThat(response.statusCode).isEqualTo(OK_200)
                assertThat(response.body).isEqualTo("")
            }
    }

    @Test
    fun `HEAD request retrieves the response headers`() {
        HttpServer(8080)
            .head("/") { req, res ->
                res.header("received", req.param("name"))
                res.write("IGNORE")
            }
            .start().use {
                val response = HTTP.head("http://localhost:8080/", params = mapOf("name" to "Bob"))

                assertThat(response.headers["received"]).isEqualTo("Bob")
            }
    }

    @Test
    fun `PUT request without body or data`() {
        HttpServer(8080)
            .put("/") { req, res -> res.write(req.body()) }
            .start().use {
                val response = HTTP.put("http://localhost:8080/", )
                assertThat(response.body).isEqualTo("")
            }
    }

    @Test
    fun `PUT request with body`() {
        HttpServer(8080)
            .put("/") { req, res -> res.write(req.body()) }
            .start().use {
                val response = HTTP.put("http://localhost:8080/", body = "Bob")
                assertThat(response.body).isEqualTo("Bob")
            }
    }

    @Test
    fun `PUT request with data`() {
        HttpServer(8080)
            .put("/") { req, res -> res.write(req.body()) }
            .start().use {
                val response = HTTP.put("http://localhost:8080/", data = mapOf("field" to "value"))
                assertThat(response.body).isEqualTo("field=value")
            }
    }

    @Test
    fun `OPTIONS request`() {
        HttpServer(8080)
                .options("/") { _, _ -> }
                .start().use {
                    val response = HTTP.options("http://localhost:8080/", params = mapOf("name" to "Bob"))
                    assertThat(response.statusCode).isEqualTo(OK_200)
                    assertThat(response.body).isEqualTo("")
                }
    }

    @Test
    fun `call with get request`() {
        HttpServer(8080)
                .get("/") { req, res -> res.write(req.param("name")) }
                .start().use {
                    val response = Http().call("http://localhost:8080/", method = "get", params = mapOf("name" to "Bob"))
                    assertThat(response.body).isEqualTo("Bob")
                }
    }

    @Test
    fun `call has default get request`() {
        HttpServer(8080)
            .get("/") { _, res -> res.status(OK_200) }
            .start().use {
                val response = Http("http://localhost:8080/").call()
                assertThat(response.statusCode).isEqualTo(OK_200)
            }
    }

    @Test
    fun `default headers`() {
        HttpServer(8080)
                .get("/") { req, res ->
                    res.write("${req.header("Accept")}|${req.header("User-Agent")}")
                }
                .start().use {
                    val response = Http("http://localhost:8080").get("/")

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
                    val response = HTTP.get("http://localhost:8080/", headers = mapOf("User-Agent" to "Bob"))

                    assertThat(response.body).isEqualTo("*/*|Bob")
                }
    }

    @Test
    fun `overrides the default headers from the Http constructor`() {
        HttpServer(8080)
            .get("/") { req, res ->
                res.write("${req.header("Accept")}|${req.header("User-Agent")}")
            }
            .start().use {
                val http = Http(headers = mapOf("Accept" to "text/plain"))
                val response = http.get("http://localhost:8080/")

                assertThat(response.body).isEqualTo("text/plain|daikonweb/topinambur")
            }
    }

    @Test
    fun `responds with headers`() {
        HttpServer(8080)
            .options("/") { _, res ->
                res.header("Allow", "OPTIONS, GET, POST")
            }
            .start().use {
                val response = HTTP.options("http://localhost:8080/")

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
                val response = HTTP.options("http://localhost:8080/", )

                assertThat(response.header("Allow")).isEqualTo("OPTIONS, GET, POST")
            }
    }

    @Test
    fun `response header not found`() {
        HttpServer(8080)
            .options("/") { _, _ -> }
            .start().use {
                val response = HTTP.options("http://localhost:8080/", )

                assertThatThrownBy { response.header("Allow") }.isInstanceOf(IllegalStateException::class.java)
            }
    }

    @Test
    fun `response header for empty key`() {
        HttpServer(8080)
                .options("/") { _, _ -> }
                .start().use {
                    val response = HTTP.options("http://localhost:8080/", )

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
                    val response = HTTP.get("http://localhost:8080/", )

                    assertThat(response.statusCode).isEqualTo(UNAUTHORIZED_401)
                    assertThat(response.body).isEqualTo("Go Away")
                }
    }

    @Test
    fun `response timeout`() {
        HttpServer(8080)
            .options("/") { _, _ -> Thread.sleep(200)}
            .start().use {
                assertThatThrownBy {
                    HTTP.options("http://localhost:8080/", timeoutMillis = 100)
                }.isInstanceOf(SocketTimeoutException::class.java)
            }
    }

    @Test
    fun `path parameters supports utf8`() {
        HttpServer(8080)
                .get("/:name") { req, res -> res.write(req.param(":name")) }
                .start().use {
                    assertThat(HTTP.get("http://localhost:8080/è%24%26").body).isEqualTo(encode("è$&", UTF_8.name()))
                }
    }

    @Test
    fun `basic auth from http constructor overrides headers`() {
        HttpServer(8080)
            .basicAuthUser("usr", "pwd")
            .basicAuth("/")
            .get("/") { _, res -> res.status(OK_200) }
            .start().use {
                val http = Http(auth = Basic("usr", "pwd"))
                val response = http.get("http://localhost:8080/", headers = mapOf("Authorization" to "pippo"))

                assertThat(response.statusCode).isEqualTo(OK_200)
            }
    }

    @Test
    fun `basic auth overrides headers and baseAuth`() {
        HttpServer(8080)
            .basicAuthUser("usr", "pwd")
            .basicAuth("/")
            .get("/") { _, res -> res.status(OK_200) }
            .start().use {
                val http = Http(auth = Basic("wrong", "wrong"))
                val response = http.get("http://localhost:8080/", auth = Basic("usr", "pwd"), headers = mapOf("Authorization" to "pippo"))

                assertThat(response.statusCode).isEqualTo(OK_200)
            }
    }

    @Test
    fun `bearer auth overrides headers`() {
        HttpServer(8080)
            .get("/") { req, res ->
                res.write(req.header("Authorization"))
            }
            .start().use {
                val response = HTTP.get("http://localhost:8080/", auth = Bearer("token"), headers = mapOf("Authorization" to "pippo"))

                assertThat(response.body).isEqualTo("Bearer token")
            }
    }

    @Test
    fun `two request with same baseUrl using the same client`() {
        HttpServer(8080)
            .get("/first") { _, res -> res.status(OK_200) }
            .post("/second") { _, res -> res.status(CREATED_201) }
            .start().use {
                val localhost = Http("http://localhost:8080")
                assertThat(localhost.get("/first").statusCode).isEqualTo(OK_200)
                assertThat(localhost.post("/second", "").statusCode).isEqualTo(CREATED_201)
            }
    }

    @Test
    fun `test http client string extension enabling logs`() {
        HttpServer(8080)
            .get("/") { _, res -> res.status(OK_200) }
            .start().use {
                val output = ByteArrayOutputStream()
                Http(log = PrintStream(output)).get("http://localhost:8080")

                assertThat(output.toString()).isEqualTo("curl -v -L -X GET -H 'Accept: */*' -H 'User-Agent: daikonweb/topinambur' 'http://localhost:8080'\n")
            }
    }
}
