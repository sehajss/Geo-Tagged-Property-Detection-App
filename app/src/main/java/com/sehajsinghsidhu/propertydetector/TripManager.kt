package com.sehajsinghsidhu.propertydetector

import android.content.Context
import android.location.Location
import java.util.UUID
import android.util.Log


class TripManager(private val context: Context) {

    private val firebaseManager = FirebaseManager()

    private val dbManager = DatabaseManager(context)
    private val gpsModule = GPSModule(context)

    private var currentTrip: Trip? = null
    private var isActive = false
    private var lastLocation: Location? = null
    private var totalDistance = 0f

    private val recentDetections = mutableListOf<Detection>()

    fun startTrip() {
        val tripId = UUID.randomUUID().toString()
        val trip = Trip(
            tripId = tripId,
            startTime = System.currentTimeMillis()
        )
        dbManager.insertTrip(trip)
        currentTrip = trip
        isActive = true
        totalDistance = 0f
        Log.d("TripDebug", "Trip saved: ${trip.tripId}")
        Log.d("TripDebug", "Total trips: ${dbManager.getAllTrips().size}")

        gpsModule.startTracking { location ->
            lastLocation?.let {
                val distance = it.distanceTo(location)
                totalDistance += distance
            }
            lastLocation = location
        }
    }

    fun stopTrip() {
        currentTrip?.let { trip ->
            val endTime = System.currentTimeMillis()
            trip.endTime = endTime
            trip.duration = endTime - trip.startTime
            trip.distance = totalDistance
            dbManager.updateTrip(trip)
        }
        gpsModule.stopTracking()
        isActive = false
        currentTrip = null
        lastLocation = null
    }

    fun saveDetection(
        propertyType: String,
        imagePath: String,
        confidence: Float
    ) {
        val trip = currentTrip ?: return
        val location = gpsModule.getLastLocation() ?: return

        // Check for duplicate within 20 meters
        val isDuplicate = recentDetections.any { recent ->
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                recent.latitude, recent.longitude,
                location.latitude, location.longitude,
                results
            )
            results[0] < 20f && recent.propertyType == propertyType
        }

        if (isDuplicate) return

        val detection = Detection(
            tripId = trip.tripId,
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis(),
            propertyType = propertyType,
            imagePath = imagePath,
            confidence = confidence
        )
        dbManager.insertDetection(detection)
        recentDetections.add(detection)
        // Upload to Firebase for shared alerts
        firebaseManager.uploadDetection(detection)
    }
    fun getLastLocation() = gpsModule.getLastLocation()

    fun getCurrentSpeed(): Float {
        return gpsModule.getLastLocation()?.speed ?: 0f
    }

    fun isVehicleMoving(): Boolean {
        // ⚠️ TODO BEFORE RELEASE: Remove the line below and uncomment the speed check
        return true
        // return getCurrentSpeed() > 1.4f // ~5 km/h in m/s
    }

    fun getCurrentTripId(): String? = currentTrip?.tripId

    fun isActive(): Boolean = isActive

    fun getDetectionsForCurrentTrip(): List<Detection> {
        return currentTrip?.let {
            dbManager.getDetectionsForTrip(it.tripId)
        } ?: emptyList()
    }
}