package topinambur

data class ServerResponse(val statusCode: Int, val body: String, val bytes: ByteArray, val headers: Map<String, String>) {
    fun header(key: String): String {
        return headers[key] ?: error("Header '$key' not found")
    }
}