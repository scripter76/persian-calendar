package com.byagowi.persiancalendar.entities

enum class GlassWidgetLanguage(val code: String) {
    FA("fa"),
    EN("en");

    val targetLanguage: Language
        get() = when (this) {
            FA -> Language.FA
            EN -> Language.EN_US
        }

    companion object {
        val DEFAULT = FA
        fun fromName(name: String?): GlassWidgetLanguage =
            entries.firstOrNull { it.name == name }
                ?: fromCode(name)
                ?: DEFAULT

        private fun fromCode(code: String?): GlassWidgetLanguage? =
            entries.firstOrNull { it.code == code }
    }
}
