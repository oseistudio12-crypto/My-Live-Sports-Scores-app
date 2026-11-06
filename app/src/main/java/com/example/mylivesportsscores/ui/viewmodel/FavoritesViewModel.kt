package com.example.mylivesportsscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mylivesportsscores.data.model.Favorite
import com.example.mylivesportsscores.data.repository.FirestoreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(private val repository: FirestoreRepository) : ViewModel() {
    val favorites: StateFlow<List<Favorite>> = repository.getFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addFavorite(type: String, targetId: String, name: String, imageUrl: String?) {
        viewModelScope.launch {
            repository.addFavorite(type, targetId, name, imageUrl)
        }
    }

    fun removeFavorite(favoriteId: String) {
        viewModelScope.launch {
            repository.removeFavorite(favoriteId)
        }
    }
}
