package com.discoverquest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovered_cities")
data class DiscoveredCity(
    @PrimaryKey val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val placeType: String,
    val discoveredAt: Long = System.currentTimeMillis()
)
