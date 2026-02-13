package com.yaamp.android.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yaamp.android.ui.components.*
import com.yaamp.android.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    var showFullPlayer by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when (currentTab) {
                            0 -> "Home"
                            1 -> "Search"
                            2 -> "Playlist"
                            else -> "Yaamp"
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Column {
                // Player controls at bottom
                if (currentTrack != null) {
                    PlayerControls(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onPlayPause = { viewModel.playerManager.playPause() },
                        onNext = { viewModel.playerManager.next() },
                        onPrevious = { viewModel.playerManager.previous() },
                        onExpand = { showFullPlayer = true }
                    )
                }

                // Bottom navigation
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentTab == 0,
                        onClick = { viewModel.setCurrentTab(0) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                        label = { Text("Search") },
                        selected = currentTab == 1,
                        onClick = { viewModel.setCurrentTab(1) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.PlaylistPlay, contentDescription = "Playlist") },
                        label = { Text("Playlist") },
                        selected = currentTab == 2,
                        onClick = { viewModel.setCurrentTab(2) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Loading indicator
            if (uiState is com.yaamp.android.ui.viewmodel.UiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Main content
            when (currentTab) {
                0 -> HomeScreen(
                    onMyWaveClick = { viewModel.playMyWave() },
                    onLikedTracksClick = { viewModel.playLikedTracks() }
                )
                1 -> SearchScreen(
                    query = searchQuery,
                    searchResults = searchResults,
                    onQueryChange = { 
                        searchQuery = it
                        viewModel.search(it)
                    },
                    onArtistClick = { artist ->
                        viewModel.playArtist(artist.id)
                    },
                    onAlbumClick = { album ->
                        viewModel.playAlbum(album.id)
                    },
                    onTrackClick = { track ->
                        // Play single track
                        viewModel.playerManager.setPlaylist(listOf(track))
                    }
                )
                2 -> PlaylistScreen(
                    playlist = playlist,
                    currentIndex = currentIndex,
                    onTrackClick = { index ->
                        // TODO: seek to track
                    }
                )
            }

            // Error snackbar
            if (uiState is com.yaamp.android.ui.viewmodel.UiState.Error) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { /* dismiss */ }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text((uiState as com.yaamp.android.ui.viewmodel.UiState.Error).message)
                }
            }
        }
    }

    // Full player overlay
    if (showFullPlayer) {
        FullPlayerScreen(
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            currentPosition = viewModel.playerManager.getCurrentPosition(),
            duration = viewModel.playerManager.getDuration(),
            onPlayPause = { viewModel.playerManager.playPause() },
            onNext = { viewModel.playerManager.next() },
            onPrevious = { viewModel.playerManager.previous() },
            onSeek = { progress ->
                val position = (viewModel.playerManager.getDuration() * progress).toLong()
                viewModel.playerManager.seekTo(position)
            },
            onClose = { showFullPlayer = false }
        )
    }
}

@Composable
fun HomeScreen(
    onMyWaveClick: () -> Unit,
    onLikedTracksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = onMyWaveClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Waves,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("My Wave", style = MaterialTheme.typography.titleMedium)
        }

        Button(
            onClick = onLikedTracksClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Liked Tracks", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Yaamp - Yandex Music Player",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun SearchScreen(
    query: String,
    searchResults: com.yaamp.android.data.model.SearchResult?,
    onQueryChange: (String) -> Unit,
    onArtistClick: (com.yaamp.android.data.model.Artist) -> Unit,
    onAlbumClick: (com.yaamp.android.data.model.Album) -> Unit,
    onTrackClick: (com.yaamp.android.data.model.Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { /* search already happens on change */ }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (searchResults != null) {
            SearchResults(
                searchResult = searchResults,
                onArtistClick = onArtistClick,
                onAlbumClick = onAlbumClick,
                onTrackClick = onTrackClick,
                modifier = Modifier.weight(1f)
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = "Search for artists, albums, or tracks",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PlaylistScreen(
    playlist: List<com.yaamp.android.data.model.Track>,
    currentIndex: Int,
    onTrackClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (playlist.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No tracks in playlist",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        PlaylistView(
            tracks = playlist,
            currentIndex = currentIndex,
            onTrackClick = onTrackClick,
            modifier = modifier
        )
    }
}
