package topinambur

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CurlPrinterTest {
    private val output = ByteArrayOutputStream()

    @Test
    fun `post as Curl`() {
        CurlPrinter(PrintStream(output)).print(
            url = "url",
            method = "POST",
            data = "name=Bob&surname=Or",
            headers = mapOf("foo" to "bar", "baz" to "foo"),
            followRedirects = true,
            timeoutMillis = 5000
        )

        assertThat(output.toString()).isEqualTo("curl -v -L -m 5 -X POST -d 'name=Bob&surname=Or' -H 'foo: bar' -H 'baz: foo' 'url'\n")
    }

    @Test
    fun `get as Curl`() {
        CurlPrinter(PrintStream(output)).print(
            url = "url?name=Bob&surname=Or",
            method = "GET",
            data = "",
            headers = mapOf("foo" to "bar", "baz" to "foo"),
            followRedirects = false,
            timeoutMillis = 5000
        )

        assertThat(output.toString()).isEqualTo("curl -v -m 5 -X GET -H 'foo: bar' -H 'baz: foo' 'url?name=Bob&surname=Or'\n")
    }

    @Test
    fun `get as Curl without headers`() {
        CurlPrinter(PrintStream(output)).print(
            url = "url",
            method = "GET",
            data = "",
            headers = mapOf(),
            followRedirects = false,
            timeoutMillis = 5000
        )

        assertThat(output.toString()).isEqualTo("curl -v -m 5 -X GET 'url'\n")
    }

    @Test
    fun `backslashes quotes`() {
        CurlPrinter(PrintStream(output)).print(
            url = "url",
            method = "POST",
            data = "string 'with' single quotes",
            headers = mapOf("foo" to "b'a'r", "baz" to "foo"),
            followRedirects = true,
            timeoutMillis = 5000
        )

        assertThat(output.toString())
            .isEqualTo("""curl -v -L -m 5 -X POST -d 'string \'with\' single quotes' -H 'foo: b\'a\'r' -H 'baz: foo' 'url'
                    |""".trimMargin())
    }

    @Test
    fun `timeout with seconds precision`() {
        CurlPrinter(PrintStream(output)).print(
            url = "url",
            method = "GET",
            data = "",
            headers = mapOf(),
            followRedirects = false,
            timeoutMillis = 5000
        )

        assertThat(output.toString()).isEqualTo("curl -v -m 5 -X GET 'url'\n")
    }

    @Test
    fun `timeout with decimal precision`() {
        CurlPrinter(PrintStream(output)).print(
            url = "url",
            method = "GET",
            data = "",
            headers = mapOf(),
            followRedirects = false,
            timeoutMillis = 1234
        )

        assertThat(output.toString()).isEqualTo("curl -v -m 1.23 -X GET 'url'\n")
    }
}