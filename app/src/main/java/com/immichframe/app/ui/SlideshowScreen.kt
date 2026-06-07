package com.immichframe.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.immichframe.app.AssetDto
import com.immichframe.app.AssetType
import com.immichframe.app.BlurTransformation
import com.immichframe.app.ImmichClient
import com.immichframe.app.ImmichRepository
import com.immichframe.app.SlideshowDefaults
import com.immichframe.app.SlideshowUiSettings
import com.immichframe.app.WeatherApi
import com.immichframe.app.WeatherSnapshot
import com.immichframe.app.assetType
import kotlinx.coroutines.delay

private val SLIDE_INTERVAL_MS = SlideshowDefaults.SLIDE_INTERVAL_MS
private val CONTROLS_TIMEOUT_MS = SlideshowDefaults.CONTROLS_TIMEOUT_MS
private val HINT_TIMEOUT_MS = SlideshowDefaults.HINT_TIMEOUT_MS
private val PAGER_VIRTUAL_COUNT = SlideshowDefaults.PAGER_VIRTUAL_COUNT

@Composable
fun SlideshowScreen(
    albumId: String,
    serverUrl: String,
    apiKey: String,
    immichRepository: ImmichRepository,
    uiSettings: SlideshowUiSettings,
    onExit: () -> Unit,
) {
    val blurredBackground = uiSettings.blurredBackground
    val cropLandscape = uiSettings.cropLandscape
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var assets by remember { mutableStateOf<List<AssetDto>?>(null) }
    var albumName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val controls = remember { com.immichframe.app.ui.slideshow.ControlsState() }
    val showControls = controls.showControls
    val showHint = controls.showHint
    val interactionNonce = controls.interactionNonce
    var videoPaused by remember { mutableStateOf(false) }
    var videoMuted by remember { mutableStateOf(true) }
    val zoom = remember { com.immichframe.app.ui.slideshow.ZoomState() }
    val imageScale = zoom.scale
    val imageOffsetX = zoom.offsetX
    val imageOffsetY = zoom.offsetY

    val pagerStartPage = remember { PAGER_VIRTUAL_COUNT / 2 }
    val pagerState = rememberPagerState(initialPage = pagerStartPage) { PAGER_VIRTUAL_COUNT }
    val assetCount = assets?.size ?: 0
    val currentIndex = if (assetCount == 0) 0 else
        ((pagerState.currentPage - pagerStartPage) % assetCount + assetCount) % assetCount
    val nextIndex = if (assetCount == 0) 0 else (currentIndex + 1) % assetCount

    val crossfadeAlpha = remember { Animatable(0f) }
    var autoAdvancing by remember { mutableStateOf(false) }

    val immichClient = com.immichframe.app.LocalImmichClient.current
    val okHttp = remember(immichClient, apiKey) { immichClient.okHttp(apiKey) }
    val imageLoader = remember(apiKey) {
        ImageLoader.Builder(context)
            .okHttpClient(okHttp)
            .crossfade(true)
            .build()
    }

    LaunchedEffect(albumId, serverUrl, apiKey) {
        assets = null
        error = null
        try {
            val loaded = immichRepository.loadAlbumAssets(serverUrl, apiKey, albumId)
            albumName = loaded.albumName
            assets = loaded.assets
            pagerState.scrollToPage(pagerStartPage)
        } catch (t: Throwable) {
            error = t.message ?: t.toString()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        videoPaused = false
        videoMuted = true
        zoom.reset()
    }

    BackHandler { onExit() }

    val advance: () -> Unit = {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }
    val goPrevious: () -> Unit = {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    val currentAsset = assets?.getOrNull(currentIndex)
    val nextAsset = assets?.getOrNull(nextIndex)

    // 30 s auto-advance timer (images only, paused while zoomed)
    LaunchedEffect(pagerState.currentPage, imageScale > 1f, currentAsset?.id) {
        if (imageScale <= 1f && currentAsset?.assetType() == AssetType.IMAGE) {
            delay(SLIDE_INTERVAL_MS)
            autoAdvancing = true
        }
    }

    // Crossfade driver: fade overlay in, snap pager forward, fade overlay out
    LaunchedEffect(autoAdvancing) {
        if (autoAdvancing) {
            crossfadeAlpha.snapTo(0f)
            crossfadeAlpha.animateTo(1f, animationSpec = tween(800))
            pagerState.scrollToPage(pagerState.currentPage + 1)
            crossfadeAlpha.snapTo(0f)
            autoAdvancing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when {
            error != null -> ErrorState(error!!, onExit)
            assets == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
            assets!!.isEmpty() -> EmptyState(onExit)
            else -> SlideshowContent(
                assets = assets!!,
                pagerState = pagerState,
                pagerStartPage = pagerStartPage,
                serverUrl = serverUrl,
                imageLoader = imageLoader,
                okHttp = okHttp,
                videoPaused = videoPaused,
                videoMuted = videoMuted,
                imageScale = imageScale,
                imageOffsetX = imageOffsetX,
                imageOffsetY = imageOffsetY,
                onImageTransform = { newScale, panX, panY -> zoom.apply(newScale, panX, panY) },
                onAdvance = advance,
                onTap = {
                    val isVideoNow = assets?.getOrNull(currentIndex)?.assetType() == AssetType.VIDEO
                    if (isVideoNow) videoPaused = !videoPaused
                    controls.onUserTap()
                },
                onSwipeUp = onExit,
                blurredBackground = blurredBackground,
                cropLandscape = cropLandscape,
            )
        }

        // Crossfade overlay for the 30s auto-advance: fades the next image in over the pager.
        // Uses the same blurred-bg + Fit-fg layering so the fade never shows black bars.
        if (crossfadeAlpha.value > 0f && nextAsset != null && nextAsset.assetType() == AssetType.IMAGE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(crossfadeAlpha.value),
            ) {
                if (blurredBackground) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(ImmichClient.previewUrl(serverUrl, nextAsset.id))
                            .transformations(BlurTransformation(context, radius = 25f, sampling = 6f))
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {},
                        error = {},
                    )
                }
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(ImmichClient.previewUrl(serverUrl, nextAsset.id))
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = nextAsset.originalFileName,
                    modifier = Modifier.fillMaxSize(),
                    loading = {},
                    error = {},
                    success = { state ->
                        val intrinsic = state.painter.intrinsicSize
                        val isLandscape = intrinsic.isSpecified &&
                            intrinsic.width > intrinsic.height
                        SubcomposeAsyncImageContent(
                            contentScale = if (cropLandscape && isLandscape)
                                ContentScale.Crop else ContentScale.Fit,
                        )
                    },
                )
            }
        }

        val currentIsVideo = assets?.getOrNull(currentIndex)?.assetType() == AssetType.VIDEO

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ControlsOverlay(
                albumName = albumName,
                onNext = {
                    advance()
                    controls.onUserNavigated()
                },
                onPrevious = {
                    goPrevious()
                    controls.onUserNavigated()
                },
            )
        }

        AnimatedVisibility(
            visible = showHint,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            HintOverlay(modifier = Modifier.fillMaxSize())
        }

        if (showControls) {
            LaunchedEffect(interactionNonce) {
                delay(CONTROLS_TIMEOUT_MS)
                controls.hideControls()
            }
        }

        if (showHint) {
            LaunchedEffect(showHint) {
                delay(HINT_TIMEOUT_MS)
                controls.hideHint()
            }
        }

        DateTimeOverlay(modifier = Modifier.align(Alignment.TopEnd))
        WeatherOverlay(
            latitude = uiSettings.weatherLatitude,
            longitude = uiSettings.weatherLongitude,
            modifier = Modifier.align(Alignment.TopStart),
        )

        if (!showHint) {
            assets?.getOrNull(currentIndex)?.let { current ->
                AssetMetaOverlay(asset = current, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }

        if (currentIsVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .size(240.dp)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = true)
                            down.consume()
                            val up = waitForUpOrCancellation()
                            if (up != null) {
                                up.consume()
                                videoMuted = !videoMuted
                            }
                        }
                    },
                contentAlignment = Alignment.BottomEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .background(Color(0xCC000000), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (videoMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                        contentDescription = if (videoMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(UnstableApi::class)
@Composable
private fun SlideshowContent(
    assets: List<AssetDto>,
    pagerState: PagerState,
    pagerStartPage: Int,
    serverUrl: String,
    imageLoader: ImageLoader,
    okHttp: okhttp3.OkHttpClient,
    videoPaused: Boolean,
    videoMuted: Boolean,
    imageScale: Float,
    imageOffsetX: Float,
    imageOffsetY: Float,
    onImageTransform: (newScale: Float, panX: Float, panY: Float) -> Unit,
    onAdvance: () -> Unit,
    onTap: () -> Unit,
    onSwipeUp: () -> Unit,
    blurredBackground: Boolean,
    cropLandscape: Boolean,
) {
    val n = assets.size
    val currentAsset = assets[((pagerState.currentPage - pagerStartPage) % n + n) % n]

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 2,
        userScrollEnabled = imageScale <= 1f,
        key = { page -> page },
    ) { page ->
        val assetIndex = ((page - pagerStartPage) % n + n) % n
        val asset = assets[assetIndex]
        val isCurrent = page == pagerState.currentPage
        SlideshowPage(
            asset = asset,
            isCurrent = isCurrent,
            serverUrl = serverUrl,
            imageLoader = imageLoader,
            okHttp = okHttp,
            videoPaused = videoPaused,
            videoMuted = videoMuted,
            imageScale = if (isCurrent) imageScale else 1f,
            imageOffsetX = if (isCurrent) imageOffsetX else 0f,
            imageOffsetY = if (isCurrent) imageOffsetY else 0f,
            onImageTransform = onImageTransform,
            onCompleted = onAdvance,
            onTap = onTap,
            onSwipeUp = onSwipeUp,
            blurredBackground = blurredBackground,
            cropLandscape = cropLandscape,
        )
    }

    if (currentAsset.assetType() == AssetType.OTHER) {
        LaunchedEffect(pagerState.currentPage) { onAdvance() }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(UnstableApi::class)
@Composable
private fun SlideshowPage(
    asset: AssetDto,
    isCurrent: Boolean,
    serverUrl: String,
    imageLoader: ImageLoader,
    okHttp: okhttp3.OkHttpClient,
    videoPaused: Boolean,
    videoMuted: Boolean,
    imageScale: Float,
    imageOffsetX: Float,
    imageOffsetY: Float,
    onImageTransform: (newScale: Float, panX: Float, panY: Float) -> Unit,
    onCompleted: () -> Unit,
    onTap: () -> Unit,
    onSwipeUp: () -> Unit,
    blurredBackground: Boolean,
    cropLandscape: Boolean,
) {
    val context = LocalContext.current
    when (asset.assetType()) {
        AssetType.IMAGE -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        onClick = onTap,
                    )
                    .pointerInput(asset.id) {
                        var totalY = 0f
                        val threshold = 120.dp.toPx()
                        detectVerticalDragGestures(
                            onDragStart = { totalY = 0f },
                            onDragEnd = { if (totalY <= -threshold) onSwipeUp() },
                            onDragCancel = { totalY = 0f },
                            onVerticalDrag = { _, dragAmount -> totalY += dragAmount },
                        )
                    }
                    .pointerInput(asset.id) {
                        // Multi-touch only pinch detector — does nothing while a single
                        // finger is down so swipes pass through to the pager.
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.count { it.pressed }
                                if (pressed == 0) break
                                if (pressed >= 2) {
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    val newScale = (imageScale * zoom).coerceIn(1f, 5f)
                                    onImageTransform(newScale, pan.x, pan.y)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    },
            ) {
                if (blurredBackground) {
                    // Blurred fill background — same image cropped + blurred so there are no black bars.
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(ImmichClient.previewUrl(serverUrl, asset.id))
                            .transformations(BlurTransformation(context, radius = 25f, sampling = 6f))
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        loading = {},
                        error = {},
                    )
                }
                // Sharp foreground — Fit by default; Crop for landscape when the user opted in.
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(ImmichClient.previewUrl(serverUrl, asset.id))
                        .crossfade(400)
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = asset.originalFileName,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = imageScale
                            scaleY = imageScale
                            translationX = imageOffsetX
                            translationY = imageOffsetY
                        },
                    loading = { SpinnerOverlay() },
                    error = { SpinnerOverlay() },
                    success = { state ->
                        val intrinsic = state.painter.intrinsicSize
                        val isLandscape = intrinsic.isSpecified &&
                            intrinsic.width > intrinsic.height
                        SubcomposeAsyncImageContent(
                            contentScale = if (cropLandscape && isLandscape)
                                ContentScale.Crop else ContentScale.Fit,
                        )
                    },
                )
            }
        }
        AssetType.VIDEO -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        onClick = onTap,
                    )
                    .pointerInput(asset.id) {
                        var totalY = 0f
                        val threshold = 120.dp.toPx()
                        detectVerticalDragGestures(
                            onDragStart = { totalY = 0f },
                            onDragEnd = { if (totalY <= -threshold) onSwipeUp() },
                            onDragCancel = { totalY = 0f },
                            onVerticalDrag = { _, dragAmount -> totalY += dragAmount },
                        )
                    },
            ) {
                if (isCurrent) {
                    VideoPlayer(
                        url = ImmichClient.videoPlaybackUrl(serverUrl, asset.id),
                        okHttp = okHttp,
                        paused = videoPaused,
                        muted = videoMuted,
                        onCompleted = onCompleted,
                    )
                }
            }
        }
        AssetType.OTHER -> {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    url: String,
    okHttp: okhttp3.OkHttpClient,
    paused: Boolean,
    muted: Boolean,
    onCompleted: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val player = remember(url) {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttp)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 30_000,
                /* bufferForPlaybackMs = */ 2_000,
                /* bufferForPlaybackAfterRebufferMs = */ 5_000,
            )
            .setTargetBufferBytes(C.LENGTH_UNSET)
            .build()
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setLoadControl(loadControl)
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                setMediaItem(MediaItem.fromUri(url))
                volume = 0f
                prepare()
                playWhenReady = true
            }
    }

    var buffering by remember(url) { mutableStateOf(true) }
    var hasStartedPlaying by remember(url) { mutableStateOf(false) }
    var advanceRequested by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        delay(20_000)
        if (!hasStartedPlaying) advanceRequested = true
    }

    LaunchedEffect(url, advanceRequested) {
        if (advanceRequested) {
            // give MediaCodec / MediaServer a beat to clean up before the next ExoPlayer instance is built
            delay(1_000)
            onCompleted()
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                buffering = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
                if (state == Player.STATE_READY) hasStartedPlaying = true
                if (state == Player.STATE_ENDED) advanceRequested = true
            }

            override fun onPlayerError(error: PlaybackException) {
                buffering = false
                advanceRequested = true
            }
        }
        player.addListener(listener)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            player.removeListener(listener)
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    LaunchedEffect(paused, player) {
        player.playWhenReady = !paused
    }

    LaunchedEffect(muted, player) {
        player.volume = if (muted) 0f else 1f
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            update = { view -> view.player = player },
        )
        if (buffering) {
            SpinnerOverlay()
        }
    }
}

