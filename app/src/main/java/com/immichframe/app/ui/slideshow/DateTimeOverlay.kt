package com.immichframe.app.ui.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Top-right pill showing local time + Dutch-locale date. Refreshes every 30 s. */
@Composable
internal fun DateTimeOverlay(modifier: Modifier = Modifier) {
    val nlLocale = remember { Locale("nl", "NL") }
    val dateFmt = remember(nlLocale) { SimpleDateFormat("d MMMM yyyy", nlLocale) }
    val timeFmt = remember(nlLocale) { SimpleDateFormat("HH:mm", nlLocale) }

    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(30_000)
        }
    }

    val dateText = dateFmt.format(now)
        .replaceFirstChar { if (it.isLowerCase()) it.uppercase(nlLocale) else it.toString() }
    val timeText = timeFmt.format(now)

    Column(
        modifier = modifier
            .padding(top = 24.dp, end = 24.dp)
            .background(Color(0x66000000), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(text = timeText, color = Color.White, style = MaterialTheme.typography.headlineMedium)
        Text(text = dateText, color = Color(0xFFE6E6E6), style = MaterialTheme.typography.bodyMedium)
    }
}
