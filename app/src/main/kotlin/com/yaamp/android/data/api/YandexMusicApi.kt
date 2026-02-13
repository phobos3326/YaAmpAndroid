package com.yaamp.android.data.api

import com.yaamp.android.data.model.*
import retrofit2.http.*

interface YandexMusicApi {

    // Search
    @GET("search")
    suspend fun search(
        @Query("text") query: String,
        @Query("type") type: String = "all", // all, artist, album, track
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20
    ): YandexMusicResponse<SearchResult>

    // Get artist by ID
    @GET("artists/{artistId}")
    suspend fun getArtist(
        @Path("artistId") artistId: String
    ): YandexMusicResponse<Artist>

    // Get artist tracks
    @GET("artists/{artistId}/tracks")
    suspend fun getArtistTracks(
        @Path("artistId") artistId: String,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20
    ): YandexMusicResponse<TracksResult>

    // Get album by ID with tracks
    @GET("albums/{albumId}/with-tracks")
    suspend fun getAlbumWithTracks(
        @Path("albumId") albumId: String
    ): YandexMusicResponse<AlbumFull>

    // Get liked tracks
    @GET("users/{userId}/likes/tracks")
    suspend fun getLikedTracks(
        @Path("userId") userId: String
    ): YandexMusicResponse<LikedTracksResult>

    // Get user playlists
    @GET("users/{userId}/playlists/list")
    suspend fun getUserPlaylists(
        @Path("userId") userId: String
    ): YandexMusicResponse<List<Playlist>>

    // Get playlist
    @GET("users/{userId}/playlists/{playlistKind}")
    suspend fun getPlaylist(
        @Path("userId") userId: String,
        @Path("playlistKind") playlistKind: String
    ): YandexMusicResponse<Playlist>

    // Get tracks by IDs
    @POST("tracks")
    suspend fun getTracks(
        @Body trackIds: List<String>
    ): YandexMusicResponse<List<Track>>

    // Get download info
    @GET("tracks/{trackId}/download-info")
    suspend fun getDownloadInfo(
        @Path("trackId") trackId: String
    ): YandexMusicResponse<List<DownloadInfo>>

    // Get stations (My Wave)
    @GET("rotor/stations/list")
    suspend fun getStations(): YandexMusicResponse<List<Station>>

    // Get station tracks
    @GET("rotor/station/{stationType}:{stationTag}/tracks")
    suspend fun getStationTracks(
        @Path("stationType") stationType: String,
        @Path("stationTag") stationTag: String,
        @Query("queue") queue: String? = null
    ): YandexMusicResponse<StationTracks>

    // Get account info
    @GET("account/status")
    suspend fun getAccountStatus(): YandexMusicResponse<UserInfo>
}

// Additional data classes for API responses
data class TracksResult(
    val tracks: List<Track> = emptyList(),
    val pager: Pager? = null
)

data class AlbumFull(
    val id: String,
    val title: String,
    val artists: List<Artist> = emptyList(),
    val coverUri: String? = null,
    val trackCount: Int = 0,
    val year: Int? = null,
    val volumes: List<List<Track>> = emptyList()
) {
    fun getAllTracks(): List<Track> {
        return volumes.flatten()
    }
}

data class LikedTracksResult(
    val library: TracksList? = null
)

data class TracksList(
    val tracks: List<TrackShort> = emptyList()
)

data class TrackShort(
    val id: String,
    val albumId: String? = null
)

data class Pager(
    val total: Int = 0,
    val page: Int = 0,
    val perPage: Int = 20
)
