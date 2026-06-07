package com.immichframe.app

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Composition-root container. Constructed once in MainActivity and exposed via
 * CompositionLocals so screens don't need to receive each dependency as a
 * constructor parameter.
 */
class AppContainer(appContext: Context) {
    val immichClient: ImmichClient = ImmichClient()
    val weatherApi: WeatherApi = WeatherApi()
    val settingsRepository: SettingsRepository = SettingsRepository(appContext)
    val immichRepository: ImmichRepository = ImmichRepository(appContext, immichClient)
}

val LocalImmichClient = staticCompositionLocalOf<ImmichClient> {
    error("LocalImmichClient not provided")
}

val LocalWeatherApi = staticCompositionLocalOf<WeatherApi> {
    error("LocalWeatherApi not provided")
}
