package com.pricetag.scanner.domain.model

enum class UnitType(val label: String, val protocol: String) {
    PCS("PCS", "PCS"),
    CTN("CTN", "CTN"),
    PKT("PKT", "PKT"),
    KGS("KGS", "KGS");

    companion object {
        fun fromProtocol(s: String) = values().firstOrNull { it.protocol == s } ?: PCS
    }
}
