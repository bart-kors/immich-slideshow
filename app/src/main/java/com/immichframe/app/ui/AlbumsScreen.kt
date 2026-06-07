package com.immichframe.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.immichframe.app.ImmichClient
import com.immichframe.app.ImmichRepository
import com.immichframe.app.SettingsRepository
import com.immichframe.app.db.AlbumEntity
import kotlinx.coroutines.launch

@Composable
fun AlbumsScreen(
    settingsRepository: SettingsRepository,
    immichRepository: ImmichRepository,
    serverUrl: String,
    apiKey: String,
    onAlbumPicked: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val immichClient = com.immichframe.app.LocalImmichClient.current
    val scope = rememberCoroutineScope()
    val albums by immichRepository.observeAlbums().collectAsState(initial = emptyList())
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val imageLoader = remember(apiKey) {
        ImageLoader.Builder(context)
            .okHttpClient(immichClient.okHttp(apiKey))
            .crossfade(true)
            .build()
    }

    LaunchedEffect(serverUrl, apiKey) {
        error = null
        refreshing = true
        try {
            immichRepository.refreshAlbums(serverUrl, apiKey)
        } catch (t: Throwable) {
            error = t.message ?: t.toString()
        } finally {
            refreshing = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Choose an album",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        when {
            albums.isEmpty() && refreshing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            albums.isEmpty() && error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Couldn't load albums", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(error ?: "", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF8888))
                        Spacer(Modifier.height(16.dp))
                        Text("Tap the gear icon to fix the connection.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            albums.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No albums found on this server.")
                }
            }

            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = albums,
                        key = { it.id },
                        contentType = { "album" },
                    ) { album ->
                        AlbumCard(
                            album = album,
                            imageLoader = imageLoader,
                            serverUrl = serverUrl,
                            onClick = {
                                scope.launch {
                                    settingsRepository.setSelectedAlbum(album.id)
                                    onAlbumPicked(album.id)
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(
    album: AlbumEntity,
    imageLoader: ImageLoader,
    serverUrl: String,
    onClick: () -> Unit,
) {
    val bytes = album.thumbnail
    val cardColor = Color(0xFF181B22)
    val thumbBgColor = Color(0xFF222831)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .drawBehind { drawRect(cardColor) }
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .drawBehind { drawRect(thumbBgColor) },
            contentAlignment = Alignment.Center,
        ) {
            if (bytes != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(bytes)
                        .memoryCacheKey("album_${album.id}_${album.thumbnailAssetId}")
                        .diskCacheKey("album_${album.id}_${album.thumbnailAssetId}")
                        .crossfade(false)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = album.albumName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("📷", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.albumName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = "${album.assetCount} items",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB5BAC5),
        )
    }
}