@Composable
private fun ControlsOverlay(
    albumName: String,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val solid = Color(0xFF000000)
    Box(modifier = Modifier.fillMaxSize()) {
        // Top: album name centered
        if (albumName.isNotBlank()) {
            Text(
                text = albumName,
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
                    .background(Color(0x66000000), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            )
        }

        // Middle-left: Previous
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .size(120.dp)
                .background(solid, CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = true).consume()
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            up.consume()
                            onPrevious()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Previous",
                tint = Color.White,
                modifier = Modifier.size(72.dp),
            )
        }


        // Middle-right: Next
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp)
                .size(120.dp)
                .background(solid, CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = true).consume()
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            up.consume()
                            onNext()
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Next",
                tint = Color.White,
                modifier = Modifier.size(72.dp),
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Failed to load album", color = Color.White, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color(0xFFFF8888), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        IconButton(onClick = onExit) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}

@Composable
private fun EmptyState(onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("This album has no photos or videos.", color = Color.White)
        Spacer(Modifier.height(16.dp))
        IconButton(onClick = onExit) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
    }
}

// Overlays moved to com.immichframe.app.ui.slideshow.* — re-exported below for backward compatibility.
@Composable private fun DateTimeOverlay(modifier: Modifier = Modifier) =
    com.immichframe.app.ui.slideshow.DateTimeOverlay(modifier)

@Composable private fun WeatherOverlay(latitude: Double, longitude: Double, modifier: Modifier = Modifier) =
    com.immichframe.app.ui.slideshow.WeatherOverlay(latitude, longitude, modifier)

@Composable private fun HintOverlay(modifier: Modifier = Modifier) =
    com.immichframe.app.ui.slideshow.HintOverlay(modifier)

@Composable private fun AssetMetaOverlay(asset: AssetDto, modifier: Modifier = Modifier) =
    com.immichframe.app.ui.slideshow.AssetMetaOverlay(asset, modifier)

@Composable private fun SpinnerOverlay() =
    com.immichframe.app.ui.slideshow.SpinnerOverlay()

