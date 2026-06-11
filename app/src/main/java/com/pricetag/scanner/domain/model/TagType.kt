package com.pricetag.scanner.domain.model

enum class TagType(val label: String, val protocol: String) {
    A4("A4",                   "A4"),
    FOUR_PCS("4 PCS",          "4PCS"),
    FOUR_PCS_DATE("4 PCS DATE","4PCS_DATE"),
    FOUR_PCS_SAME("4 PCS SAME","4PCS_SAME"),
    VEG("VEG",                 "VEG");

    val is4PcsVariant: Boolean
        get() = this == FOUR_PCS || this == FOUR_PCS_DATE || this == FOUR_PCS_SAME

    val isSingleBarcode: Boolean
        get() = this == A4 || this == VEG || this == FOUR_PCS_SAME

    companion object {
        fun fromProtocol(s: String) = values().firstOrNull { it.protocol == s } ?: A4
    }
}
