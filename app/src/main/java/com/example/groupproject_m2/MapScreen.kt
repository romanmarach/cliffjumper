package com.example.groupproject_m2

import android.Manifest
import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

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
    return results[0] / 1609.34f
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(onSpotClick: (CliffSpot) -> Unit = {}) {
    var isMapView by remember { mutableStateOf(true) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = LocalContext.current

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(Unit) {
        locationPermission.launchPermissionRequest()
    }

    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L
            ).build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                android.os.Looper.getMainLooper()
            )
        }
    }

    val sortedSpots = remember(userLocation) {
        userLocation?.let { loc ->
            cliffSpots.sortedBy { spot ->
                distanceBetween(loc.latitude, loc.longitude, spot.coordinates.latitude, spot.coordinates.longitude)
            }
        } ?: cliffSpots
    }

    Box(modifier = Modifier.fillMaxSize()) {

        if (isMapView) {
            MapView(locationPermission.status.isGranted, onSpotClick)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                ListView(sortedSpots, userLocation, onSpotClick)
            }
        }

        // Floating segmented toggle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                    .padding(4.dp)
            ) {
                Row {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (isMapView) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .padding(horizontal = 28.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { isMapView = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "Map",
                                color = if (isMapView) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = if (isMapView) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (!isMapView) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .padding(horizontal = 28.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = { isMapView = false },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                "List",
                                color = if (!isMapView) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = if (!isMapView) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapView(hasLocationPermission: Boolean, onSpotClick: (CliffSpot) -> Unit) {
    val usCenter = LatLng(39.5, -98.35)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(usCenter, 4f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = hasLocationPermission,
            zoomControlsEnabled = true
        )
    ) {
        cliffSpots.forEach { spot ->
            Marker(
                state = MarkerState(position = spot.coordinates),
                title = spot.name,
                snippet = "${spot.location} • ${spot.height} • ${spot.difficulty}",
                onClick = {
                    it.showInfoWindow()
                    false
                },
                onInfoWindowClick = { onSpotClick(spot) }
            )
        }
    }
}

@Composable
fun ListView(
    spots: List<CliffSpot>,
    userLocation: LatLng?,
    onSpotClick: (CliffSpot) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(spots) { spot ->
            val distance = userLocation?.let {
                distanceBetween(it.latitude, it.longitude, spot.coordinates.latitude, spot.coordinates.longitude)
            }

            val difficultyColor = when (spot.difficulty) {
                "Beginner" -> Color(0xFF2E7D32)
                "Intermediate" -> Color(0xFFF9A825)
                "Advanced" -> Color(0xFFC62828)
                else -> Color.Gray
            }

            Card(
                onClick = { onSpotClick(spot) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(difficultyColor)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = spot.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = spot.location,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${spot.height}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(difficultyColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = spot.difficulty,
                                    fontSize = 12.sp,
                                    color = difficultyColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        distance?.let {
                            Text(
                                text = "%.1f miles away".format(it),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}