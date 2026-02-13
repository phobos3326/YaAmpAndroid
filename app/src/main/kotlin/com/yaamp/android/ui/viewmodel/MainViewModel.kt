package com.yaamp.android.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yaamp.android.data.model.*
import com.yaamp.android.data.repository.MusicRepository
import com.yaamp.android.player.PlayerManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UiState {
    object Loading : UiState()
    object Success : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository()
    val playerManager = PlayerManager(application, repository)

    private val _uiState = MutableStateFlow<UiState>(UiState.Success)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<SearchResult?>(null)
    val searchResults: StateFlow<SearchResult?> = _searchResults.asStateFlow()

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // Expose player state
    val currentTrack = playerManager.currentTrack
    val isPlaying = playerManager.isPlaying
    val playlist = playerManager.playlist
    val currentIndex = playerManager.currentIndex

    fun setAuthToken(token: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                repository.setAuthToken(token)
                _uiState.value = UiState.Success
                loadUserPlaylists()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Authentication failed")
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val result = repository.search(query)
                result.onSuccess { searchResult ->
                    _searchResults.value = searchResult
                    _uiState.value = UiState.Success
                }.onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Search failed")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Search failed")
            }
        }
    }

    fun playArtist(artistId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val result = repository.getArtistTracks(artistId)
                result.onSuccess { tracks ->
                    if (tracks.isNotEmpty()) {
                        playerManager.setPlaylist(tracks)
                    }
                    _uiState.value = UiState.Success
                }.onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load artist")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load artist")
            }
        }
    }

    fun playAlbum(albumId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val result = repository.getAlbumTracks(albumId)
                result.onSuccess { tracks ->
                    if (tracks.isNotEmpty()) {
                        playerManager.setPlaylist(tracks)
                    }
                    _uiState.value = UiState.Success
                }.onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load album")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load album")
            }
        }
    }

    fun playMyWave() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val result = repository.getMyWaveTracks()
                result.onSuccess { tracks ->
                    if (tracks.isNotEmpty()) {
                        playerManager.setPlaylist(tracks)
                    }
                    _uiState.value = UiState.Success
                }.onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load My Wave")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load My Wave")
            }
        }
    }

    fun playLikedTracks() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                val result = repository.getLikedTracks()
                result.onSuccess { tracks ->
                    if (tracks.isNotEmpty()) {
                        playerManager.setPlaylist(tracks)
                    }
                    _uiState.value = UiState.Success
                }.onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Failed to load liked tracks")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load liked tracks")
            }
        }
    }

    private fun loadUserPlaylists() {
        viewModelScope.launch {
            try {
                val result = repository.getUserPlaylists()
                result.onSuccess { playlists ->
                    _playlists.value = playlists
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCurrentTab(tab: Int) {
        _currentTab.value = tab
    }

    fun clearSearch() {
        _searchResults.value = null
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
