package com.immichframe.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Drives the panel backlight directly via the Rockchip sysfs node so the
 * sleep schedule can take the backlight all the way to zero. The normal
 * Settings.System.SCREEN_BRIGHTNESS API is clamped above the panel's
 * vendor-defined minimum (~12% on the PFF-1042LW), which is not "off".
 *
 * Requires root (`su`). On devices without root, every call is a no-op
 * and the caller falls back to the window-level brightness override.
 */
object Backlight {

    private const val NODE = "/sys/class/backlight/rk28_bl/brightness"
    private const val SAFE_RESTORE = 200

    @Volatile
    private var savedValue: Int? = null

    suspend fun off(): Boolean = withContext(Dispatchers.IO) {
        savedValue = readCurrent()?.takeIf { it > 0 } ?: savedValue
        runSu("echo 0 > $NODE")
    }

    suspend fun restore(): Boolean = withContext(Dispatchers.IO) {
        val target = savedValue ?: SAFE_RESTORE
        runSu("echo $target > $NODE")
    }

    /** Called at app start to undo a stuck-at-zero backlight from a prior crash. */
    suspend fun ensureOnAtBoot(): Boolean = withContext(Dispatchers.IO) {
        val current = readCurrent() ?: return@withContext false
        if (current <= 0) runSu("echo $SAFE_RESTORE > $NODE") else true
    }

    private fun readCurrent(): Int? = runCatching {
        File(NODE).readText().trim().toIntOrNull()
    }.getOrNull()

    private fun runSu(cmd: String): Boolean = runCatching {
        val proc = ProcessBuilder("su", "-c", cmd)
            .redirectErrorStream(true)
            .start()
        // If su isn't available or the supervisor is waiting on a grant
        // prompt, don't hang forever — bail after 2 s.
        if (proc.waitFor(2, TimeUnit.SECONDS)) {
            proc.exitValue() == 0
        } else {
            proc.destroyForcibly()
            false
        }
    }.getOrElse { false }
}
