package com.discoverquest.data.remote

data class OverpassResponse(
    val elements: List<OverpassElement>
)

data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double?,
    val lon: Double?,
    val center: OverpassCenter?,
    val tags: Map<String, String>?
) {
    val effectiveLat: Double get() = lat ?: center?.lat ?: 0.0
    val effectiveLon: Double get() = lon ?: center?.lon ?: 0.0
    val name: String get() = tags?.get("name") ?: "Unknown"
    val placeType: String get() = tags?.get("place") ?: "unknown"
}

data class OverpassCenter(
    val lat: Double,
    val lon: Double
)
