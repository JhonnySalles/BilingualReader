package br.com.fenix.bilingualreader.service.llm

/**
 * Classe base para exceções operacionais esperadas do ecossistema LLM
 * (por exemplo: falta de saldo, limite de taxa 429, chave de API ausente ou inválida, conexão de rede).
 * Exceções derivadas desta classe NÃO devem ser registradas no Sentry ou Crashlytics.
 */
abstract class LlmExpectedException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class OpenRouterRateLimitException(
    message: String,
    cause: Throwable? = null
) : LlmExpectedException(message, cause)

class OpenRouterQuotaExceededException(
    message: String,
    cause: Throwable? = null
) : LlmExpectedException(message, cause)

class OpenRouterAuthException(
    message: String,
    cause: Throwable? = null
) : LlmExpectedException(message, cause)

class OpenRouterNetworkException(
    message: String,
    cause: Throwable? = null
) : LlmExpectedException(message, cause)

class OpenRouterServiceUnavailableException(
    message: String,
    cause: Throwable? = null
) : LlmExpectedException(message, cause)
