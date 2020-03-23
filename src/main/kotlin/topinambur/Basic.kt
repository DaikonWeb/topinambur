package topinambur

import java.nio.charset.StandardCharsets
import java.util.*

class Basic(private val user: String, private val password: String): AuthorizationStrategy {
    override fun toHeader(): Map<String, String> {
        val auth = String(Base64.getEncoder().encode("$user:$password".toByteArray()), StandardCharsets.UTF_8)
        return mapOf("Authorization" to "Basic $auth")
    }
}