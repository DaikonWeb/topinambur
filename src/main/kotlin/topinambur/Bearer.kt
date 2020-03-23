package topinambur

class Bearer(private val accessToken: String): AuthorizationStrategy {
    override fun toHeader(): Map<String, String> {
        return mapOf("Authorization" to "Bearer $accessToken")
    }
}