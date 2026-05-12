package com.sehajsinghsidhu.propertydetector

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

data class SharedDetection(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val propertyType: String = "",
    val timestamp: Long = 0,
    val confidence: Float = 0f
)

class FirebaseManager {

    private val database = FirebaseDatabase.getInstance()
    private val detectionsRef = database.getReference("detections")

    fun uploadDetection(detection: Detection) {
        val shared = SharedDetection(
            id = detection.id.toString(),
            latitude = detection.latitude,
            longitude = detection.longitude,
            propertyType = detection.propertyType,
            timestamp = detection.timestamp,
            confidence = detection.confidence
        )
        detectionsRef.push().setValue(shared)
    }

    fun getNearbyDetections(
        currentLat: Double,
        currentLng: Double,
        radiusMeters: Float = 50f,
        onDetectionsFound: (List<SharedDetection>) -> Unit
    ) {
        detectionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nearby = mutableListOf<SharedDetection>()
                snapshot.children.forEach { child ->
                    val detection = child.getValue(SharedDetection::class.java) ?: return@forEach
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        currentLat, currentLng,
                        detection.latitude, detection.longitude,
                        results
                    )
                    if (results[0] <= radiusMeters) {
                        nearby.add(detection)
                    }
                }
                if (nearby.isNotEmpty()) {
                    onDetectionsFound(nearby)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseManager", "Error: ${error.message}")
            }
        })
    }
}