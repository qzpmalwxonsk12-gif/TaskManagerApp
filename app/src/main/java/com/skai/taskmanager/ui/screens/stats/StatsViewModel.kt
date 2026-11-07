package com.skai.taskmanager.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skai.taskmanager.data.repository.StatsRepository
import com.skai.taskmanager.domain.model.StatRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ActivityStatUi(
    val name: String,
    val totalSeconds: Long,
    val percent: Int
)

data class StatsUiState(
    val range: StatRange = StatRange.DAILY,
    val activities: List<ActivityStatUi> = emptyList(),
    val totalSeconds: Long = 0,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val range = MutableStateFlow(StatRange.DAILY)

    val uiState: StateFlow<StatsUiState> = range
        .flatMapLatest { r ->
            statsRepository.observeStats(r).map { stats ->
                val total = stats.sumOf { it.totalSeconds }
                StatsUiState(
                    range = r,
                    activities = stats.map {
                        ActivityStatUi(
                            name = it.name,
                            totalSeconds = it.totalSeconds,
                            percent = if (total > 0) ((it.totalSeconds * 100) / total).toInt() else 0
                        )
                    },
                    totalSeconds = total,
                    isLoading = false
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())

    fun setRange(newRange: StatRange) {
        range.value = newRange
    }
}
