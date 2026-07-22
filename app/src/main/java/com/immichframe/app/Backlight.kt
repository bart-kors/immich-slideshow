package com.immichframe.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

    private val suCandidates = listOf(
        "/system/xbin/su",
        "/system/bin/su",
        "/sbin/su",
        "su",
    )

    private fun runSu(cmd: String): Boolean {
        for (path in suCandidates) {
            val outcome = runCatching {
                val proc = ProcessBuilder(path, "-c", cmd)
                    .redirectErrorStream(true)
                    .start()
                try {
                    // API 23 has no waitFor(timeout). Poll exitValue() instead.
                    val deadline = System.nanoTime() + 2_000_000_000L
                    var exit: Int? = null
                    while (System.nanoTime() < deadline) {
                        try { exit = proc.exitValue(); break } catch (_: IllegalThreadStateException) {
                            Thread.sleep(50)
                        }
                    }
                    if (exit == null) {
                        "TIMEOUT"
                    } else {
                        val out = proc.inputStream.bufferedReader().use { it.readText().trim() }
                        if (exit == 0) "OK" else "exit=$exit out=$out"
                    }
                } finally {
                    // destroy() closes the process's stdio fds on every path,
                    // including timeout and thrown exceptions.
                    proc.destroy()
                }
            }.getOrElse { e -> "throw=${e::class.simpleName}:${e.message}" }
            android.util.Log.i("Backlight", "$path → $outcome for: $cmd")
            if (outcome == "OK") return true
        }
        return false
    }
}
