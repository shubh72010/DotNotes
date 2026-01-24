package com.folius.dotnotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val images: List<String> = emptyList(),
    val isChecklist: Boolean = false,
    val checklist: List<ChecklistItem> = emptyList(),
    val folderId: Int? = null,
    val isSecret: Boolean = false,
    val isPinned: Boolean = false
)

data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isChecked: Boolean = false
)
