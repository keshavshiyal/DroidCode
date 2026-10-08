package com.droidcode.database.sqlite

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.droidcode.database.DatabaseProvider
import java.io.File

data class SqliteColumnInfo(
    val cid: Int,
    val name: String,
    val type: String,
    val notNull: Boolean,
    val defaultValue: String?,
    val isPrimaryKey: Boolean
)

data class SqliteTableInfo(
    val name: String,
    val type: String, // "table" or "view"
    val rowCount: Long = 0L,
    val columns: List<SqliteColumnInfo> = emptyList()
)

data class SqliteQueryResult(
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val rowsAffected: Int = 0,
    val executionTimeMs: Long = 0L,
    val error: String? = null
)

class SqliteDatabaseProvider(val dbFile: File) : DatabaseProvider {

    private var database: SQLiteDatabase? = null

    init {
        open()
    }

    private fun open() {
        if (database == null || !database!!.isOpen) {
            database = SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READWRITE or SQLiteDatabase.CREATE_IF_NECESSARY
            )
        }
    }

    override fun getProviderId(): String = "sqlite"

    override fun getDisplayName(): String = "SQLite (${dbFile.name})"

    override fun isConnected(): Boolean = database?.isOpen == true

    override fun executeQuery(sql: String): String {
        val result = execute(sql)
        if (result.error != null) {
            throw Exception(result.error)
        }
        val sb = StringBuilder()
        sb.append(result.columns.joinToString(" | ")).append("\n")
        sb.append("-".repeat(40)).append("\n")
        for (row in result.rows.take(50)) {
            sb.append(row.joinToString(" | ")).append("\n")
        }
        sb.append("\n(${result.rows.size} rows returned in ${result.executionTimeMs}ms)")
        return sb.toString()
    }

    /**
     * Executes arbitrary SQL (both queries and DDL/DML statements).
     */
    fun execute(sql: String): SqliteQueryResult {
        val trimmed = sql.trim()
        if (trimmed.isEmpty()) {
            return SqliteQueryResult(error = "Empty query")
        }

        val startTime = System.currentTimeMillis()
        val db = database ?: return SqliteQueryResult(error = "Database not connected")

        val isSelect = trimmed.startsWith("SELECT", ignoreCase = true) ||
                trimmed.startsWith("PRAGMA", ignoreCase = true) ||
                trimmed.startsWith("EXPLAIN", ignoreCase = true)

        return try {
            if (isSelect) {
                var cursor: Cursor? = null
                try {
                    cursor = db.rawQuery(trimmed, null)
                    val cols = cursor.columnNames.toList()
                    val rows = ArrayList<List<String>>()

                    while (cursor.moveToNext() && rows.size < 500) {
                        val row = ArrayList<String>(cols.size)
                        for (i in cols.indices) {
                            if (cursor.isNull(i)) {
                                row.add("NULL")
                            } else {
                                val str = try {
                                    cursor.getString(i)
                                } catch (e: Exception) {
                                    "[BLOB]"
                                }
                                row.add(str ?: "NULL")
                            }
                        }
                        rows.add(row)
                    }

                    val timeMs = System.currentTimeMillis() - startTime
                    SqliteQueryResult(
                        columns = cols,
                        rows = rows,
                        executionTimeMs = timeMs
                    )
                } finally {
                    cursor?.close()
                }
            } else {
                db.execSQL(trimmed)
                val timeMs = System.currentTimeMillis() - startTime
                SqliteQueryResult(
                    rowsAffected = 1,
                    executionTimeMs = timeMs
                )
            }
        } catch (e: Exception) {
            SqliteQueryResult(
                executionTimeMs = System.currentTimeMillis() - startTime,
                error = e.message ?: "Execution error"
            )
        }
    }

    /**
     * Retrieves all tables, views, and schemas in the SQLite database.
     */
    fun getTables(): List<SqliteTableInfo> {
        val db = database ?: return emptyList()
        val tables = ArrayList<SqliteTableInfo>()

        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery(
                "SELECT name, type FROM sqlite_master WHERE type IN ('table', 'view') AND name NOT LIKE 'sqlite_%' ORDER BY name",
                null
            )
            while (cursor.moveToNext()) {
                val tableName = cursor.getString(0)
                val type = cursor.getString(1)

                // Get row count
                var count = 0L
                try {
                    val countCursor = db.rawQuery("SELECT count(*) FROM \"$tableName\"", null)
                    if (countCursor.moveToFirst()) {
                        count = countCursor.getLong(0)
                    }
                    countCursor.close()
                } catch (e: Exception) {
                    // Ignore count errors on complex views
                }

                // Get column schemas
                val columns = getTableColumns(tableName)
                tables.add(SqliteTableInfo(tableName, type, count, columns))
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            cursor?.close()
        }

        return tables
    }

    fun getTableColumns(tableName: String): List<SqliteColumnInfo> {
        val db = database ?: return emptyList()
        val columns = ArrayList<SqliteColumnInfo>()
        var cursor: Cursor? = null
        try {
            cursor = db.rawQuery("PRAGMA table_info(\"$tableName\")", null)
            while (cursor.moveToNext()) {
                val cid = cursor.getInt(0)
                val name = cursor.getString(1)
                val type = cursor.getString(2)
                val notNull = cursor.getInt(3) == 1
                val dflt = cursor.getString(4)
                val pk = cursor.getInt(5) == 1
                columns.add(SqliteColumnInfo(cid, name, type, notNull, dflt, pk))
            }
        } catch (e: Exception) {
            // Ignore
        } finally {
            cursor?.close()
        }
        return columns
    }

    fun close() {
        try {
            if (database?.isOpen == true) {
                database?.close()
            }
        } catch (e: Exception) {
            // Ignore
        }
        database = null
    }
}
