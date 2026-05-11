package com.sehajsinghsidhu.propertydetector

import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sehajsinghsidhu.propertydetector.ui.theme.PropertyDetectorTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasPermissions()) permissionLauncher.launch(permissions)

        setContent {
            PropertyDetectorTheme {
                AppNavigation()
            }
        }
    }

    private fun hasPermissions() = permissions.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("camera") }
    val context = LocalContext.current
    val tripManager = remember { TripManager(context) }
    val dbManager = remember { DatabaseManager(context) }
    val reportGenerator = remember { ReportGenerator(context) }
    var lastTripId by remember { mutableStateOf<String?>(null) }
    var previousTrips by remember { mutableStateOf(dbManager.getAllTrips()) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Camera") },
                    label = { Text("Camera") },
                    selected = currentScreen == "camera",
                    onClick = { currentScreen = "camera" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = "Map") },
                    label = { Text("Map") },
                    selected = currentScreen == "map",
                    onClick = { currentScreen = "map" }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Share, contentDescription = "Report") },
                    label = { Text("Report") },
                    selected = currentScreen == "report",
                    onClick = { currentScreen = "report" }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentScreen) {
                "camera" -> MainScreen(
                    tripManager = tripManager,
                    onTripStopped = { tripId ->
                        lastTripId = tripId
                        previousTrips = dbManager.getAllTrips()
                    }
                )
                "map" -> MapScreen(
                    tripId = lastTripId,
                    dbManager = dbManager,
                    previousTrips = previousTrips
                )
                "report" -> ReportScreen(
                    tripId = lastTripId,
                    dbManager = dbManager,
                    reportGenerator = reportGenerator,
                    previousTrips = previousTrips
                )
            }
        }
    }
}

@Composable
fun MainScreen(
    tripManager: TripManager,
    onTripStopped: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraModule = remember { CameraModule(context) }
    val detectionManager = remember { DetectionManager(context) }
    val dbManager = remember { DatabaseManager(context) }

    var isTripActive by remember { mutableStateOf(false) }
    var detectionCount by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf("Ready") }

    var totalTrips by remember { mutableIntStateOf(dbManager.getAllTrips().size) }
    var totalDetections by remember {
        mutableIntStateOf(
            dbManager.getAllTrips().sumOf {
                dbManager.getDetectionsForTrip(it.tripId).size
            }
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        cameraModule.startCamera(lifecycleOwner, previewView) { imageProxy ->
                            if (isTripActive && tripManager.isVehicleMoving()) {
                                detectionManager.processFrame(imageProxy) { propertyType, confidence, bitmap ->
                                    val imagePath = saveImage(context, bitmap)
                                    tripManager.saveDetection(propertyType, imagePath, confidence)
                                    detectionCount++
                                    statusMessage = "Detected: $propertyType"
                                }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isTripActive) "Trip Active" else "No Active Trip",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Detections: $detectionCount")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = statusMessage)
                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Trips Saved: $totalTrips")

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Total Detections: $totalDetections")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (isTripActive) {
                            val finishedTripId = tripManager.getCurrentTripId()

                            tripManager.stopTrip()

                            isTripActive = false
                            statusMessage = "Trip stopped"

                            finishedTripId?.let {
                                onTripStopped(it)
                            }
                            totalTrips = dbManager.getAllTrips().size
                            totalDetections = dbManager.getAllTrips().sumOf {
                                dbManager.getDetectionsForTrip(it.tripId).size
                            }
                        } else {
                            tripManager.startTrip()
                            isTripActive = true
                            detectionCount = 0
                            statusMessage = "Trip started"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isTripActive)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(if (isTripActive) "Stop Trip" else "Start Trip")
                }
            }
        }
    }
}

fun saveImage(context: Context, bitmap: Bitmap?): String {
    if (bitmap == null) return ""
    return try {
        val file = File(context.filesDir, "detection_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        ""
    }
}