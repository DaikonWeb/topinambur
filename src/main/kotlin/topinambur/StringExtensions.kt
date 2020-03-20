package topinambur

import java.io.PrintStream


fun String.http(printer: PrintStream? = null) = HttpClient(this, printer)

val String.http: HttpClient
    get() = HttpClient(this, null)

val String.needsBody
    get() = this == "POST" || this == "PUT"