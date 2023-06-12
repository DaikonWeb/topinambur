package topinambur

import java.io.PrintStream


fun String.http(printer: PrintStream? = null) = Http(this, printer)

val String.http: Http
    get() = Http(this, null)

val String.needsBody
    get() = this == "POST" || this == "PUT" || this == "DELETE"