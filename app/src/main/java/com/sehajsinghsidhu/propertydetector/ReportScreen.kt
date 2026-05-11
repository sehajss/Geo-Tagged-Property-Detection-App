package com.sehajsinghsidhu.propertydetector

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider

@Composable
fun ReportScreen(
    tripId: String?,
    dbManager: DatabaseManager,
    reportGenerator: ReportGenerator,
    previousTrips: List<Trip>
) {
    val context = LocalContext.current
    val tripList = remember { mutableStateListOf<Trip>() }

    var tripToDelete by remember {
        mutableStateOf<Trip?>(null)
    }

    // Remove previousTrips synchronization logic. Instead, refresh tripList when tripId changes.
    LaunchedEffect(tripId) {

        val trips = dbManager.getAllTrips()

        android.util.Log.d("ReportDebug", "Trips loaded: ${trips.size}")

        tripList.clear()
        tripList.addAll(trips)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Trip Report",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                if (tripList.isEmpty()) {

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "No trips recorded yet",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                tripList.toList().forEachIndexed { index, trip ->

                    val detections = dbManager.getDetectionsForTrip(trip.tripId)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {

                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Text(
                                text = "Trip ${index + 1} • ${detections.size} detections",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text("Trip ID: ${trip.tripId}")
                            Text("Detections: ${detections.size}")
                            Text("Distance: ${trip.distance} km")
                            Text("Duration: ${trip.duration} ms")
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {

                                    val file = reportGenerator.generateCSV(trip, detections)

                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )

                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }

                                    context.startActivity(
                                        Intent.createChooser(intent, "Export CSV")
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Export CSV")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {

                                    val file = reportGenerator.generatePDF(trip, detections)

                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )

                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }

                                    context.startActivity(
                                        Intent.createChooser(intent, "Export PDF")
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Export PDF")
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    tripToDelete = trip
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Delete Trip")
                            }
                        }
                    }
                }
            }
        }

        tripToDelete?.let { trip ->

            AlertDialog(

                onDismissRequest = {
                    tripToDelete = null
                },

                title = {
                    Text("Delete Trip")
                },

                text = {
                    Text("Are you sure you want to delete this trip?")
                },

                confirmButton = {

                    Button(
                        onClick = {

                            dbManager.deleteTrip(trip.tripId)
                            tripList.remove(trip)

                            tripToDelete = null
                        }
                    ) {
                        Text("Delete")
                    }
                },

                dismissButton = {

                    OutlinedButton(
                        onClick = {
                            tripToDelete = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (tripId == null) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Complete a trip first to see the report",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}