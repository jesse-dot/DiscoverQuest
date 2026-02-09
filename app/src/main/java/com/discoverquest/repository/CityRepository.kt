package com.discoverquest.repository

import android.location.Location
import com.discoverquest.data.local.AppDatabase
import com.discoverquest.data.local.DiscoveredCity
import com.discoverquest.data.remote.OverpassElement
import com.discoverquest.data.remote.OverpassService
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CityRepository(private val database: AppDatabase) {

    private val overpassService: OverpassService = Retrofit.Builder()
        .baseUrl(OverpassService.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OverpassService::class.java)

    private var lastFetchLat: Double? = null
    private var lastFetchLon: Double? = null

    companion object {
        private const val MOVE_THRESHOLD_METERS = 2000f
    }

    val discoveredCities: Flow<List<DiscoveredCity>> =
        database.discoveredCityDao().getAllDiscovered()

    fun shouldFetch(lat: Double, lon: Double): Boolean {
        val lastLat = lastFetchLat ?: return true
        val lastLon = lastFetchLon ?: return true

        val results = FloatArray(1)
        Location.distanceBetween(lastLat, lastLon, lat, lon, results)
        return results[0] > MOVE_THRESHOLD_METERS
    }

    suspend fun fetchNearbyCities(lat: Double, lon: Double): List<OverpassElement> {
        if (!shouldFetch(lat, lon)) return emptyList()

        val query = OverpassService.buildNearbyPlacesQuery(lat, lon)
        val response = overpassService.query(query)

        lastFetchLat = lat
        lastFetchLon = lon

        return response.elements.filter { it.tags?.get("name") != null }
    }

    suspend fun markDiscovered(element: OverpassElement) {
        val city = DiscoveredCity(
            id = element.id,
            name = element.name,
            latitude = element.effectiveLat,
            longitude = element.effectiveLon,
            placeType = element.placeType
        )
        database.discoveredCityDao().insert(city)
    }

    suspend fun isDiscovered(cityId: Long): Boolean {
        return database.discoveredCityDao().isDiscovered(cityId)
    }
}
