package com.example.mylivesportsscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylivesportsscores.data.model.NewsArticle
import com.example.mylivesportsscores.data.model.SportMatch
import com.example.mylivesportsscores.data.repository.MultiSportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val matches: List<SportMatch>, val news: List<NewsArticle>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val multiSportRepository: MultiSportRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                // Fetch scores for non-football sports
                val basketballMatches = multiSportRepository.getScoresForSport("Basketball")
                val tennisMatches = multiSportRepository.getScoresForSport("Tennis")
                
                val allMatches = basketballMatches + tennisMatches
                
                _uiState.value = HomeUiState.Success(allMatches, emptyList())
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Failed to load multi-sport data")
            }
        }
    }
}
