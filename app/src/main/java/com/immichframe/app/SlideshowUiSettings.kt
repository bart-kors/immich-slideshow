package com.immichframe.app

/** Read-only flags consumed by SlideshowScreen and its children. */
data class SlideshowUiSettings(
    val blurredBackground: Boolean,
    val cropLandscape: Boolean,
    val kenBurnsEffect: Boolean,
    val weatherLatitude: Double,
    val weatherLongitude: Double,
)

fun ImmichSettings.toSlideshowUiSettings(): SlideshowUiSettings = SlideshowUiSettings(
    blurredBackground = blurredBackground,
    cropLandscape = cropLandscape,
    kenBurnsEffect = kenBurnsEffect,
    weatherLatitude = weatherLatitude,
    weatherLongitude = weatherLongitude,
)
