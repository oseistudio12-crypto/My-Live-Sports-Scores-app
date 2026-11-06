package com.example.mylivesportsscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylivesportsscores.data.model.NewsArticle
import com.example.mylivesportsscores.data.model.SportMatch
import com.example.mylivesportsscores.data.repository.SportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val matches: List<SportMatch>, val news: List<NewsArticle>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val sportRepository: SportRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                // Fetch real football data
                val liveScores = sportRepository.getLiveScores()
                val fixtures = sportRepository.getTodayFixtures()
                
                val allMatches = (liveScores + fixtures).distinctBy { it.id }
                
                _uiState.value = HomeUiState.Success(allMatches, emptyList())
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Failed to load football data")
            }
        }
    }
}
