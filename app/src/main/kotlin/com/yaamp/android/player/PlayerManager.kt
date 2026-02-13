package com.yaamp.android.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.yaamp.android.data.model.Track
import com.yaamp.android.data.repository.MusicRepository
import com.yaamp.android.service.MusicPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerManager(
    private val context: Context,
    private val repository: MusicRepository
) {
    private var mediaController: MediaController? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _playlist = MutableStateFlow<List<Track>>(emptyList())
    val playlist: StateFlow<List<Track>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicPlaybackService::class.java)
        )

        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                setupPlayerListener()
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentTrack()
            }
        })
    }

    private fun updateCurrentTrack() {
        val currentIndex = mediaController?.currentMediaItemIndex ?: 0
        _currentIndex.value = currentIndex
        if (currentIndex < _playlist.value.size) {
            _currentTrack.value = _playlist.value[currentIndex]
        }
    }

    fun setPlaylist(tracks: List<Track>, startIndex: Int = 0) {
        _playlist.value = tracks
        _currentIndex.value = startIndex

        coroutineScope.launch {
            val mediaItems = mutableListOf<MediaItem>()
            
            tracks.forEach { track ->
                try {
                    val result = repository.getStreamUrl(track.id)
                    result.getOrNull()?.let { streamUrl ->
                        val mediaItem = MusicPlaybackService.buildMediaItem(track, streamUrl)
                        mediaItems.add(mediaItem)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            mediaController?.apply {
                setMediaItems(mediaItems, startIndex, 0)
                prepare()
                play()
            }

            if (startIndex < tracks.size) {
                _currentTrack.value = tracks[startIndex]
            }
        }
    }

    fun playPause() {
        mediaController?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun play() {
        mediaController?.play()
    }

    fun pause() {
        mediaController?.pause()
    }

    fun next() {
        mediaController?.seekToNextMediaItem()
    }

    fun previous() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }

    fun setVolume(volume: Float) {
        mediaController?.volume = volume
    }

    fun getVolume(): Float {
        return mediaController?.volume ?: 1f
    }

    fun getDuration(): Long {
        return mediaController?.duration ?: 0
    }

    fun getCurrentPosition(): Long {
        return mediaController?.currentPosition ?: 0
    }

    fun release() {
        mediaController?.release()
        mediaController = null
    }
}
