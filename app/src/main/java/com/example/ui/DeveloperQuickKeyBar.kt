package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.AppSettings

@Composable
fun DeveloperQuickKeyBar(
    density: AppSettings.KeyBarDensity,
    ctrlActive: Boolean,
    shiftActive: Boolean,
    altActive: Boolean,
    onToggleCtrl: () -> Unit,
    onToggleShift: () -> Unit,
    onToggleAlt: () -> Unit,
    onInsertText: (String) -> Unit,
    onActionKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val barHeight = density.heightDp.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sticky Modifiers
        ModifierKeyButton("Ctrl", ctrlActive, onToggleCtrl, "quick_key_ctrl")
        ModifierKeyButton("Shift", shiftActive, onToggleShift, "quick_key_shift")
        ModifierKeyButton("Alt", altActive, onToggleAlt, "quick_key_alt")

        // Action Keys
        QuickActionButton("↶", { onActionKey("UNDO") }, "quick_key_undo")
        QuickActionButton("↷", { onActionKey("REDO") }, "quick_key_redo")
        QuickActionButton("Tab", { onInsertText("\t") }, "quick_key_tab")
        QuickActionButton("Esc", { onActionKey("ESC") }, "quick_key_esc")

        // Arrow Keys
        QuickActionButton("←", { onActionKey("LEFT") }, "quick_key_left")
        QuickActionButton("↑", { onActionKey("UP") }, "quick_key_up")
        QuickActionButton("↓", { onActionKey("DOWN") }, "quick_key_down")
        QuickActionButton("→", { onActionKey("RIGHT") }, "quick_key_right")

        // Code Brackets & Symbols with Variations
        SymbolKeyWithVariations("{", listOf("{", "}", "{}"), onInsertText, "quick_key_brace")
        SymbolKeyWithVariations("}", listOf("}", "{"), onInsertText, "quick_key_close_brace")
        SymbolKeyWithVariations("[", listOf("[", "]", "[]"), onInsertText, "quick_key_bracket")
        SymbolKeyWithVariations("]", listOf("]", "["), onInsertText, "quick_key_close_bracket")
        SymbolKeyWithVariations("(", listOf("(", ")", "()"), onInsertText, "quick_key_paren")
        SymbolKeyWithVariations(")", listOf(")", "("), onInsertText, "quick_key_close_paren")
        SymbolKeyWithVariations("<", listOf("<", ">", "</>", "<="), onInsertText, "quick_key_lt")
        SymbolKeyWithVariations(">", listOf(">", "<", "=>", ">="), onInsertText, "quick_key_gt")

        // Common Syntax Operators
        SymbolKeyWithVariations("=", listOf("=", "==", "===", "=>", "!="), onInsertText, "quick_key_eq")
        SymbolKeyWithVariations(";", listOf(";", ":"), onInsertText, "quick_key_semicolon")
        SymbolKeyWithVariations(":", listOf(":", ";"), onInsertText, "quick_key_colon")
        SymbolKeyWithVariations("'", listOf("'", "\"", "`"), onInsertText, "quick_key_quote")
        SymbolKeyWithVariations("\"", listOf("\"", "'", "`"), onInsertText, "quick_key_dquote")
        SymbolKeyWithVariations("`", listOf("`", "```"), onInsertText, "quick_key_backtick")

        QuickKeyButton("/", { onInsertText("/") }, "quick_key_slash")
        QuickKeyButton("\\", { onInsertText("\\") }, "quick_key_backslash")
        QuickKeyButton("|", { onInsertText("|") }, "quick_key_pipe")
        QuickKeyButton("+", { onInsertText("+") }, "quick_key_plus")
        QuickKeyButton("-", { onInsertText("-") }, "quick_key_minus")
        QuickKeyButton("*", { onInsertText("*") }, "quick_key_star")
        QuickKeyButton("%", { onInsertText("%") }, "quick_key_percent")
        QuickKeyButton("#", { onInsertText("#") }, "quick_key_hash")
        QuickKeyButton("@", { onInsertText("@") }, "quick_key_at")
        QuickKeyButton("_", { onInsertText("_") }, "quick_key_underscore")
    }
}

@Composable
private fun ModifierKeyButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val bg = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val text = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 40.dp)
            .background(bg, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 34.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun QuickKeyButton(
    label: String,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 32.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SymbolKeyWithVariations(
    defaultLabel: String,
    variations: List<String>,
    onSelect: (String) -> Unit,
    testTag: String
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 32.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onSelect(defaultLabel) },
                    onLongPress = { expanded = true }
                )
            }
            .padding(horizontal = 6.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = defaultLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            variations.forEach { variant ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = variant,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onClick = {
                        onSelect(variant)
                        expanded = false
                    }
                )
            }
        }
    }
}
