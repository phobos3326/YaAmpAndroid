package com.yaamp.android.ui.viewmodel

import android.app.Application
import android.content.Context
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

    // SharedPreferences для сохранения токена
    private val prefs = application.getSharedPreferences("yaamp_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<UiState>(UiState.Success)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchResults = MutableStateFlow<SearchResult?>(null)
    val searchResults: StateFlow<SearchResult?> = _searchResults.asStateFlow()

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    // Состояние авторизации
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Expose player state
    val currentTrack = playerManager.currentTrack
    val isPlaying = playerManager.isPlaying
    val playlist = playerManager.playlist
    val currentIndex = playerManager.currentIndex

    init {
        // Проверяем сохраненный токен при запуске
        checkSavedToken()
    }

    private fun checkSavedToken() {
        val savedToken = prefs.getString("auth_token", null)
        if (!savedToken.isNullOrBlank()) {
            setAuthToken(savedToken)
        }
    }

    fun setAuthToken(token: String) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                repository.setAuthToken(token)

                // Сохраняем токен
                prefs.edit().putString("auth_token", token).apply()

                _isAuthenticated.value = true
                _uiState.value = UiState.Success
                loadUserPlaylists()
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Authentication failed")
                _isAuthenticated.value = false

                // Удаляем неверный токен
                prefs.edit().remove("auth_token").apply()
            }
        }
    }

    fun logout() {
        prefs.edit().remove("auth_token").apply()
        _isAuthenticated.value = false
        _searchResults.value = null
        _playlists.value = emptyList()
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
                    } else {
                        _uiState.value = UiState.Error("No liked tracks found")
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
