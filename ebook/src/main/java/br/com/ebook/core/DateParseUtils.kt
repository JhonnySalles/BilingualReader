package br.com.ebook.core

import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DateParseUtils {
    private val LOGGER = LoggerFactory.getLogger(DateParseUtils::class.java)

    private val formatters = listOf(
        DateTimeFormatter.ISO_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("yyyy")
    )

    fun parseFlexibleDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        val cleaned = dateStr.trim()
        
        for (formatter in formatters) {
            try {
                if (formatter == DateTimeFormatter.ISO_DATE_TIME || cleaned.contains("-") || cleaned.contains("/") || cleaned.contains(".")) {
                    // Tenta fazer o parse completo
                    try {
                        return LocalDate.parse(cleaned, formatter)
                    } catch (e: DateTimeParseException) {
                        // Se falhar e for formato com tempo, tenta extrair só a data
                        if (cleaned.contains(" ") || cleaned.contains("T")) {
                            val part = cleaned.split("T", " ")[0]
                            return LocalDate.parse(part, DateTimeFormatter.ofPattern(
                                if (part.contains("-")) "yyyy-MM-dd"
                                else if (part.contains("/")) "dd/MM/yyyy"
                                else "dd.MM.yyyy"
                            ))
                        }
                    }
                } else if (cleaned.length == 4 && cleaned.all { it.isDigit() }) {
                    // Trata apenas ano (ex: "2026")
                    return LocalDate.of(cleaned.toInt(), 1, 1)
                }
            } catch (e: Exception) {
                // Silenciosamente ignora e tenta o próximo
            }
        }
        
        LOGGER.warn("Nao foi possivel converter a data: {}", dateStr)
        return null
    }
}
