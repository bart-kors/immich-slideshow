package com.immichframe.app.ui.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.immichframe.app.AssetDto
import java.text.SimpleDateFormat
import java.util.Locale

/** Bottom-center pill showing the asset's location + date taken (or fallbacks). */
@Composable
internal fun AssetMetaOverlay(asset: AssetDto, modifier: Modifier = Modifier) {
    val location = listOfNotNull(asset.exifInfo?.city, asset.exifInfo?.country)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val rawDate = asset.exifInfo?.dateTimeOriginal
        ?: asset.localDateTime
        ?: asset.fileCreatedAt
    val formattedDate = remember(rawDate) { formatIsoToDutch(rawDate) }
    if (location.isBlank() && formattedDate.isBlank()) return

    Column(
        modifier = modifier
            .padding(bottom = 24.dp)
            .background(Color(0x66000000), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (location.isNotBlank()) {
            Text(text = location, color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
        if (formattedDate.isNotBlank()) {
            Text(
                text = formattedDate,
                color = Color(0xFFE6E6E6),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private val isoCandidates = arrayOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss",
)

internal fun formatIsoToDutch(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val nl = Locale("nl", "NL")
    val out = SimpleDateFormat("d MMMM yyyy", nl)
    for (pattern in isoCandidates) {
        runCatching {
            val parsed = SimpleDateFormat(pattern, Locale.US).parse(raw)
            if (parsed != null) return out.format(parsed)
        }
    }
    return ""
}
