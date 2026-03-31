package com.example.groupproject_m2

import android.Manifest
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.compose.ui.platform.LocalContext

data class CliffSpot(
    val name: String,
    val location: String,
    val height: String,
    val difficulty: String,
    val coordinates: LatLng
)

val cliffSpots = listOf(
    CliffSpot("Blue Hole", "Santa Rosa, New Mexico", "25 ft", "Beginner", LatLng(34.9391, -104.6672)),
    CliffSpot("Havasupai Falls", "Grand Canyon, Arizona", "40 ft", "Intermediate", LatLng(36.2553, -112.6980)),
    CliffSpot("Lake Powell", "Page, Arizona", "60 ft", "Advanced", LatLng(36.9380, -111.4879)),
    CliffSpot("Red River Gorge", "Kentucky", "35 ft", "Intermediate", LatLng(37.7926, -83.6813)),
    CliffSpot("Sliding Rock", "Brevard, North Carolina", "20 ft", "Beginner", LatLng(35.2620, -82.8743))
)

fun distanceBetween(userLat: Double, userLng: Double, spotLat: Double, spotLng: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(userLat, userLng, spotLat, spotLng, results)
    return results[0] / 1609.34f // convert meters to miles
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    var isMapView by remember { mutableStateOf(true) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        locationPermission.launchPermissionRequest()
    }

    // Get user location once permission is granted
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    userLocation = LatLng(it.latitude, it.longitude)
                }
            }
        }
    }

    // Sort spots by distance if we have user location
    val sortedSpots = remember(userLocation) {
        userLocation?.let { loc ->
            cliffSpots.sortedBy { spot ->
                distanceBetween(loc.latitude, loc.longitude, spot.coordinates.latitude, spot.coordinates.longitude)
            }
        } ?: cliffSpots
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // Toggle buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = { isMapView = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMapView) MaterialTheme.colorScheme.primary else Color.Gray
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Map View")
            }
            Button(
                onClick = { isMapView = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isMapView) MaterialTheme.colorScheme.primary else Color.Gray
                )
            ) {
                Text("List View")
            }
        }

        // Content
        if (isMapView) {
            MapView(locationPermission.status.isGranted)
        } else {
            ListView(sortedSpots, userLocation)
        }
    }
}

@Composable
fun MapView(hasLocationPermission: Boolean) {
    val usCenter = LatLng(39.5, -98.35)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(usCenter, 4f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = hasLocationPermission
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = hasLocationPermission,
            zoomControlsEnabled = true
        )
    ) {
        cliffSpots.forEach { spot ->
            Marker(
                state = MarkerState(position = spot.coordinates),
                title = spot.name,
                snippet = "${spot.location} • ${spot.height} • ${spot.difficulty}"
            )
        }
    }
}

@Composable
fun ListView(spots: List<CliffSpot>, userLocation: LatLng?) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        items(spots) { spot ->
            val distance = userLocation?.let {
                distanceBetween(it.latitude, it.longitude, spot.coordinates.latitude, spot.coordinates.longitude)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = spot.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = spot.location,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Text(
                            text = "Height: ${spot.height}",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        Text(
                            text = "Difficulty: ${spot.difficulty}",
                            fontSize = 13.sp
                        )
                    }
                    distance?.let {
                        Text(
                            text = "%.1f miles away".format(it),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}