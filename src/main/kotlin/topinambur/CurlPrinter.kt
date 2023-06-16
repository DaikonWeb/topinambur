package topinambur

import java.io.PrintStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat


class CurlPrinter(private val log: PrintStream?) : Printer {
    override fun print(
        url: String,
        method: String,
        headers: Map<String, String>,
        data: String,
        followRedirects: Boolean,
        timeoutMillis: Int
    ) {
        if (log != null) {
            val headersString =
                if (headers.isEmpty()) "" else headers.map { entry -> "'${escape(entry.key)}: ${escape(entry.value)}'" }
                    .joinToString(" -H ", "-H ", " ")
            val follow = if (followRedirects) "-L " else ""
            val maxTime = "-m ${timeoutMillis.toSeconds().format()} "
            val printData = if (method.needsBody) "-d '${escape(data)}' " else ""

            log.println("curl -v $follow$maxTime-X $method ${printData}$headersString'$url'")
        }
    }

    private fun Int.toSeconds(): BigDecimal {
        return BigDecimal(this).setScale(2).divide(BigDecimal(1000), RoundingMode.DOWN)
    }

    private fun BigDecimal.format(): String {
        val decFormat = DecimalFormat()
        decFormat.maximumFractionDigits = 2
        decFormat.minimumFractionDigits = 0

        return decFormat.format(this)
    }

    private fun escape(text: String): String {
        return text.replace("'", """\'""")
    }
}
