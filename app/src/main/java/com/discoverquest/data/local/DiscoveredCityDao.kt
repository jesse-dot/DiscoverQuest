package com.discoverquest.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveredCityDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(city: DiscoveredCity)

    @Query("SELECT * FROM discovered_cities ORDER BY discoveredAt DESC")
    fun getAllDiscovered(): Flow<List<DiscoveredCity>>

    @Query("SELECT EXISTS(SELECT 1 FROM discovered_cities WHERE id = :cityId)")
    suspend fun isDiscovered(cityId: Long): Boolean
}
