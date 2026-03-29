package com.folius.dotnotes.data

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    @ColumnInfo(index = true)
    val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(index = true, defaultValue = "0")
    val lastModified: Long = System.currentTimeMillis(),
    val images: List<String> = emptyList(),
    val isChecklist: Boolean = false,
    val checklist: List<ChecklistItem> = emptyList(),
    @ColumnInfo(index = true)
    val isSecret: Boolean = false,
    @ColumnInfo(index = true)
    val isPinned: Boolean = false,
    @ColumnInfo(index = true, defaultValue = "0")
    val isDeleted: Boolean = false,
    val deletedTimestamp: Long? = null,
    val linkedNoteIds: List<Int> = emptyList(),
    val tags: List<String> = emptyList(),
    val color: Int? = null,
    val isMap: Boolean = false,
    val mapItems: List<MapItem> = emptyList()
)

@Immutable
data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
)

@Immutable
data class MapItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val noteId: Int,
    val x: Float,
    val y: Float,
    val color: Int? = null
)
