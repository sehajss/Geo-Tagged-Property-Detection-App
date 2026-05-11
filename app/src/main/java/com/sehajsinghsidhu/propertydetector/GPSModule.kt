package com.sehajsinghsidhu.propertydetector

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*

class GPSModule(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var lastLocation: Location? = null
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startTracking(onLocationUpdate: (Location) -> Unit) {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000L
        ).setMinUpdateDistanceMeters(5f).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    lastLocation = it
                    onLocationUpdate(it)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }

    fun getLastLocation(): Location? = lastLocation
}