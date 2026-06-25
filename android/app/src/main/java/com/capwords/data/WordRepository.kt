package com.capwords.data

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class WordRepository(
    private val context: Context,
    private val dao: WordDao,
) {
    val words: Flow<List<WordEntity>> = dao.observeAll()
    val count: Flow<Int> = dao.observeCount()

    private val stickerDir: File by lazy {
        File(context.filesDir, "stickers").apply { mkdirs() }
    }

    /** Persist the cut-out sticker PNG and insert the word row. */
    suspend fun saveWord(
        english: String,
        zhTw: String?,
        zhCn: String?,
        confidence: Float,
        sticker: Bitmap,
        createdAt: Long,
    ): Long = withContext(Dispatchers.IO) {
        val file = File(stickerDir, "sticker_$createdAt.png")
        FileOutputStream(file).use { out ->
            sticker.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        dao.insert(
            WordEntity(
                english = english,
                zhTw = zhTw,
                zhCn = zhCn,
                stickerPath = file.absolutePath,
                confidence = confidence,
                createdAt = createdAt,
            ),
        )
    }

    suspend fun delete(word: WordEntity) = withContext(Dispatchers.IO) {
        runCatching { File(word.stickerPath).delete() }
        dao.delete(word)
    }
}
