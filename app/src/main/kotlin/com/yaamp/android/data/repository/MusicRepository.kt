package com.yaamp.android.data.repository

import com.yaamp.android.data.api.YandexMusicClient
import com.yaamp.android.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest

class MusicRepository {

    private val api = YandexMusicClient.api
    private val httpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
    private var currentUserId: String? = null

    suspend fun setAuthToken(token: String) = withContext(Dispatchers.IO) {
        YandexMusicClient.setAuthToken(token)

        // Get user info
        val response = api.getAccountStatus()
        val userId = response.result?.account?.uid
            ?: throw IllegalStateException(response.error ?: "Invalid auth token")
        if (userId <= 0) {
            throw IllegalStateException("Invalid account uid: $userId")
        }
        currentUserId = userId.toString()
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
                val trackIds = trackShorts.mapNotNull { track ->
                    val albumId = track.albumId
                    if (albumId.isNullOrBlank()) {
                        null
                    } else {
                        "${track.id}:$albumId"
                    }
                }

                if (trackIds.isEmpty()) {
                    return@withContext Result.failure(
                        Exception("Liked tracks response missing albumId values")
                    )
                }

                val tracksResponse = api.getTracks(trackIds.joinToString(","))

                if (tracksResponse.result != null) {
                    Result.success(tracksResponse.result)
                } else {
                    Result.failure(Exception(tracksResponse.error ?: "Unknown error"))
                }
            } else {
                val isUnauthorized = response.error?.contains("Unauthorized", ignoreCase = true) == true
                if (isUnauthorized) {
                    Result.failure(Exception("Authorization expired. Please sign in again."))
                } else {
                    Result.success(emptyList())
                }
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
                val directUrl = getDirectDownloadUrl(downloadInfo.downloadInfoUrl)
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
            val request = Request.Builder()
                .url(xmlUrl)
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            response.close()

            val host = Regex("<host>(.+?)</host>").find(body)?.groupValues?.get(1)
            val path = Regex("<path>(.+?)</path>").find(body)?.groupValues?.get(1)
            val ts = Regex("<ts>(.+?)</ts>").find(body)?.groupValues?.get(1)
            val s = Regex("<s>(.+?)</s>").find(body)?.groupValues?.get(1)

            if (host == null || path == null || ts == null || s == null) {
                return@withContext xmlUrl
            }

            val secret = "XGRlBW9FXlekgbPrRHuSiA" + path.drop(1) + s
            val hash = MessageDigest.getInstance("MD5")
                .digest(secret.toByteArray())
                .joinToString("") { "%02x".format(it) }

            "https://$host/get-mp3/$hash/$ts$path"
        }

    fun clearAuthToken() {
        currentUserId = null
        YandexMusicClient.setAuthToken("")
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
