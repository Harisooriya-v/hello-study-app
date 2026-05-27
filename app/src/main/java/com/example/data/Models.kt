package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklist_items")
data class ChecklistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val priority: String = "Medium", // Low, Medium, High
    val category: String = "General", // Math, Science, Language, General
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class NoteItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val noteType: String, // TEXT, PICTURE, AUDIO, VIDEO
    val mediaUri: String? = null, // URI, URL or sample image asset name
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "scribble_items")
data class ScribbleItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val strokeData: String, // Serialized drawing strokes (e.g. "colorIndex:x1,y1|x2,y2;...")
    val timestamp: Long = System.currentTimeMillis()
)
