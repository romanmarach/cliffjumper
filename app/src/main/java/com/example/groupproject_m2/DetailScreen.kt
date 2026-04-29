package com.example.groupproject_m2

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import kotlin.math.sqrt

data class WeatherData(
    val temperature: Double,
    val windSpeed: Double,
    val description: String,
    val feelsLike: Double
)

data class SafetyScore(
    val score: Int,
    val label: String,
    val color: Color
)

fun calculateFallTime(heightFt: Double): Double {
    val heightM = heightFt * 0.3048
    return sqrt(2 * heightM / 9.81)
}

fun calculateImpactSpeed(heightFt: Double): Double {
    val heightM = heightFt * 0.3048
    val speedMs = sqrt(2 * 9.81 * heightM)
    return speedMs * 2.237
}

fun calculateSafetyScore(
    windSpeed: Double,
    temperature: Double,
    heightFt: Double
): SafetyScore {
    var score = 0
    score += when {
        windSpeed < 10 -> 33
        windSpeed < 20 -> 17
        else -> 0
    }
    score += when {
        temperature in 65.0..95.0 -> 33
        temperature in 50.0..64.9 -> 17
        else -> 0
    }
    score += when {
        heightFt < 25 -> 34
        heightFt < 50 -> 17
        else -> 0
    }
    return when {
        score >= 90 -> SafetyScore(score, "Safe to Jump", Color(0xFF2E7D32))
        score >= 50 -> SafetyScore(score, "Jump with Caution", Color(0xFFF9A825))
        else -> SafetyScore(score, "Do Not Jump", Color(0xFFC62828))
    }
}

suspend fun fetchWeather(lat: Double, lng: Double, apiKey: String): WeatherData? {
    return withContext(Dispatchers.IO) {
        try {
            val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lng&appid=$apiKey&units=imperial"
            val response = URL(url).readText()
            val json = JSONObject(response)
            val main = json.getJSONObject("main")
            val wind = json.getJSONObject("wind")
            val weather = json.getJSONArray("weather").getJSONObject(0)
            WeatherData(
                temperature = main.getDouble("temp"),
                windSpeed = wind.getDouble("speed"),
                description = weather.getString("description"),
                feelsLike = main.getDouble("feels_like")
            )
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    spot: CliffSpot,
    weatherApiKey: String,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onBackClick: () -> Unit
) {
    var weatherData by remember { mutableStateOf<WeatherData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val heightFt = spot.height.replace(" ft", "").toDoubleOrNull() ?: 0.0

    LaunchedEffect(spot.coordinates) {
        weatherData = fetchWeather(spot.coordinates.latitude, spot.coordinates.longitude, weatherApiKey)
        isLoading = false
    }

    val safetyScore = weatherData?.let { calculateSafetyScore(it.windSpeed, it.temperature, heightFt) }
    val fallTime = calculateFallTime(heightFt)
    val impactSpeed = calculateImpactSpeed(heightFt)

    val difficultyColor = when (spot.difficulty) {
        "Beginner" -> Color(0xFF2E7D32)
        "Intermediate" -> Color(0xFFF9A825)
        "Advanced" -> Color(0xFFC62828)
        else -> Color.Gray
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(spot.name, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleLike) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save",
                            tint = if (isLiked) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(spot.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, textAlign = TextAlign.Center)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                        Text(spot.location, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(difficultyColor.copy(alpha = 0.85f))
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(spot.difficulty, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Height Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Cliff Height", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(spot.height, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                // Safety Score Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SAFETY ASSESSMENT", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(28.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        } else if (safetyScore != null) {
                            Text(safetyScore.label, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = safetyScore.color)
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { safetyScore.score / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = safetyScore.color,
                                trackColor = safetyScore.color.copy(alpha = 0.15f)
                            )
                            Text("${safetyScore.score} / 100", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
                        } else {
                            Text("Loading safety data...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 14.sp)
                        }
                    }
                }

                // Weather Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("CURRENT CONDITIONS", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(28.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                        } else if (weatherData != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Thermostat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Text("${weatherData!!.temperature.toInt()}°F", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Temperature", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                HorizontalDivider(modifier = Modifier.height(60.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Air, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Text("${weatherData!!.windSpeed.toInt()} mph", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Wind Speed", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                HorizontalDivider(modifier = Modifier.height(60.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Text("${weatherData!!.feelsLike.toInt()}°F", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Feels Like", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(weatherData!!.description.replaceFirstChar { it.uppercase() }, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            Text("Loading weather data...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                }

                // Physics Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("PHYSICS", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("%.1f sec".format(fallTime), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                Text("Fall Time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
                            }
                            HorizontalDivider(modifier = Modifier.height(50.dp).width(1.dp), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("%.1f mph".format(impactSpeed), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                                Text("Impact Speed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("t = √(2h/g)   ·   v = √(2gh)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), modifier = Modifier.align(Alignment.CenterHorizontally), letterSpacing = 1.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

