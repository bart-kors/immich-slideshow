package com.immichframe.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.app.Activity
import android.os.PowerManager
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.immichframe.app.ui.AlbumsScreen
import com.immichframe.app.ui.ImmichFrameTheme
import com.immichframe.app.ui.SettingsScreen
import com.immichframe.app.ui.SlideshowScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        setContent {
            ImmichFrameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black,
                    contentColor = Color(0xFFE6E6E6),
                ) {
                    AppRoot()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            WindowInsetsControllerCompat(window, window.decorView)
                .hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun applyScreenState(window: Window, asleep: Boolean) {
    val attrs = window.attributes
    attrs.screenBrightness = if (asleep) 0f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    window.attributes = attrs
    if (asleep) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Suppress("DEPRECATION")
private fun wakeScreen(activity: Activity) {
    val pm = activity.getSystemService(android.content.Context.POWER_SERVICE) as? PowerManager ?: return
    val flags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
        PowerManager.ACQUIRE_CAUSES_WAKEUP or
        PowerManager.ON_AFTER_RELEASE
    val wl = pm.newWakeLock(flags, "ImmichSlideshow:wake")
    runCatching {
        wl.acquire(3_000L)
        wl.release()
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val settingsRepo: SettingsRepository = org.koin.compose.koinInject()
    val immichRepo: ImmichRepository = org.koin.compose.koinInject()
    val settings by settingsRepo.settings.collectAsState(initial = null)
    val nav = rememberNavController()

    val current = settings
    if (current == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        return
    }

    @Suppress("ProduceStateDoesNotAssignValue")
    val scheduledAsleep by produceState(SleepSchedule.isAsleepNow(current), current) {
        value = SleepSchedule.isAsleepNow(current)
        while (true) {
            delay(30_000)
            value = SleepSchedule.isAsleepNow(current)
        }
    }

    var manualWake by remember { mutableStateOf(false) }
    LaunchedEffect(scheduledAsleep) {
        if (!scheduledAsleep) manualWake = false
    }

    val asleep = scheduledAsleep && !manualWake

    val activity = context as? Activity
    val window = activity?.window
    DisposableEffect(asleep, window) {
        if (window != null) applyScreenState(window, asleep)
        if (!asleep && activity != null) wakeScreen(activity)
        onDispose {
            window?.let { applyScreenState(it, false) }
        }
    }

    if (asleep) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        manualWake = true
                    }
                },
        )
        return
    }

    val startDestination = when {
        current.serverUrl.isBlank() || current.apiKey.isBlank() -> "settings"
        current.selectedAlbumId.isBlank() -> "albums"
        else -> "slideshow/${current.selectedAlbumId}"
    }

    NavHost(navController = nav, startDestination = startDestination) {
        composable("settings") {
            SettingsScreen(
                settingsRepository = settingsRepo,
                immichRepository = immichRepo,
                onSaved = {
                    nav.navigate("albums") {
                        popUpTo("settings") { inclusive = true }
                    }
                },
            )
        }
        composable("albums") {
            AlbumsScreen(
                settingsRepository = settingsRepo,
                immichRepository = immichRepo,
                serverUrl = current.serverUrl,
                apiKey = current.apiKey,
                onAlbumPicked = { albumId ->
                    nav.navigate("slideshow/$albumId") {
                        popUpTo("albums") { inclusive = true }
                    }
                },
                onOpenSettings = {
                    nav.navigate("settings")
                },
            )
        }
        composable(
            route = "slideshow/{albumId}",
            arguments = listOf(navArgument("albumId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getString("albumId").orEmpty()
            SlideshowScreen(
                albumId = albumId,
                serverUrl = current.serverUrl,
                apiKey = current.apiKey,
                immichRepository = immichRepo,
                uiSettings = current.toSlideshowUiSettings(),
                onExit = {
                    nav.navigate("albums") {
                        popUpTo("slideshow/$albumId") { inclusive = true }
                    }
                },
            )
        }
    }
}
