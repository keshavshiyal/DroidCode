package com.droidcode.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.droidcode.BuildConfig
import com.droidcode.R
import com.droidcode.debug.StackTraceManager
import com.droidcode.settings.AppSettings
import com.droidcode.settings.SettingsManager
import com.droidcode.ui.theme.CornerMedium
import com.droidcode.ui.theme.CornerSmall
import com.droidcode.ui.theme.SpacingL
import com.droidcode.ui.theme.SpacingM
import com.droidcode.ui.theme.SpacingS
import com.droidcode.ui.theme.SpacingXS

enum class SettingsCategory(val label: String, val icon: ImageVector) {
    APPEARANCE("Appearance", Icons.Default.Palette),
    EDITOR("Editor", Icons.Default.Code),
    QUICK_KEY_BAR("Quick Key Bar", Icons.Default.Keyboard),
    GENERAL("General", Icons.Default.Tune),
    ABOUT("About DroidCode", Icons.Default.Info)
}

@Composable
fun SettingsView(
    onBack: () -> Unit,
    onSettingsChanged: () -> Unit
) {
    val context = LocalContext.current
    val settingsMgr = remember { SettingsManager.getInstance(context) }
    val settings = settingsMgr.settings

    var selectedCategory by rememberSaveable { mutableStateOf(SettingsCategory.APPEARANCE) }
    var showStackTraceDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                .padding(horizontal = SpacingM),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("settings_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(SpacingS))
                Column {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Top Category Scrollable Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedCategory.ordinal,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            }
        ) {
            SettingsCategory.values().forEach { category ->
                val isSelected = category == selectedCategory
                Tab(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = category.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }

        // Category Content Details Pane (Full Width)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                when (selectedCategory) {
                    SettingsCategory.APPEARANCE -> AppearanceSettingsSection(settings, settingsMgr, context, onSettingsChanged)
                    SettingsCategory.EDITOR -> EditorSettingsSection(settings, settingsMgr, context, onSettingsChanged)
                    SettingsCategory.QUICK_KEY_BAR -> KeyBarSettingsSection(settings, settingsMgr, context, onSettingsChanged)
                    SettingsCategory.ABOUT -> AboutSection(onShowStackTrace = { showStackTraceDialog = true })
                    else -> GeneralSettingsSection(onShowStackTrace = { showStackTraceDialog = true })
                }
            }
        }
    }

    if (showStackTraceDialog) {
        StackTraceViewerDialog(onDismiss = { showStackTraceDialog = false })
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
            text = "Appearance & Theme",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Select your preferred color scheme for the DroidCode workstation",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        AppSettings.ThemeMode.values().forEach { mode ->
            val isSelected = settings.themeMode == mode
            val modeIcon = when (mode) {
                AppSettings.ThemeMode.DARK -> Icons.Default.DarkMode
                AppSettings.ThemeMode.ONE_DARK -> Icons.Default.DarkMode
                AppSettings.ThemeMode.DRACULA -> Icons.Default.DarkMode
                AppSettings.ThemeMode.MONOKAI -> Icons.Default.DarkMode
                AppSettings.ThemeMode.SOLARIZED_DARK -> Icons.Default.DarkMode
                AppSettings.ThemeMode.NORD -> Icons.Default.DarkMode
                AppSettings.ThemeMode.LIGHT -> Icons.Default.LightMode
                AppSettings.ThemeMode.SOLARIZED_LIGHT -> Icons.Default.LightMode
                AppSettings.ThemeMode.SYSTEM -> Icons.Default.SettingsSystemDaydream
            }
            val modeDesc = when (mode) {
                AppSettings.ThemeMode.DARK -> "Modern high-contrast dark theme with sharp syntax highlighting"
                AppSettings.ThemeMode.ONE_DARK -> "Deep midnight dark palette with vibrant accents and soft dark canvas"
                AppSettings.ThemeMode.DRACULA -> "Rich twilight purple, pink, and cyan accents on a dark velvet canvas"
                AppSettings.ThemeMode.MONOKAI -> "High-contrast vivid colors on deep charcoal background"
                AppSettings.ThemeMode.SOLARIZED_DARK -> "Ergonomic low-blue-light palette designed for eye protection & fatigue reduction"
                AppSettings.ThemeMode.NORD -> "Arctic bluish-gray palette with gentle, balanced contrast"
                AppSettings.ThemeMode.LIGHT -> "Clean, crisp light canvas with sharp syntax highlighting"
                AppSettings.ThemeMode.SOLARIZED_LIGHT -> "Warm sepia paper-tone palette for comfortable daytime reading"
                AppSettings.ThemeMode.SYSTEM -> "Automatically synchronize theme with Android system preferences"
            }

            OutlinedCard(
                onClick = {
                    settings.themeMode = mode
                    settingsMgr.saveSettings(context)
                    onSettingsChanged()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            settings.themeMode = mode
                            settingsMgr.saveSettings(context)
                            onSettingsChanged()
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = modeIcon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = mode.label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Palette Color Dots Preview
                            val palettePreview = when (mode) {
                                AppSettings.ThemeMode.DARK -> listOf(Color(0xFF1E1E1E), Color(0xFF0E639C), Color(0xFF4EC9B0), Color(0xFFDCDCAA))
                                AppSettings.ThemeMode.ONE_DARK -> listOf(Color(0xFF282C34), Color(0xFF61AFEF), Color(0xFF98C379), Color(0xFFC678DD))
                                AppSettings.ThemeMode.DRACULA -> listOf(Color(0xFF282A36), Color(0xFFBD93F9), Color(0xFFFF79C6), Color(0xFF50FA7B))
                                AppSettings.ThemeMode.MONOKAI -> listOf(Color(0xFF272822), Color(0xFFF92672), Color(0xFFA6E22E), Color(0xFF66D9EF))
                                AppSettings.ThemeMode.SOLARIZED_DARK -> listOf(Color(0xFF002B36), Color(0xFF268BD2), Color(0xFF2AA198), Color(0xFF859900))
                                AppSettings.ThemeMode.NORD -> listOf(Color(0xFF2E3440), Color(0xFF88C0D0), Color(0xFFA3BE8C), Color(0xFF81A1C1))
                                AppSettings.ThemeMode.LIGHT -> listOf(Color(0xFFFFFFFF), Color(0xFF007ACC), Color(0xFF098658), Color(0xFFAF00DB))
                                AppSettings.ThemeMode.SOLARIZED_LIGHT -> listOf(Color(0xFFFDF6E3), Color(0xFF268BD2), Color(0xFF2AA198), Color(0xFFB58900))
                                AppSettings.ThemeMode.SYSTEM -> listOf(Color(0xFF1E1E1E), Color(0xFFFFFFFF), Color(0xFF0E639C))
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                palettePreview.forEach { c ->
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .background(c, CircleShape)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = modeDesc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
    var selectedFont by remember { mutableStateOf(settings.editorFontFamily) }

    Column {
        Text(
            text = "Editor Preferences",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Customize code fonts, font size, line numbers, and line wrapping",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        // Font Size Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Font Size",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${fontSize.toInt()} sp",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = EditorFontHelper.getFontFamily(selectedFont),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

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

                // Code Preview Box
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    val activeFontFamily = EditorFontHelper.getFontFamily(selectedFont)
                    Row(modifier = Modifier.padding(12.dp)) {
                        if (lineNumbers) {
                            Text(
                                text = "1\n2\n3",
                                fontSize = fontSize.sp,
                                fontFamily = activeFontFamily,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                lineHeight = (fontSize * 1.4).sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                        Text(
                            text = "fun main() {\n    println(\"Hello DroidCode!\")\n}",
                            fontSize = fontSize.sp,
                            fontFamily = activeFontFamily,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = (fontSize * 1.4).sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Code Font Family Selection
        Text(
            text = "Code Font Family",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        AppSettings.EditorFontFamily.values().forEach { fontOption ->
            val isSelected = selectedFont == fontOption
            val optionFont = EditorFontHelper.getFontFamily(fontOption)

            OutlinedCard(
                onClick = {
                    selectedFont = fontOption
                    settings.editorFontFamily = fontOption
                    settingsMgr.saveSettings(context)
                    onSettingsChanged()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("font_option_${fontOption.name.lowercase()}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                selectedFont = fontOption
                                settings.editorFontFamily = fontOption
                                settingsMgr.saveSettings(context)
                                onSettingsChanged()
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fontOption.label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = fontOption.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Live sample rendered in this specific font
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 42.dp)
                    ) {
                        Text(
                            text = "val code = \"DroidCode\" // => 100% Kotlin",
                            fontSize = 12.sp,
                            fontFamily = optionFont,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Line Numbers Switch
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show Line Numbers",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Display vertical gutter line numbering beside editor lines",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Word Wrap Switch
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Word Wrap",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Wrap long code lines within editor viewport instead of horizontal scrolling",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
}

@Composable
private fun KeyBarSettingsSection(
    settings: AppSettings,
    settingsMgr: SettingsManager,
    context: android.content.Context,
    onSettingsChanged: () -> Unit
) {
    var keyBarEnable by remember { mutableStateOf(settings.isQuickKeyBarEnabled) }
    var selectedDensity by remember { mutableStateOf(settings.quickKeyBarDensity) }

    Column {
        Text(
            text = "Developer Quick Key Bar",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Configure the touch key bar displayed above the soft keyboard",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        // Enable Toggle
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Quick Key Bar",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Quick access bar for Ctrl, Shift, Alt, brackets, symbols, arrows and Menu",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Density Height Selection
        Text(
            text = "Bar Height & Density",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        AppSettings.KeyBarDensity.values().forEach { density ->
            val isSelected = selectedDensity == density

            val densityDesc = when (density) {
                AppSettings.KeyBarDensity.COMPACT -> "Compact 36 dp - Saves maximum editor space, smaller touch targets"
                AppSettings.KeyBarDensity.NORMAL -> "Normal 44 dp - Standard balanced mobile coding bar"
                AppSettings.KeyBarDensity.COMFORTABLE -> "Comfortable 52 dp - Larger touch targets for tablets or easier typing"
            }

            OutlinedCard(
                onClick = {
                    selectedDensity = density
                    settings.quickKeyBarDensity = density
                    settingsMgr.saveSettings(context)
                    onSettingsChanged()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            selectedDensity = density
                            settings.quickKeyBarDensity = density
                            settingsMgr.saveSettings(context)
                            onSettingsChanged()
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${density.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} (${density.heightDp} dp)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = densityDesc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Live Interactive Key Bar Preview
        if (keyBarEnable) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Live Key Bar Preview",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Current Size: ${selectedDensity.name} (${selectedDensity.heightDp} dp)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    DeveloperQuickKeyBar(
                        density = selectedDensity,
                        ctrlActive = false,
                        shiftActive = false,
                        altActive = false,
                        onToggleCtrl = {},
                        onToggleShift = {},
                        onToggleAlt = {},
                        onInsertText = {
                            Toast.makeText(context, "Key inserted: $it", Toast.LENGTH_SHORT).show()
                        },
                        onActionKey = {
                            Toast.makeText(context, "Action: $it", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralSettingsSection(onShowStackTrace: () -> Unit = {}) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val hasTraces = StackTraceManager.hasStackTraces()
    val traceCount = StackTraceManager.getAllStackTraces().size

    Column {
        Text(
            text = "General System Information",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Offline-first native workspace engine running on Android",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
        )

        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Storage Access Framework (SAF)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Direct file system sync enabled for device internal storage and cloud locations.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Diagnostic Stack Trace & Logs Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Diagnostics & Stack Traces",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (hasTraces) {
                        "Recorded crash/error stack traces available ($traceCount logged). Inspect logs or copy them to debug app behavior."
                    } else {
                        "No application crashes recorded. System diagnostic snapshots and environment specs are available for debugging."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onShowStackTrace,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_view_stacktrace_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Stack Trace", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val text = StackTraceManager.getLatestStackTrace()
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "Diagnostic stack trace copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("settings_copy_diagnostics_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Diagnostics",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutSection(onShowStackTrace: () -> Unit = {}) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val githubUrl = "https://github.com/keshavshiyal"

    Column {
        Text(
            text = "About DroidCode Workstation",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Developer Information Card (Keshav Shiyal)
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "KS",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Keshav Shiyal",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Creator & Lead Android Developer",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Passionate mobile software developer creating high-performance developer tools, native IDEs, and offline-first Android applications.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // GitHub Profile Details
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GitHub: github.com/keshavshiyal",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("about_open_github_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GitHub Profile", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(githubUrl))
                            Toast.makeText(context, "GitHub URL copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("about_copy_github_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy GitHub URL",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Application Technical Info Card
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "<D/> DroidCode Workstation",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Professional open-source native Android IDE workstation powered by Kotlin, Java, and Jetpack Compose.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onShowStackTrace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_view_stacktrace_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Stack Trace & System Diagnostics", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StackTraceViewerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var traces by remember { mutableStateOf(StackTraceManager.getAllStackTraces()) }
    var selectedIndex by remember { mutableIntStateOf(0) }

    val currentContent = when {
        traces.isNotEmpty() && selectedIndex in traces.indices -> traces[selectedIndex]
        traces.isNotEmpty() -> traces.first()
        else -> StackTraceManager.getSystemDiagnostics()
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "App Stack Trace & Diagnostics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (traces.isNotEmpty()) {
                                    "Log ${selectedIndex + 1} of ${traces.size} recorded entries"
                                } else {
                                    "System Diagnostic Snapshot (Healthy)"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_close_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // If multiple traces exist, show navigation bar
                if (traces.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (selectedIndex > 0) {
                                    selectedIndex--
                                }
                            },
                            enabled = selectedIndex > 0
                        ) {
                            Text("< Newer", fontSize = 11.sp)
                        }

                        Text(
                            text = "Log ${selectedIndex + 1} of ${traces.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = {
                                if (selectedIndex < traces.size - 1) {
                                    selectedIndex++
                                }
                            },
                            enabled = selectedIndex < traces.size - 1
                        ) {
                            Text("Older >", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Monospace Content View
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = currentContent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .verticalScroll(verticalScroll)
                                    .horizontalScroll(horizontalScroll)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action Buttons at Bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy to Clipboard (Primary Button)
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentContent))
                            Toast.makeText(context, "Stack trace copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("dialog_copy_stacktrace_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy to Clipboard", fontSize = 12.sp, maxLines = 1)
                    }

                    // Test Log / Error Generator
                    OutlinedButton(
                        onClick = {
                            StackTraceManager.recordTestException()
                            traces = StackTraceManager.getAllStackTraces()
                            selectedIndex = 0
                            Toast.makeText(context, "Test stack trace recorded!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("dialog_test_stacktrace_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test Log", fontSize = 11.sp, maxLines = 1)
                    }

                    // Clear Traces
                    if (traces.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                StackTraceManager.clearStackTraces()
                                traces = StackTraceManager.getAllStackTraces()
                                selectedIndex = 0
                                Toast.makeText(context, "Diagnostic logs cleared", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("dialog_clear_stacktrace_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear logs",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

