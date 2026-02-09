package com.discoverquest.data.remote

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OverpassService {

    @FormUrlEncoded
    @POST("interpreter")
    suspend fun query(@Field("data") data: String): OverpassResponse

    companion object {
        const val BASE_URL = "https://overpass-api.de/api/"

        fun buildNearbyPlacesQuery(lat: Double, lon: Double, radiusMeters: Int = 15000): String {
            return """
                [out:json][timeout:25];
                (
                  node["place"~"city|town|village"](around:$radiusMeters,$lat,$lon);
                  way["place"~"city|town|village"](around:$radiusMeters,$lat,$lon);
                  relation["place"~"city|town|village"](around:$radiusMeters,$lat,$lon);
                );
                out center;
            """.trimIndent()
        }
    }
}
