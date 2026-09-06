package com.phoenix.phnx.adblock

data class AdBlockRule(
    val host: String,
    val exception: Boolean = false,
)

object AdBlockFilterParser {
    fun parse(payload: String): List<AdBlockRule> = payload
        .lineSequence()
        .mapNotNull(::parseLine)
        .distinctBy { it.exception to it.host }
        .toList()

    private fun parseLine(raw: String): AdBlockRule? {
        val line = raw.trim()
        if (line.isBlank() || line.startsWith("!") || line.startsWith("[") || line.contains("##")) return null

        val exception = line.startsWith("@@")
        val candidate = line.removePrefix("@@")
        val host = when {
            candidate.startsWith("||") -> candidate
                .removePrefix("||")
                .substringBefore('^')
                .substringBefore('/')
                .substringBefore('$')
            candidate.split(Regex("\\s+")).size == 2 && candidate.split(Regex("\\s+"))[0].isIpAddress() ->
                candidate.split(Regex("\\s+"))[1]
            else -> return null
        }
        val canonicalHost = host.lowercase().trim('.')
        if (!DOMAIN_PATTERN.matches(canonicalHost)) return null
        return AdBlockRule(host = canonicalHost, exception = exception)
    }

    private fun String.isIpAddress(): Boolean = matches(IP_PATTERN)

    private val DOMAIN_PATTERN = Regex("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+")
    private val IP_PATTERN = Regex("(?:\\d{1,3}\\.){3}\\d{1,3}")
}
