package com.immichframe.app

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Koin graph for the whole app. */
val appModule = module {
    single { ImmichClient() }
    single { WeatherApi() }
    single { SettingsRepository(androidContext()) }
    single { ImmichRepository(androidContext(), get()) }
}
