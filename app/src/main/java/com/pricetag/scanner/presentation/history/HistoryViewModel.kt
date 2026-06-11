package com.pricetag.scanner.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricetag.scanner.data.db.entity.JobEntity
import com.pricetag.scanner.data.network.SocketManager
import com.pricetag.scanner.domain.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val jobs:         List<JobEntity> = emptyList(),
    val filtered:     List<JobEntity> = emptyList(),
    val query:        String          = "",
    val snackbar:     String?         = null,
    val isResending:  Set<Long>       = emptySet(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val jobRepository: JobRepository,
    private val socketManager: SocketManager,
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            jobRepository.getAllJobs().collect { jobs ->
                val q = _state.value.query
                _state.update { it.copy(jobs = jobs, filtered = filterJobs(jobs, q)) }
            }
        }
    }

    fun search(query: String) {
        _state.update { it.copy(query = query, filtered = filterJobs(it.jobs, query)) }
    }

    fun resend(job: JobEntity) {
        viewModelScope.launch {
            _state.update { it.copy(isResending = it.isResending + job.id) }
            if (!socketManager.isConnected) {
                // Mark as pending so the retry loop picks it up
                jobRepository.saveJob(job.copy(id = 0, status = JobEntity.STATUS_PENDING))
                _state.update {
                    it.copy(
                        isResending = it.isResending - job.id,
                        snackbar    = "Queued — will send when connected",
                    )
                }
                return@launch
            }
            val response = socketManager.send(job.payload)
            if (response != null && response.startsWith("OK")) {
                jobRepository.markSent(job.id, response)
                _state.update {
                    it.copy(
                        isResending = it.isResending - job.id,
                        snackbar    = "✅ Re-sent! $response",
                    )
                }
            } else {
                jobRepository.markFailed(job.id, response ?: "No response")
                _state.update {
                    it.copy(
                        isResending = it.isResending - job.id,
                        snackbar    = "❌ Failed: ${response ?: "No response"}",
                    )
                }
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            jobRepository.deleteJob(id)
            _state.update { it.copy(snackbar = "Job deleted") }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            jobRepository.deleteAll()
            _state.update { it.copy(snackbar = "All history cleared") }
        }
    }

    fun dismissSnackbar() = _state.update { it.copy(snackbar = null) }

    private fun filterJobs(jobs: List<JobEntity>, q: String): List<JobEntity> {
        if (q.isBlank()) return jobs
        val lower = q.lowercase()
        return jobs.filter {
            it.barcodes.lowercase().contains(lower) ||
            it.tagType.lowercase().contains(lower)  ||
            it.status.lowercase().contains(lower)
        }
    }
}
