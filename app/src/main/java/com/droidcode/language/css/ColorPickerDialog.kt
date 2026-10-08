package com.droidcode.language.css

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

enum class ColorOutputFormat {
    HEX, RGB
}

private val PRESET_PALETTE = listOf(
    // Material & Web Colors
    0xFFF44336.toInt(), // Red
    0xFFE91E63.toInt(), // Pink
    0xFF9C27B0.toInt(), // Purple
    0xFF673AB7.toInt(), // Deep Purple
    0xFF3F51B5.toInt(), // Indigo
    0xFF2196F3.toInt(), // Blue
    0xFF03A9F4.toInt(), // Light Blue
    0xFF00BCD4.toInt(), // Cyan
    0xFF009688.toInt(), // Teal
    0xFF4CAF50.toInt(), // Green
    0xFF8BC34A.toInt(), // Light Green
    0xFFCDDC39.toInt(), // Lime
    0xFFFFEB3B.toInt(), // Yellow
    0xFFFFC107.toInt(), // Amber
    0xFFFF9800.toInt(), // Orange
    0xFFFF5722.toInt(), // Deep Orange
    0xFF795548.toInt(), // Brown
    0xFF9E9E9E.toInt(), // Grey
    0xFF607D8B.toInt(), // Blue Grey
    0xFF000000.toInt(), // Black
    0xFFFFFFFF.toInt()  // White
)

@Composable
fun ColorPickerDialog(
    initialColor: Int = 0xFF2196F3.toInt(),
    onColorSelected: (String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val hsv = remember(initialColor) {
        val array = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor, array)
        array
    }

    var hue by remember { mutableStateOf(hsv[0]) }
    var saturation by remember { mutableStateOf(hsv[1]) }
    var value by remember { mutableStateOf(hsv[2]) }
    var alpha by remember { mutableStateOf(android.graphics.Color.alpha(initialColor) / 255f) }
    var format by remember { mutableStateOf(ColorOutputFormat.HEX) }

    val currentColorInt = remember(hue, saturation, value, alpha) {
        val rgb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        val a = (alpha * 255f).toInt().coerceIn(0, 255)
        android.graphics.Color.argb(a, android.graphics.Color.red(rgb), android.graphics.Color.green(rgb), android.graphics.Color.blue(rgb))
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CSS Color Picker",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Preview Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(currentColorInt))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val hexText = CssColorHelper.toHexString(currentColorInt, includeAlpha = alpha < 1f)
                        val rgbText = CssColorHelper.toRgbString(currentColorInt)

                        Text(
                            text = if (format == ColorOutputFormat.HEX) hexText else rgbText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row {
                            FilterChip(
                                selected = format == ColorOutputFormat.HEX,
                                onClick = { format = ColorOutputFormat.HEX },
                                label = { Text("HEX", fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = format == ColorOutputFormat.RGB,
                                onClick = { format = ColorOutputFormat.RGB },
                                label = { Text("RGB", fontSize = 11.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sliders: Hue
                Text("Hue: ${hue.toInt()}°", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    modifier = Modifier.height(28.dp)
                )

                // Sliders: Saturation
                Text("Saturation: ${(saturation * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.height(28.dp)
                )

                // Sliders: Value / Brightness
                Text("Brightness: ${(value * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.height(28.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Palette Presets
                Text("Palette Presets", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(PRESET_PALETTE) { presetColor ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(presetColor))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                .clickable {
                                    val arr = FloatArray(3)
                                    android.graphics.Color.colorToHSV(presetColor, arr)
                                    hue = arr[0]
                                    saturation = arr[1]
                                    value = arr[2]
                                    alpha = 1f
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val result = if (format == ColorOutputFormat.HEX) {
                                CssColorHelper.toHexString(currentColorInt, includeAlpha = alpha < 1f)
                            } else {
                                CssColorHelper.toRgbString(currentColorInt)
                            }
                            onColorSelected(result)
                            onDismissRequest()
                        }
                    ) {
                        Text("Apply Color")
                    }
                }
            }
        }
    }
}
