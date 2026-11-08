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
        "PL" to "Premier League",
        "PD" to "La Liga",
        "BL1" to "Bundesliga",
        "SA" to "Serie A",
        "FL1" to "Ligue 1",
        "CL" to "Champions League",
        "DED" to "Eredivisie",
        "BSA" to "Série A (Brazil)",
        "PPL" to "Primeira Liga",
        "ELC" to "Championship",
        "EC" to "European Championship",
        "WC" to "World Cup",
        "8" to "Premier League (SM)", // Keeping for testing/fallback
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
