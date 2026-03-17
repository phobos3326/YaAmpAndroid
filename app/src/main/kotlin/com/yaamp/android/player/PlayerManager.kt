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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

class PlayerManager(
    private val context: Context,
    private val repository: MusicRepository
) {
    private var mediaController: MediaController? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
        if (mediaController != null) return

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

        if (startIndex < tracks.size) {
            _currentTrack.value = tracks[startIndex]
        }

        ioScope.launch {
            val mediaItems = mutableListOf<MediaItem>()
            val seenTrackIds = mutableSetOf<String>()
            val limitedTracks = tracks.drop(startIndex).take(30)
            var startedPlayback = false

            limitedTracks.forEach { track ->
                if (!seenTrackIds.add(track.id)) {
                    return@forEach
                }
                try {
                    val result = repository.getStreamUrl(track.id)
                    result.getOrNull()?.let { streamUrl ->
                        if (repository.validateDirectUrl(streamUrl) == null) {
                            return@let
                        }
                        val validatedUrl = streamUrl
                        val mediaItem = MusicPlaybackService.buildMediaItem(track, validatedUrl)
                        mediaItems.add(mediaItem)

                        if (!startedPlayback) {
                            startedPlayback = true
                            withContext(Dispatchers.Main) {
                                mediaController?.apply {
                                    stop()
                                    clearMediaItems()
                                    setMediaItems(listOf(mediaItem), 0, 0)
                                    prepare()
                                    play()
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                mediaController?.addMediaItem(mediaItem)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun playFromIndex(index: Int) {
        val tracks = _playlist.value
        if (tracks.isEmpty() || index !in tracks.indices) {
            return
        }
        if (mediaController == null) {
            initializeController()
        }
        mediaController?.let { controller ->
            val hasMediaItems = controller.mediaItemCount > 0
            if (hasMediaItems && controller.mediaItemCount == tracks.size) {
                controller.seekTo(index, 0)
                controller.play()
                _currentIndex.value = index
                _currentTrack.value = tracks[index]
                return
            }
        }
        setPlaylist(tracks, index)
        // Явно запускаем воспроизведение после установки плейлиста
        play()
    }

    fun playPause() {
        if (mediaController == null) {
            initializeController()
            return
        }

        mediaController?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun play() {
        if (mediaController == null) {
            initializeController()
            return
        }
        mediaController?.play()
    }

    fun pause() {
        if (mediaController == null) {
            initializeController()
            return
        }
        mediaController?.pause()
    }

    fun next() {
        if (mediaController == null) {
            initializeController()
            return
        }
        mediaController?.seekToNextMediaItem()
    }

    fun previous() {
        if (mediaController == null) {
            initializeController()
            return
        }
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
        coroutineScope.coroutineContext.cancel()
        ioScope.coroutineContext.cancel()
    }
}
