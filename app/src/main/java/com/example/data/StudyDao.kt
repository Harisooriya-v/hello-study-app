package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
    // === Checklist ===
    @Query("SELECT * FROM checklist_items ORDER BY isCompleted ASC, priority DESC, timestamp DESC")
    fun getAllChecklistItems(): Flow<List<ChecklistItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItem)

    @Update
    suspend fun updateChecklistItem(item: ChecklistItem)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItemById(id: Int)

    // === Multimedia Notes ===
    @Query("SELECT * FROM study_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<NoteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteItem)

    @Query("DELETE FROM study_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)

    // === Scribbles ===
    @Query("SELECT * FROM scribble_items ORDER BY timestamp DESC")
    fun getAllScribbles(): Flow<List<ScribbleItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScribble(scribble: ScribbleItem)

    @Query("DELETE FROM scribble_items WHERE id = :id")
    suspend fun deleteScribbleById(id: Int)
}
