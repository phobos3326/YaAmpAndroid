package com.yaamp.android.data.repository

import com.yaamp.android.data.api.YandexMusicClient
import com.yaamp.android.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository {

    private val api = YandexMusicClient.api
    private var currentUserId: String? = null

    suspend fun setAuthToken(token: String) = withContext(Dispatchers.IO) {
        YandexMusicClient.setAuthToken(token)
        
        // Get user info
        try {
            val response = api.getAccountStatus()
            currentUserId = response.result?.uid?.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun search(query: String, type: String = "all"): Result<SearchResult> = 
        withContext(Dispatchers.IO) {
            try {
                val response = api.search(query, type)
                if (response.result != null) {
                    Result.success(response.result)
                } else {
                    Result.failure(Exception(response.error ?: "Unknown error"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getArtistTracks(artistId: String): Result<List<Track>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = api.getArtistTracks(artistId, pageSize = 50)
                if (response.result != null) {
                    Result.success(response.result.tracks)
                } else {
                    Result.failure(Exception(response.error ?: "Unknown error"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getAlbumTracks(albumId: String): Result<List<Track>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = api.getAlbumWithTracks(albumId)
                if (response.result != null) {
                    Result.success(response.result.getAllTracks())
                } else {
                    Result.failure(Exception(response.error ?: "Unknown error"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getLikedTracks(): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            val userId = currentUserId ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            val response = api.getLikedTracks(userId)
            val trackShorts = response.result?.library?.tracks ?: emptyList()
            
            if (trackShorts.isNotEmpty()) {
                val trackIds = trackShorts.map { "${it.id}:${it.albumId}" }
                val tracksResponse = api.getTracks(trackIds)
                
                if (tracksResponse.result != null) {
                    Result.success(tracksResponse.result)
                } else {
                    Result.failure(Exception(tracksResponse.error ?: "Unknown error"))
                }
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyWaveTracks(): Result<List<Track>> = withContext(Dispatchers.IO) {
        try {
            // Get user's wave station
            val stationsResponse = api.getStations()
            val waveStation = stationsResponse.result?.find { 
                it.id.type == "user" && it.id.tag.contains("onyourwave")
            }
            
            if (waveStation != null) {
                val tracksResponse = api.getStationTracks(
                    waveStation.id.type,
                    waveStation.id.tag
                )
                
                if (tracksResponse.result != null) {
                    val tracks = tracksResponse.result.sequence.map { it.track }
                    Result.success(tracks)
                } else {
                    Result.failure(Exception(tracksResponse.error ?: "Unknown error"))
                }
            } else {
                Result.failure(Exception("Wave station not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStreamUrl(trackId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDownloadInfo(trackId)
            
            if (response.result != null && response.result.isNotEmpty()) {
                // Get best quality stream
                val downloadInfo = response.result
                    .filter { !it.preview }
                    .maxByOrNull { it.bitrateInKbps }
                    ?: response.result.first()
                
                // Parse direct link from download info
                val directUrl = getDirectDownloadUrl(downloadInfo.src)
                Result.success(directUrl)
            } else {
                Result.failure(Exception(response.error ?: "No download info"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getDirectDownloadUrl(xmlUrl: String): String = 
        withContext(Dispatchers.IO) {
            try {
                // This is simplified - in real app you need to parse XML and construct URL
                // For now, return the XML URL as placeholder
                // Real implementation would download XML, parse it, and construct direct URL
                xmlUrl
            } catch (e: Exception) {
                xmlUrl
            }
        }

    suspend fun getUserPlaylists(): Result<List<Playlist>> = withContext(Dispatchers.IO) {
        try {
            val userId = currentUserId ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            val response = api.getUserPlaylists(userId)
            if (response.result != null) {
                Result.success(response.result)
            } else {
                Result.failure(Exception(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylistTracks(userId: String, playlistKind: String): Result<List<Track>> = 
        withContext(Dispatchers.IO) {
            try {
                val response = api.getPlaylist(userId, playlistKind)
                // Note: Real implementation needs to fetch actual tracks from playlist
                // This is simplified version
                Result.success(emptyList())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
