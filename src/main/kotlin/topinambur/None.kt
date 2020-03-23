package topinambur

class None: AuthorizationStrategy {
    override fun toHeader() = emptyMap<String, String>()
}