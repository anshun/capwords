package com.capwords.ui.gallery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.capwords.appContainer
import com.capwords.data.WordEntity
import com.capwords.ui.util.DateUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Words for one calendar day, newest day first. */
data class DaySection(
    val dayKey: Long,
    val label: String,
    val words: List<WordEntity>,
)

class GalleryViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app.appContainer

    val sections: StateFlow<List<DaySection>> =
        container.repository.words
            .map { words ->
                words.groupBy { DateUtils.dayKey(it.createdAt) }
                    .toSortedMap(compareByDescending { it })
                    .map { (key, items) ->
                        DaySection(
                            dayKey = key,
                            label = DateUtils.dayLabel(key),
                            words = items,
                        )
                    }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
