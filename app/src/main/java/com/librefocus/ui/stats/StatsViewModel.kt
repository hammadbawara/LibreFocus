package com.librefocus.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.librefocus.data.repository.UsageTrackingRepository
import com.librefocus.models.AppUsageData
import com.librefocus.models.UsageValuePoint
import com.librefocus.utils.DateTimeFormatterManager
import com.librefocus.utils.FormattedDateTimePreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
data class StatsUiState(
    val totalUnlocks: Int = 0,
    val appUsage: List<AppUsageData> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class StatsViewModel(
    private val usageRepository: UsageTrackingRepository,
    private val dateTimeFormatterManager: DateTimeFormatterManager
) : ViewModel() {

    private var _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    suspend fun refreshUsageStats(period: StatsPeriodState) {
        viewModelScope.launch {
            _uiState.update {it.copy(isLoading = true)}

            runCatching {
                val appUsage = usageRepository.getAppUsageSummaryInTimeRange(
                    startUtc = period.startUtc,
                    endUtc = period.endUtc
                )

                val totalUnlocks = usageRepository.getDailyUnlockSummary(
                    startUtc = period.startUtc,
                    endUtc = period.endUtc
                ).sumOf { it.totalUnlocks }

                delay(5000)

                StatsUiState(
                    totalUnlocks = totalUnlocks,
                    appUsage = appUsage,
                    isLoading = false
                )

            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
            }
        }
    }
}
