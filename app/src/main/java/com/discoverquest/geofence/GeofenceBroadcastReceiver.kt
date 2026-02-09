package com.discoverquest.geofence

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.discoverquest.R
import com.discoverquest.audio.DiscoveryAudioManager
import com.discoverquest.data.local.AppDatabase
import com.discoverquest.data.local.DiscoveredCity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "GeofenceReceiver"
        private const val GEOFENCE_RADIUS_METERS = 500f
        private const val PREFS_NAME = "geofence_city_data"

        fun createGeofence(id: String, lat: Double, lon: Double): Geofence {
            return Geofence.Builder()
                .setRequestId(id)
                .setCircularRegion(lat, lon, GEOFENCE_RADIUS_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL
                )
                .setLoiteringDelay(30_000)
                .build()
        }

        fun buildGeofencingRequest(geofences: List<Geofence>): GeofencingRequest {
            return GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build()
        }

        fun getPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
            return PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        }

        fun storeCityData(
            context: Context,
            cityId: Long,
            name: String,
            lat: Double,
            lon: Double,
            placeType: String
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("${cityId}_name", name)
                .putFloat("${cityId}_lat", lat.toFloat())
                .putFloat("${cityId}_lon", lon.toFloat())
                .putString("${cityId}_type", placeType)
                .apply()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, "Geofence error: $errorMessage")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER ||
            transitionType == Geofence.GEOFENCE_TRANSITION_DWELL
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return
            for (geofence in triggeringGeofences) {
                handleDiscovery(context, geofence.requestId)
            }
        }
    }

    private fun handleDiscovery(context: Context, geofenceId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val cityId = geofenceId.toLongOrNull() ?: return@launch
            val alreadyDiscovered = db.discoveredCityDao().isDiscovered(cityId)

            if (!alreadyDiscovered) {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val name = prefs.getString("${cityId}_name", null) ?: "City $cityId"
                val lat = prefs.getFloat("${cityId}_lat", 0f).toDouble()
                val lon = prefs.getFloat("${cityId}_lon", 0f).toDouble()
                val placeType = prefs.getString("${cityId}_type", null) ?: "unknown"

                val city = DiscoveredCity(
                    id = cityId,
                    name = name,
                    latitude = lat,
                    longitude = lon,
                    placeType = placeType
                )
                db.discoveredCityDao().insert(city)

                sendNotification(context, city.name)

                val audioManager = DiscoveryAudioManager(context)
                audioManager.playDiscoverySound()
            }
        }
    }

    private fun sendNotification(context: Context, cityName: String) {
        val notification = NotificationCompat.Builder(
            context,
            context.getString(R.string.notification_channel_id)
        )
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle("New Discovery!")
            .setContentText("You discovered $cityName!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(cityName.hashCode(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted", e)
        }
    }
}
