package com.example.mylivesportsscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylivesportsscores.data.model.SportMatch
import com.example.mylivesportsscores.data.remote.SportmonksFixture
import com.example.mylivesportsscores.data.repository.AiRepository
import com.example.mylivesportsscores.data.repository.SportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MatchDetailUiState {
    data object Loading : MatchDetailUiState
    data class Success(val fixture: SportmonksFixture, val aiSummary: String) : MatchDetailUiState
    data class Error(val message: String) : MatchDetailUiState
}

class MatchDetailViewModel(
    private val sportRepository: SportRepository,
    private val aiRepository: AiRepository,
    private val matchId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow<MatchDetailUiState>(MatchDetailUiState.Loading)
    val uiState: StateFlow<MatchDetailUiState> = _uiState.asStateFlow()

    init {
        loadMatchDetails()
    }

    private fun loadMatchDetails() {
        viewModelScope.launch {
            _uiState.value = MatchDetailUiState.Loading
            try {
                val fixture = sportRepository.getMatchDetails(matchId)
                if (fixture != null) {
                    // Fetch AI summary
                    val sportMatch = fixture.toSportMatch()
                    val summary = aiRepository.getMatchSummary(sportMatch)
                    _uiState.value = MatchDetailUiState.Success(fixture, summary)
                } else {
                    _uiState.value = MatchDetailUiState.Error("Match not found")
                }
            } catch (e: Exception) {
                _uiState.value = MatchDetailUiState.Error(e.localizedMessage ?: "Failed to load details")
            }
        }
    }

    // Helper to map fixture to SportMatch (could be moved to repository or utility)
    private fun SportmonksFixture.toSportMatch(): SportMatch {
        val homeParticipant = participants.find { it.meta?.location == "home" }
        val awayParticipant = participants.find { it.meta?.location == "away" }
        val homeScore = scores.find { it.description == "CURRENT" && it.id == homeParticipant?.id }?.score?.goals
        val awayScore = scores.find { it.description == "CURRENT" && it.id == awayParticipant?.id }?.score?.goals

        return SportMatch(
            id = id.toString(),
            homeTeam = homeParticipant?.name ?: "Home Team",
            awayTeam = awayParticipant?.name ?: "Away Team",
            homeScore = homeScore,
            awayScore = awayScore,
            status = if (starting_at.contains("LIVE")) "Live" else "Scheduled",
            startingAt = starting_at,
            league = league?.name ?: "Unknown League",
            sport = "Football"
        )
    }
}
