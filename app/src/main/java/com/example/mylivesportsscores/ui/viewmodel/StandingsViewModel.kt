package com.example.mylivesportsscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylivesportsscores.data.remote.SportmonksStanding
import com.example.mylivesportsscores.data.repository.SportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StandingsUiState {
    data object Loading : StandingsUiState
    data class Success(
        val standingsMap: Map<String, List<SportmonksStanding>>
    ) : StandingsUiState
    data class Error(val message: String) : StandingsUiState
}

class StandingsViewModel(private val sportRepository: SportRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<StandingsUiState>(StandingsUiState.Loading)
    val uiState: StateFlow<StandingsUiState> = _uiState.asStateFlow()

    private val leagues = mapOf(
        "8" to "Premier League",
        "564" to "La Liga",
        "82" to "Bundesliga",
        "384" to "Serie A",
        "2" to "Champions League",
        "501" to "Scottish Premiership",
        "271" to "Danish Superliga",
        "451" to "Ghana Premier League"
    )

    init {
        loadStandings()
    }

    fun loadStandings() {
        viewModelScope.launch {
            _uiState.value = StandingsUiState.Loading
            try {
                val results = mutableMapOf<String, List<SportmonksStanding>>()
                // We fetch all requested, but only successful ones will be shown
                leagues.forEach { (id, name) ->
                    val standing = sportRepository.getStandingsByLeague(id)
                    if (standing.isNotEmpty()) {
                        results[name] = standing
                    }
                }
                
                if (results.isEmpty()) {
                    _uiState.value = StandingsUiState.Error("No standings data available for your current subscription.")
                } else {
                    _uiState.value = StandingsUiState.Success(results)
                }
            } catch (e: Exception) {
                _uiState.value = StandingsUiState.Error(e.localizedMessage ?: "Failed to load standings")
            }
        }
    }
}
