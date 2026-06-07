package com.immichframe.app

/** Magic numbers that influence the slideshow behaviour, gathered in one place. */
object SlideshowDefaults {

    /** Auto-advance interval for image pages. */
    const val SLIDE_INTERVAL_MS: Long = 30_000L

    /** Controls overlay auto-hide. Restart timer on each user interaction. */
    const val CONTROLS_TIMEOUT_MS: Long = 5_000L

    /** Swipe-up hint visibility once per controls reveal. */
    const val HINT_TIMEOUT_MS: Long = 3_000L

    /** Crossfade duration during 30s auto-advance. */
    const val CROSSFADE_DURATION_MS: Int = 800

    /** Pager virtual count gives the carousel its "infinite" feel. */
    const val PAGER_VIRTUAL_COUNT: Int = 10_000

    /** Weather polling interval — matches Open-Meteo's update cadence. */
    const val WEATHER_REFRESH_MS: Long = 15L * 60_000L

    /** Maximum allowed pinch-zoom scale on images. */
    const val MAX_IMAGE_SCALE: Float = 5f

    /** Slop the user needs to drag up before the slideshow exits to album picker. */
    const val SWIPE_UP_EXIT_THRESHOLD_DP: Int = 120

    /** Mute hit area in the bottom-right corner. */
    const val MUTE_HIT_SIZE_DP: Int = 240
    const val MUTE_HIT_INSET_DP: Int = 10

    /** Blur transformation parameters for the no-bars fill behind landscape/portrait shots. */
    const val BLUR_RADIUS: Float = 25f
    const val BLUR_SAMPLING: Float = 6f
}
