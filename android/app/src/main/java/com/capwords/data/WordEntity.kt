package com.capwords.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One captured & recognized object: an English word, its Chinese translations,
 * the path to its cut-out sticker PNG, and when it was captured.
 */
@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val english: String,
    val zhTw: String?,
    val zhCn: String?,
    val stickerPath: String,
    val confidence: Float,
    val createdAt: Long,
)
