package topinambur

interface AuthorizationStrategy {
    fun toHeader(): Map<String, String>
}
