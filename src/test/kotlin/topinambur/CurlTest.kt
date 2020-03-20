package topinambur

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class CurlTest {
    private val output = ByteArrayOutputStream()

    @Test
    fun `post as Curl`() {
        Curl(PrintStream(output)).print(
                url = "url",
                method = "POST",
                data = "name=Bob&surname=Or",
                headers = mapOf("foo" to "bar", "baz" to "foo"),
                followRedirects = true
        )

        assertThat(output.toString()).isEqualTo("curl -v -L -X POST -d 'name=Bob&surname=Or' -H 'foo: bar' -H 'baz: foo' 'url'\n")
    }

    @Test
    fun `get as Curl`() {
        Curl(PrintStream(output)).print(
                url = "url?name=Bob&surname=Or",
                method = "GET",
                data = "",
                headers = mapOf("foo" to "bar", "baz" to "foo"),
                followRedirects = false
        )

        assertThat(output.toString()).isEqualTo("curl -v -X GET -H 'foo: bar' -H 'baz: foo' 'url?name=Bob&surname=Or'\n")
    }

    @Test
    fun `get as Curl without headers`() {
        Curl(PrintStream(output)).print(
                url = "url",
                method = "GET",
                data = "",
                headers = mapOf(),
                followRedirects = false
        )

        assertThat(output.toString()).isEqualTo("curl -v -X GET 'url'\n")
    }

    @Test
    fun `backslashes quotes`() {
        Curl(PrintStream(output)).print(
                url = "url",
                method = "POST",
                data = "string 'with' single quotes",
                headers = mapOf("foo" to "b'a'r", "baz" to "foo"),
                followRedirects = true
        )

        assertThat(output.toString())
                .isEqualTo("""curl -v -L -X POST -d 'string \'with\' single quotes' -H 'foo: b\'a\'r' -H 'baz: foo' 'url'
                    |""".trimMargin())
    }
}