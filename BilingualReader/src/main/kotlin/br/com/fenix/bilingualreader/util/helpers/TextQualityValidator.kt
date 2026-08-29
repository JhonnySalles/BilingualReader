package br.com.fenix.bilingualreader.util.helpers

object TextQualityValidator {

    /**
     * Verifica se o texto tem qualidade mínima para uso como contexto.
     * Rejeita texto que seja maioritariamente caracteres soltos, ruído de OCR
     * ou sequências sem sentido.
     */
    fun isReadable(text: String, minWordRatio: Float = 0.3f): Boolean {
        if (text.isBlank()) return false
        val cleaned = text.trim()
        if (cleaned.length < 10) return false

        val tokens = cleaned.split(Regex("\\s+"))
        if (tokens.isEmpty()) return false

        // Contar tokens que parecem "palavras reais" (>= 2 chars, contém letra ou caractere CJK)
        val wordLikeTokens = tokens.count { token ->
            token.length >= 2 && (token.any { it.isLetter() } || isCjk(token))
        }

        val ratio = wordLikeTokens.toFloat() / tokens.size
        if (ratio < minWordRatio && !isMostlyCjk(cleaned)) return false

        // Verificar se não é maioritariamente caracteres repetidos
        val uniqueChars = cleaned.filter { !it.isWhitespace() }.toSet()
        if (uniqueChars.size < 3 && cleaned.length > 20) return false

        return true
    }

    /**
     * Calcula um score de confiança para o texto (0.0 = lixo, 1.0 = texto perfeito).
     * Útil para decidir se vale a pena incluir no contexto.
     */
    fun confidenceScore(text: String): Float {
        if (text.isBlank()) return 0f
        val cleaned = text.trim()
        if (cleaned.length < 5) return 0f

        val tokens = cleaned.split(Regex("\\s+"))
        val wordTokens = tokens.count { it.length >= 2 && (it.any { c -> c.isLetter() } || isCjk(it)) }
        val avgWordLen = tokens.filter { it.isNotEmpty() }
            .map { it.length.toFloat() }.average().toFloat()

        var score = 0f
        // Razão de palavras reais
        score += (wordTokens.toFloat() / tokens.size.coerceAtLeast(1)) * 0.4f
        // Comprimento médio de palavra razoável (2-15 chars) ou texto CJK
        score += if (avgWordLen in 2f..15f || isMostlyCjk(cleaned)) 0.3f else 0.1f
        // Diversidade de caracteres
        val nonSpace = cleaned.filter { !it.isWhitespace() }
        val uniqueRatio = if (nonSpace.isNotEmpty()) nonSpace.toSet().size.toFloat() / nonSpace.length else 0f
        score += (uniqueRatio.coerceIn(0.1f, 0.5f) / 0.5f) * 0.3f

        return score.coerceIn(0f, 1f)
    }

    private fun isCjk(str: String): Boolean {
        return str.any { c ->
            Character.UnicodeBlock.of(c) in setOf(
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
                Character.UnicodeBlock.HIRAGANA,
                Character.UnicodeBlock.KATAKANA,
                Character.UnicodeBlock.HANGUL_SYLLABLES
            )
        }
    }

    private fun isMostlyCjk(text: String): Boolean {
        val total = text.filter { !it.isWhitespace() }.length
        if (total == 0) return false
        val cjkCount = text.count { isCjk(it.toString()) }
        return (cjkCount.toFloat() / total) > 0.3f
    }
}
