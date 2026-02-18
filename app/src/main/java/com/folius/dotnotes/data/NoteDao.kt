package com.folius.dotnotes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isPinned = 1 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getPinnedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY deletedTimestamp DESC")
    fun getDeletedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Query("SELECT * FROM notes WHERE title = :title AND isDeleted = 0 LIMIT 1")
    suspend fun getNoteByTitle(title: String): Note?

    @Query("""
        SELECT * FROM notes 
        WHERE isDeleted = 0 
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY timestamp DESC
    """)
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id IN (:ids) AND isDeleted = 0")
    suspend fun getNotesByIds(ids: List<Int>): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<Note>): List<Long>

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    // Soft delete → move to trash
    @Query("UPDATE notes SET isDeleted = 1, deletedTimestamp = :timestamp WHERE id = :id")
    suspend fun softDeleteNote(id: Int, timestamp: Long = System.currentTimeMillis())

    // Restore from trash
    @Query("UPDATE notes SET isDeleted = 0, deletedTimestamp = null WHERE id = :id")
    suspend fun restoreNote(id: Int)

    // Permanently delete notes older than cutoff
    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedTimestamp < :cutoffTimestamp")
    suspend fun permanentlyDeleteOldTrash(cutoffTimestamp: Long)

    // Empty entire trash
    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()
}
