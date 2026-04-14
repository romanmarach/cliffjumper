package com.example.groupproject_m2

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.pow

private sealed interface PressureCaptureResult {
    data class Success(val pressureHpa: Float) : PressureCaptureResult
    data object SensorNotAvailable : PressureCaptureResult
    data object FailedToRead : PressureCaptureResult
}

@Composable
fun PressureMeasurementScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pressure1 by rememberSaveable { mutableStateOf<Float?>(null) }
    var pressure2 by rememberSaveable { mutableStateOf<Float?>(null) }
    var isCapturing by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    val heightMeters = calculateHeightMeters(pressure1, pressure2)
    val heightFeet = heightMeters?.times(3.28084)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Pressure Height Tool",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Capture at the top first, then at the bottom to estimate cliff height.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )

        PressureValueCard(
            title = "Pressure 1 (Location 1)",
            value = pressure1?.let { "${"%.2f".format(it)} hPa" } ?: "--"
        )
        PressureValueCard(
            title = "Pressure 2 (Location 2)",
            value = pressure2?.let { "${"%.2f".format(it)} hPa" } ?: "--"
        )
        PressureValueCard(
            title = "Estimated Height",
            value = if (heightMeters != null && heightFeet != null) {
                "${"%.2f".format(heightMeters)} m (${"%.2f".format(heightFeet)} ft)"
            } else {
                "--"
            }
        )

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Button(
            onClick = {
                scope.launch {
                    isCapturing = true
                    errorMessage = null
                    when (val result = capturePressureOnce(context)) {
                        is PressureCaptureResult.Success -> {
                            if (pressure1 == null) {
                                pressure1 = result.pressureHpa
                            } else {
                                pressure2 = result.pressureHpa
                            }
                        }
                        PressureCaptureResult.SensorNotAvailable -> {
                            errorMessage = "This device does not support a pressure sensor."
                        }
                        PressureCaptureResult.FailedToRead -> {
                            errorMessage = "Could not read pressure. Please try again."
                        }
                    }
                    isCapturing = false
                }
            },
            enabled = !isCapturing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isCapturing) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            } else {
                Text(
                    if (pressure1 == null) {
                        "Take pressure at location 1"
                    } else if (pressure2 == null) {
                        "Take pressure at location 2"
                    } else {
                        "Retake pressure at location 2"
                    }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    pressure1 = null
                    pressure2 = null
                    errorMessage = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset")
            }
        }
    }
}

@Composable
private fun PressureValueCard(
    title: String,
    value: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun calculateHeightMeters(pressure1: Float?, pressure2: Float?): Double? {
    if (pressure1 == null || pressure2 == null || pressure1 <= 0f || pressure2 <= 0f) return null

    val deltaHeight = 44330.0 * (1 - (pressure2 / pressure1).toDouble().pow(0.1903))
    return abs(deltaHeight)
}

private suspend fun capturePressureOnce(context: Context): PressureCaptureResult {
    return withContext<PressureCaptureResult>(Dispatchers.Main) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
            ?: return@withContext PressureCaptureResult.SensorNotAvailable

        suspendCancellableCoroutine<PressureCaptureResult> { continuation ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val pressure = event?.values?.firstOrNull()
                    sensorManager.unregisterListener(this)
                    if (!continuation.isCompleted && pressure != null) {
                        continuation.resumeWith(Result.success(PressureCaptureResult.Success(pressure)))
                    } else if (!continuation.isCompleted) {
                        continuation.resumeWith(Result.success(PressureCaptureResult.FailedToRead))
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            val registered = sensorManager.registerListener(
                listener,
                pressureSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            if (!registered) {
                sensorManager.unregisterListener(listener)
                if (!continuation.isCompleted) {
                    continuation.resumeWith(Result.success(PressureCaptureResult.FailedToRead))
                }
            }

            continuation.invokeOnCancellation {
                sensorManager.unregisterListener(listener)
            }
        }
    }
}


