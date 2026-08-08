package br.com.ebook.core

interface BookExtractor {
    val supportedFormats: Set<String>

    suspend fun extractMetadata(path: String): Result<BookMetadata>
    suspend fun extractCover(path: String): Result<ByteArray?>
    suspend fun extractContent(path: String, outputDir: String): Result<BookContent>
    suspend fun extractFooterNotes(path: String): Result<Map<String, String>>
    suspend fun extractOverview(path: String): Result<String>
}
