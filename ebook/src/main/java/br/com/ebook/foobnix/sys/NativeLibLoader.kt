package br.com.ebook.foobnix.sys

import org.slf4j.LoggerFactory

/**
 * Utility to safely load native libraries.
 * Handles UnsatisfiedLinkError by logging a warning, which is common in unit tests
 * running on the JVM where Android native libraries are not available.
 */
object NativeLibLoader {
    private val LOGGER = LoggerFactory.getLogger(NativeLibLoader::class.java)

    @JvmStatic
    fun loadLibrary(libName: String) {
        val targets = when (libName.lowercase()) {
            "mypdf", "mupdf", "mobi" -> listOf("MuPDF", "mupdf", "mypdf", "mobi")
            else -> listOf(libName)
        }
        for (target in targets) {
            try {
                System.loadLibrary(target)
                return
            } catch (e: UnsatisfiedLinkError) {
                // Tenta próximo candidato da lista de compatibilidade
            } catch (e: LinkageError) {
                LOGGER.warn("Linkage error loading native library {}: {}", target, e.message)
            } catch (e: Throwable) {
                LOGGER.error("Unexpected error loading native library {}: {}", target, e)
            }
        }
        LOGGER.warn("Could not load native library: {}. This is expected in unit tests if not running on Android.", libName)
    }
}
