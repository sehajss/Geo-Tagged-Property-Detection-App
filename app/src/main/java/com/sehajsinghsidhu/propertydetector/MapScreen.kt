package com.sehajsinghsidhu.propertydetector

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.BitmapFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Date

@Composable
fun MapScreen(
    tripId: String?,
    dbManager: DatabaseManager,
    previousTrips: List<Trip>
) {
    val context = LocalContext.current
    var selectedDetection by remember {
        mutableStateOf<Detection?>(null)
    }
    val detections = remember(tripId, previousTrips) {
        buildList {
            previousTrips.forEach { trip ->
                addAll(dbManager.getDetectionsForTrip(trip.tripId))
            }

            if (tripId != null && none { it.tripId == tripId }) {
                addAll(dbManager.getDetectionsForTrip(tripId))
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Trips: ${previousTrips.size} | Detections: ${detections.size}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (detections.isEmpty()) {

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "No detections available yet",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            return@Column
        }
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    onCreate(null)
                    getMapAsync { map ->
                        setupMap(map, detections) { detection ->
                            selectedDetection = detection
                        }
                    }
                }
            },
            update = { mapView ->
                mapView.getMapAsync { map ->
                    setupMap(map, detections) { detection ->
                        selectedDetection = detection
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        selectedDetection?.let { detection ->

            AlertDialog(
                onDismissRequest = {
                    selectedDetection = null
                },

                title = {
                    Text("Detection Details")
                },

                text = {
                    Column {

                        val bitmap = remember(detection.imagePath) {
                            try {
                                BitmapFactory.decodeFile(detection.imagePath)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        bitmap?.let {

                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Detection Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Text("Property Type: ${detection.propertyType}")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Confidence: ${String.format("%.0f", detection.confidence * 100)}%",
                            color = when {
                                detection.confidence > 0.7f -> MaterialTheme.colorScheme.primary
                                detection.confidence > 0.4f -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Latitude: ${detection.latitude}")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Longitude: ${detection.longitude}")

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Timestamp: ${Date(detection.timestamp)}")
                    }
                },

                confirmButton = {
                    Button(
                        onClick = {
                            selectedDetection = null
                        }
                    ) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

private fun setupMap(
    map: GoogleMap,
    detections: List<Detection>,
    onMarkerClick: (Detection) -> Unit
) {
    map.clear()

    if (detections.isEmpty()) return

    detections.forEach { detection ->
        val position = LatLng(detection.latitude, detection.longitude)
        val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        val time = dateFormat.format(java.util.Date(detection.timestamp))

        val marker = map.addMarker(
            MarkerOptions()
                .position(position)
                .title(detection.propertyType)
                .snippet("Tap for full details")
        )
        marker?.tag = detection
    }

    map.setOnMarkerClickListener { marker ->

        val detection = marker.tag as? Detection

        if (detection != null) {
            onMarkerClick(detection)
        }

        false
    }

    // Move camera to first detection
    val first = detections.first()
    map.moveCamera(
        CameraUpdateFactory.newLatLngZoom(
            LatLng(first.latitude, first.longitude), 15f
        )
    )
}