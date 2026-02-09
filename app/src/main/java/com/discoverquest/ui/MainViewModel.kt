package com.discoverquest.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.discoverquest.DiscoverQuestApp
import com.discoverquest.data.local.DiscoveredCity
import com.discoverquest.data.remote.OverpassElement
import com.discoverquest.geofence.GeofenceBroadcastReceiver
import com.discoverquest.repository.CityRepository
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LocationState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isAvailable: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CityRepository
    private val geofencingClient: GeofencingClient
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _location = MutableStateFlow(LocationState())
    val location: StateFlow<LocationState> = _location.asStateFlow()

    private val _nearbyCities = MutableStateFlow<List<OverpassElement>>(emptyList())
    val nearbyCities: StateFlow<List<OverpassElement>> = _nearbyCities.asStateFlow()

    val discoveredCities: StateFlow<List<DiscoveredCity>>

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                _location.value = LocationState(loc.latitude, loc.longitude, true)
                fetchNearbyCities(loc.latitude, loc.longitude)
            }
        }
    }

    init {
        val app = application as DiscoverQuestApp
        repository = CityRepository(app.database)
        geofencingClient = LocationServices.getGeofencingClient(application)

        discoveredCities = repository.discoveredCities
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        startLocationUpdates()
    }

    private fun startLocationUpdates() {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10_000L
        ).setMinUpdateIntervalMillis(5_000L).build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private fun fetchNearbyCities(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                val cities = repository.fetchNearbyCities(lat, lon)
                if (cities.isNotEmpty()) {
                    _nearbyCities.value = cities
                    registerGeofences(cities)
                }
            } catch (e: Exception) {
                // Network errors are expected when offline
            }
        }
    }

    @Suppress("MissingPermission")
    private fun registerGeofences(elements: List<OverpassElement>) {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val geofences = elements.map { element ->
            GeofenceBroadcastReceiver.createGeofence(
                element.id.toString(),
                element.effectiveLat,
                element.effectiveLon
            )
        }

        val request = GeofenceBroadcastReceiver.buildGeofencingRequest(geofences)
        val pendingIntent = GeofenceBroadcastReceiver.getPendingIntent(context)

        geofencingClient.addGeofences(request, pendingIntent)
    }

    override fun onCleared() {
        super.onCleared()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(application) as T
        }
    }
}
