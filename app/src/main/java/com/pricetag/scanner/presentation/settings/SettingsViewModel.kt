package com.pricetag.scanner.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pricetag.scanner.domain.model.AppSettings
import com.pricetag.scanner.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getSettings().collect { _settings.value = it }
        }
    }

    fun save(s: AppSettings) {
        viewModelScope.launch {
            repo.saveSettings(s)
            _saved.value = true
        }
    }

    fun clearSaved() { _saved.value = false }
}
