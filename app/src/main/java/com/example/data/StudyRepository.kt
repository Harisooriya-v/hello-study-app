package com.example.data

import kotlinx.coroutines.flow.Flow

class StudyRepository(private val studyDao: StudyDao) {

    // === Checklist ===
    val allChecklistItems: Flow<List<ChecklistItem>> = studyDao.getAllChecklistItems()

    suspend fun insertChecklistItem(item: ChecklistItem) {
        studyDao.insertChecklistItem(item)
    }

    suspend fun updateChecklistItem(item: ChecklistItem) {
        studyDao.updateChecklistItem(item)
    }

    suspend fun deleteChecklistItem(id: Int) {
        studyDao.deleteChecklistItemById(id)
    }

    // === Multimedia Notes ===
    val allNotes: Flow<List<NoteItem>> = studyDao.getAllNotes()

    suspend fun insertNote(note: NoteItem) {
        studyDao.insertNote(note)
    }

    suspend fun deleteNote(id: Int) {
        studyDao.deleteNoteById(id)
    }

    // === Scribbles ===
    val allScribbles: Flow<List<ScribbleItem>> = studyDao.getAllScribbles()

    suspend fun insertScribble(scribble: ScribbleItem) {
        studyDao.insertScribble(scribble)
    }

    suspend fun deleteScribble(id: Int) {
        studyDao.deleteScribbleById(id)
    }

    // === Focus Sessions ===
    val allFocusSessions: Flow<List<FocusSession>> = studyDao.getAllFocusSessions()

    suspend fun insertFocusSession(session: FocusSession) {
        studyDao.insertFocusSession(session)
    }

    suspend fun deleteFocusSession(id: Int) {
        studyDao.deleteFocusSessionById(id)
    }

    suspend fun deleteAllFocusSessions() {
        studyDao.deleteAllFocusSessions()
    }
}
