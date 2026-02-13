package com.yaamp.android.data.model

import com.google.gson.annotations.SerializedName

// Track model
data class Track(
    val id: String,
    val title: String,
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    @SerializedName("durationMs")
    val durationMs: Long = 0,
    val coverUri: String? = null,
    val available: Boolean = true
) {
    fun getCoverUrl(size: String = "200x200"): String? {
        return coverUri?.let { "https://${it.replace("%%", size)}" }
    }

    fun getArtistNames(): String {
        return artists.joinToString(", ") { it.name }
    }

    fun getAlbumName(): String {
        return albums.firstOrNull()?.title ?: ""
    }
}

// Artist model
data class Artist(
    val id: String,
    val name: String,
    val coverUri: String? = null
) {
    fun getCoverUrl(size: String = "200x200"): String? {
        return coverUri?.let { "https://${it.replace("%%", size)}" }
    }
}

// Album model
data class Album(
    val id: String,
    val title: String,
    val artists: List<Artist> = emptyList(),
    val coverUri: String? = null,
    val trackCount: Int = 0,
    val year: Int? = null
) {
    fun getCoverUrl(size: String = "400x400"): String? {
        return coverUri?.let { "https://${it.replace("%%", size)}" }
    }
}

// Playlist model
data class Playlist(
    val uid: Long,
    val kind: Int,
    val title: String,
    val trackCount: Int = 0,
    val coverUri: String? = null,
    val owner: Owner? = null
) {
    fun getCoverUrl(size: String = "200x200"): String? {
        return coverUri?.let { "https://${it.replace("%%", size)}" }
    }
}

data class Owner(
    val uid: Long,
    val login: String,
    val name: String
)

// Search result
data class SearchResult(
    val artists: SearchArtists? = null,
    val albums: SearchAlbums? = null,
    val tracks: SearchTracks? = null
)

data class SearchArtists(
    val results: List<Artist> = emptyList(),
    val total: Int = 0
)

data class SearchAlbums(
    val results: List<Album> = emptyList(),
    val total: Int = 0
)

data class SearchTracks(
    val results: List<Track> = emptyList(),
    val total: Int = 0
)

// API Response wrapper
data class YandexMusicResponse<T>(
    val result: T? = null,
    val error: String? = null
)

// Download info for streaming
data class DownloadInfo(
    val codec: String,
    val bitrateInKbps: Int,
    val src: String,
    val gain: Boolean = false,
    val preview: Boolean = false
)

// Station (My Wave)
data class Station(
    val id: StationId,
    val name: String,
    val icon: Icon? = null
)

data class StationId(
    val type: String,
    val tag: String
)

data class Icon(
    val backgroundColor: String,
    val imageUrl: String
)

// Station tracks
data class StationTracks(
    val sequence: List<StationTrack> = emptyList()
)

data class StationTrack(
    val track: Track
)

// User info
data class UserInfo(
    val uid: Long,
    val login: String,
    val displayName: String
)
