package com.droidcode.database.sqlite

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseConsoleDialog(
    dbFile: File,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val provider = remember(dbFile) { SqliteDatabaseProvider(dbFile) }
    var tables by remember { mutableStateOf<List<SqliteTableInfo>>(emptyList()) }
    var selectedTable by remember { mutableStateOf<SqliteTableInfo?>(null) }
    var queryText by remember { mutableStateOf("SELECT * FROM ") }
    var queryResult by remember { mutableStateOf<SqliteQueryResult?>(null) }
    var isRunningQuery by remember { mutableStateOf(false) }

    val refreshTables = {
        tables = provider.getTables()
        if (selectedTable == null && tables.isNotEmpty()) {
            selectedTable = tables.first()
            queryText = "SELECT * FROM \"${tables.first().name}\" LIMIT 50;"
        }
    }

    LaunchedEffect(dbFile) {
        refreshTables()
    }

    DisposableEffect(provider) {
        onDispose {
            provider.close()
        }
    }

    val runQuery: (String) -> Unit = { sql ->
        isRunningQuery = true
        try {
            queryResult = provider.execute(sql)
        } finally {
            isRunningQuery = false
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "SQLite Console",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SQLite Database Console",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${dbFile.name} (${tables.size} tables)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = { refreshTables() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Tables")
                        }

                        IconButton(onClick = onDismissRequest) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                // Table Selector Chips (Horizontal Scroll)
                if (tables.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (tbl in tables) {
                            FilterChip(
                                selected = selectedTable?.name == tbl.name,
                                onClick = {
                                    selectedTable = tbl
                                    queryText = "SELECT * FROM \"${tbl.name}\" LIMIT 50;"
                                    runQuery(queryText)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text("${tbl.name} (${tbl.rowCount})", fontSize = 12.sp)
                                }
                            )
                        }
                    }
                    HorizontalDivider()
                }

                // SQL Query Box
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        placeholder = { Text("SELECT * FROM ...", fontSize = 12.sp) },
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick queries
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            selectedTable?.let { tbl ->
                                SuggestionChip(
                                    onClick = {
                                        queryText = "SELECT * FROM \"${tbl.name}\" LIMIT 50;"
                                        runQuery(queryText)
                                    },
                                    label = { Text("SELECT *", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        queryText = "SELECT count(*) FROM \"${tbl.name}\";"
                                        runQuery(queryText)
                                    },
                                    label = { Text("COUNT(*)", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = {
                                        queryText = "PRAGMA table_info(\"${tbl.name}\");"
                                        runQuery(queryText)
                                    },
                                    label = { Text("SCHEMA", fontSize = 11.sp) }
                                )
                            }
                        }

                        // Run Button
                        Button(
                            onClick = { runQuery(queryText) },
                            enabled = !isRunningQuery && queryText.isNotBlank(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run", fontSize = 13.sp)
                        }
                    }
                }

                HorizontalDivider()

                // Query Execution Status / Error
                val result = queryResult
                if (result != null) {
                    Surface(
                        color = if (result.error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (result.error != null) "Error: ${result.error}" else "${result.rows.size} rows returned in ${result.executionTimeMs}ms",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (result.error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (result.rows.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        val csv = buildString {
                                            append(result.columns.joinToString(",")).append("\n")
                                            for (row in result.rows) {
                                                append(row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }).append("\n")
                                            }
                                        }
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("CSV Results", csv))
                                        Toast.makeText(context, "Exported ${result.rows.size} rows to clipboard (CSV)", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy CSV", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy CSV", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // Results Grid Table (2D Scrollable)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    if (result == null || (result.rows.isEmpty() && result.columns.isEmpty())) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isRunningQuery) "Executing query..." else "No query results to display",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        val horizontalScroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(horizontalScroll)
                        ) {
                            // Column Headers Row
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(vertical = 6.dp)
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.width(40.dp).padding(horizontal = 4.dp)
                                )
                                for (col in result.columns) {
                                    Text(
                                        text = col,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.width(130.dp).padding(horizontal = 4.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            HorizontalDivider()

                            // Data Rows
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(result.rows.indices.toList()) { rowIdx ->
                                    val rowData = result.rows[rowIdx]
                                    val rowBg = if (rowIdx % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    Row(
                                        modifier = Modifier
                                            .background(rowBg)
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = (rowIdx + 1).toString(),
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.width(40.dp).padding(horizontal = 4.dp)
                                        )
                                        for (cell in rowData) {
                                            Text(
                                                text = cell,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                                color = if (cell == "NULL") MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.width(130.dp).padding(horizontal = 4.dp),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
