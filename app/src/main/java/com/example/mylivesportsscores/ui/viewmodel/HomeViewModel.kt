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
import java.text.SimpleDateFormat
import java.util.*

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val matches: List<SportMatch>,
        val filteredMatches: List<SportMatch>,
        val news: List<NewsArticle>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

enum class MatchFilter {
    ALL, LIVE, UPCOMING, FINISHED
}

class HomeViewModel(private val sportRepository: SportRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate.asStateFlow()

    private val _selectedFilter = MutableStateFlow(MatchFilter.ALL)
    val selectedFilter: StateFlow<MatchFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allMatches: List<SportMatch> = emptyList()

    init {
        refreshData()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        applyFilter()
    }

    fun setDate(calendar: Calendar) {
        _selectedDate.value = calendar
        refreshData()
    }

    fun setFilter(filter: MatchFilter) {
        _selectedFilter.value = filter
        applyFilter()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(_selectedDate.value.time)
                
                // Fetch real football data
                val liveScores = if (isToday(_selectedDate.value)) {
                    sportRepository.getLiveScores()
                } else {
                    emptyList()
                }
                
                val fixtures = sportRepository.getFixturesByDate(dateStr)
                
                allMatches = (liveScores + fixtures).distinctBy { it.id }
                applyFilter()
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Failed to load football data")
            }
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.lowercase()
        val filteredBySearch = if (query.isEmpty()) {
            allMatches
        } else {
            allMatches.filter { 
                it.homeTeam.lowercase().contains(query) || 
                it.awayTeam.lowercase().contains(query) || 
                it.league.lowercase().contains(query) 
            }
        }

        val filtered = when (_selectedFilter.value) {
            MatchFilter.ALL -> filteredBySearch
            MatchFilter.LIVE -> filteredBySearch.filter { it.status.contains("Live", ignoreCase = true) || it.status.contains("INPLAY", ignoreCase = true) }
            MatchFilter.UPCOMING -> filteredBySearch.filter { it.status.contains("Scheduled", ignoreCase = true) || it.status.contains("NS", ignoreCase = true) }
            MatchFilter.FINISHED -> filteredBySearch.filter { it.status.contains("Finished", ignoreCase = true) || it.status.contains("FT", ignoreCase = true) }
        }
        _uiState.value = HomeUiState.Success(allMatches, filtered, emptyList())
    }

    private fun isToday(calendar: Calendar): Boolean {
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
    }
}
