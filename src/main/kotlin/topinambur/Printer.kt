package topinambur

interface Printer {
    fun print(
        url: String,
        method: String,
        headers: Map<String, String>,
        data: String,
        followRedirects: Boolean,
        timeoutMillis: Int
    )
}