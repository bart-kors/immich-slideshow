package com.immichframe.app.ui.slideshow

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.immichframe.app.SlideshowDefaults

/**
 * Mutable, observable pinch-and-pan state for the current image page.
 * Lifted out of SlideshowScreen so the screen body stays readable.
 */
@Stable
class ZoomState {
    var scale by mutableStateOf(1f)
        private set
    var offsetX by mutableStateOf(0f)
        private set
    var offsetY by mutableStateOf(0f)
        private set

    val isZoomed: Boolean get() = scale > 1f

    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    /** Apply a transform delta from a 2-finger pinch (or a 1-finger pan while zoomed in). */
    fun apply(newScale: Float, panX: Float, panY: Float) {
        scale = newScale.coerceIn(1f, SlideshowDefaults.MAX_IMAGE_SCALE)
        if (scale > 1f) {
            offsetX += panX
            offsetY += panY
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }
}

/**
 * Tracks the controls overlay + swipe-up hint timers.
 *
 *  - showControls extends on each user interaction (tap / next / prev).
 *  - showHint only fires once per controls reveal and never extends.
 */
@Stable
class ControlsState {
    var showControls by mutableStateOf(false)
        private set
    var showHint by mutableStateOf(false)
        private set

    /** Bumped on every user interaction to reset the controls auto-hide timer. */
    var interactionNonce by mutableStateOf(0)
        private set

    /** Called when the user taps anywhere outside the mute hit area. */
    fun onUserTap() {
        if (!showControls) showHint = true
        showControls = true
        interactionNonce++
    }

    /** Called by next/prev button — extends controls visibility but not the hint. */
    fun onUserNavigated() {
        interactionNonce++
    }

    fun hideControls() {
        showControls = false
    }

    fun hideHint() {
        showHint = false
    }
}
