package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.settings.AppSettings
import com.example.settings.SettingsManager

enum class SettingsCategory(val label: String) {
    GENERAL("General"),
    APPEARANCE("Appearance"),
    EDITOR("Editor"),
    QUICK_KEY_BAR("Quick Key Bar"),
    ABOUT("About DroidCode")
}

@Composable
fun SettingsView(
    onBack: () -> Unit,
    onSettingsChanged: () -> Unit
) {
    val context = LocalContext.current
    val settingsMgr = remember { SettingsManager.getInstance(context) }
    val settings = settingsMgr.settings

    var selectedCategory by remember { mutableStateOf(SettingsCategory.APPEARANCE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Settings Body Split
        Row(modifier = Modifier.fillMaxSize()) {
            // Category Sidebar
            Column(
                modifier = Modifier
                    .width(160.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                SettingsCategory.values().forEach { category ->
                    val isSelected = category == selectedCategory
                    val bg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg)
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = category.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Category Details Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                when (selectedCategory) {
                    SettingsCategory.APPEARANCE -> AppearanceSettingsSection(settings, settingsMgr, context, onSettingsChanged)
                    SettingsCategory.EDITOR -> EditorSettingsSection(settings, settingsMgr, context, onSettingsChanged)
                    SettingsCategory.QUICK_KEY_BAR -> KeyBarSettingsSection(settings, settingsMgr, context, onSettingsChanged)
                    SettingsCategory.ABOUT -> AboutSection()
                    else -> GeneralSettingsSection()
                }
            }
        }
    }
}

@Composable
private fun AppearanceSettingsSection(
    settings: AppSettings,
    settingsMgr: SettingsManager,
    context: android.content.Context,
    onSettingsChanged: () -> Unit
) {
    Column {
        Text(
            text = "Color Theme",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppSettings.ThemeMode.values().forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        settings.themeMode = mode
                        settingsMgr.saveSettings(context)
                        onSettingsChanged()
                    }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = settings.themeMode == mode,
                    onClick = {
                        settings.themeMode = mode
                        settingsMgr.saveSettings(context)
                        onSettingsChanged()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mode.label,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun EditorSettingsSection(
    settings: AppSettings,
    settingsMgr: SettingsManager,
    context: android.content.Context,
    onSettingsChanged: () -> Unit
) {
    var fontSize by remember { mutableStateOf(settings.fontSizeSp.toFloat()) }
    var wordWrap by remember { mutableStateOf(settings.isWordWrap) }
    var lineNumbers by remember { mutableStateOf(settings.isLineNumbersEnabled) }

    Column {
        Text(
            text = "Editor Font Size (${fontSize.toInt()} sp)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Slider(
            value = fontSize,
            onValueChange = {
                fontSize = it
                settings.fontSizeSp = it.toInt()
                settingsMgr.saveSettings(context)
                onSettingsChanged()
            },
            valueRange = 10f..24f,
            steps = 14,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Show Line Numbers",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = lineNumbers,
                onCheckedChange = {
                    lineNumbers = it
                    settings.isLineNumbersEnabled = it
                    settingsMgr.saveSettings(context)
                    onSettingsChanged()
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Word Wrap",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = wordWrap,
                onCheckedChange = {
                    wordWrap = it
                    settings.isWordWrap = it
                    settingsMgr.saveSettings(context)
                    onSettingsChanged()
                }
            )
        }
    }
}

@Composable
private fun KeyBarSettingsSection(
    settings: AppSettings,
    settingsMgr: SettingsManager,
    context: android.content.Context,
    onSettingsChanged: () -> Unit
) {
    var keyBarEnable by remember { mutableStateOf(settings.isQuickKeyBarEnabled) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enable Quick Key Bar",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = keyBarEnable,
                onCheckedChange = {
                    keyBarEnable = it
                    settings.isQuickKeyBarEnabled = it
                    settingsMgr.saveSettings(context)
                    onSettingsChanged()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Bar Density / Height",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        AppSettings.KeyBarDensity.values().forEach { density ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        settings.quickKeyBarDensity = density
                        settingsMgr.saveSettings(context)
                        onSettingsChanged()
                    }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = settings.quickKeyBarDensity == density,
                    onClick = {
                        settings.quickKeyBarDensity = density
                        settingsMgr.saveSettings(context)
                        onSettingsChanged()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${density.name} (${density.heightDp} dp)",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun GeneralSettingsSection() {
    Column {
        Text(
            text = "General Environment",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Offline-first native storage and SAF permissions active.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun AboutSection() {
    Column {
        Text(
            text = "<D/> DroidCode Workstation",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Application ID: ${BuildConfig.APPLICATION_ID}",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        Text(
            text = "Build Type: ${BuildConfig.BUILD_TYPE}",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Open Source Native Android IDE built with Java & Jetpack Compose.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
