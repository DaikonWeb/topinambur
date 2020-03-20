package topinambur

import java.io.PrintStream

class Curl(private val log: PrintStream?) {
    fun print(url: String, method: String, headers: Map<String, String>, data: String, followRedirects: Boolean) {
        if (log != null) {
            val headersString = if (headers.isEmpty()) "" else headers.map { entry -> "'${escape(entry.key)}: ${escape(entry.value)}'" }.joinToString(" -H ", "-H ", " ")
            val follow = if (followRedirects) "-L " else ""
            val printData = if (method.needsBody) "-d '${escape(data)}' " else ""

            log.println("curl -v $follow-X $method ${printData}$headersString'$url'")
        }
    }

    private fun escape(text: String): String {
        return text.replace("'", """\'""")
    }
}
