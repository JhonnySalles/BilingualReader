package br.com.fenix.bilingualreader.model.enums

enum class LlmProvider(val prefValue: String) {
    AUTO("auto"),
    ON_DEVICE("on_device"),
    OPENROUTER("openrouter");

    companion object {
        fun fromPref(value: String?): LlmProvider {
            return entries.firstOrNull { it.prefValue == value } ?: AUTO
        }
    }
}
