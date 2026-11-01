package com.ultraviolette.navigator.model

/**
 * Selectable cluster brand pack (Settings → Themes).
 * Remaps ride-mode accents while keeping GLIDE / COMBAT / BALLISTIC behavior.
 */
object BrandTheme {
    const val UV = "UV"
    const val KTM = "KTM"
    const val BMW = "BMW"
    const val APRILIA = "APRILIA"

    val DEFAULT = UV

    val order = listOf(UV, KTM, BMW, APRILIA)

    data class Meta(
        val id: String,
        val label: String,
        val tagline: String
    )

    val catalog = listOf(
        Meta(UV, "UV", "ULTRAVIOLETTE"),
        Meta(KTM, "KTM", "READY TO RACE"),
        Meta(BMW, "BMW", "MOTORRAD"),
        Meta(APRILIA, "aprilia", "RACING")
    )

    fun canonicalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return order.firstOrNull { it.equals(raw, ignoreCase = true) }
    }

    fun meta(id: String): Meta =
        catalog.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: catalog.first()
}
