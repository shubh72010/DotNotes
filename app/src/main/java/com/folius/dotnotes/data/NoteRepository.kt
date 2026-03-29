package com.folius.dotnotes.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * NoteRepository — pure data-access layer wrapping NoteDao.
 * Business logic (filtering, sorting, search) stays in the ViewModel.
 */
class NoteRepository(private val noteDao: NoteDao) {

    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun getPinnedNotes(): Flow<List<Note>> = noteDao.getPinnedNotes()

    fun getDeletedNotes(): Flow<List<Note>> = noteDao.getDeletedNotes()

    suspend fun getNoteById(id: Int): Note? = noteDao.getNoteById(id)

    suspend fun getNoteByTitle(title: String): Note? = noteDao.getNoteByTitle(title)

    suspend fun getNotesByIds(ids: List<Int>): List<Note> = noteDao.getNotesByIds(ids)

    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)

    suspend fun insertNotes(notes: List<Note>): List<Long> = noteDao.insertNotes(notes)

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun deleteNoteById(id: Int) = noteDao.deleteNoteById(id)

    suspend fun softDeleteNote(id: Int, timestamp: Long = System.currentTimeMillis()) =
        noteDao.softDeleteNote(id, timestamp)

    suspend fun restoreNote(id: Int) = noteDao.restoreNote(id)

    suspend fun permanentlyDeleteOldTrash(cutoffTimestamp: Long) =
        noteDao.permanentlyDeleteOldTrash(cutoffTimestamp)

    suspend fun emptyTrash() = noteDao.emptyTrash()

    /**
     * Snapshot of all non-deleted notes. Used by backup and backlinks.
     */
    suspend fun getAllNotesSnapshot(): List<Note> = noteDao.getAllNotes().first()
}
