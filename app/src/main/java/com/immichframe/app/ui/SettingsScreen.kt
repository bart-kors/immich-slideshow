package com.immichframe.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.immichframe.app.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.immichframe.app.CityResult
import com.immichframe.app.ImmichRepository
import com.immichframe.app.SettingsRepository
import com.immichframe.app.SleepSchedule
import com.immichframe.app.WeatherApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface SettingItem {
    val title: String
    val summary: String
    val icon: androidx.compose.ui.graphics.vector.ImageVector

    data class ServerUrl(
        override val title: String,
        override val summary: String,
    ) : SettingItem {
        override val icon = Icons.Filled.Language
    }

    data class ApiKey(
        override val title: String,
        override val summary: String,
    ) : SettingItem {
        override val icon = Icons.Filled.Key
    }

    data class ClearCache(
        override val title: String,
        override val summary: String,
    ) : SettingItem {
        override val icon = Icons.Filled.Delete
    }

    data class OpenSystemSettings(
        override val title: String,
        override val summary: String,
    ) : SettingItem {
        override val icon = Icons.Filled.PhoneAndroid
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    immichRepository: ImmichRepository,
    onSaved: () -> Unit,
) {
    val stored by settingsRepository.settings.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val serverUrl = stored?.serverUrl.orEmpty()
    val apiKey = stored?.apiKey.orEmpty()
    val sleepEnabled = stored?.sleepEnabled ?: false
    val sleepOffTime = stored?.sleepOffTime ?: "23:00"
    val sleepOnTime = stored?.sleepOnTime ?: "08:00"
    val blurredBackground = stored?.blurredBackground ?: true
    val cropLandscape = stored?.cropLandscape ?: false
    val weatherCity = stored?.weatherCityName.orEmpty()
    var showCityPicker by remember { mutableStateOf(false) }

    var editing by remember { mutableStateOf<SettingItem?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var editingTime by remember { mutableStateOf<TimeEdit?>(null) }

    val canContinue = serverUrl.isNotBlank() && apiKey.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    if (canContinue) {
                        IconButton(onClick = onSaved) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                            )
                        }
                    }
                },
                actions = {
                    if (canContinue) {
                        IconButton(onClick = onSaved) {
                            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.common_done))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = Color(0xFF0F1115),
    ) { padding ->
        val notSet = stringResource(R.string.common_not_set)
        val items: List<SettingItem> = listOf(
            SettingItem.ServerUrl(
                title = stringResource(R.string.settings_server_url),
                summary = serverUrl.ifBlank { notSet },
            ),
            SettingItem.ApiKey(
                title = stringResource(R.string.settings_api_key),
                summary = maskApiKey(apiKey, notSet),
            ),
            SettingItem.ClearCache(
                title = stringResource(R.string.settings_clear_cache_title),
                summary = stringResource(R.string.settings_clear_cache_summary),
            ),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                SectionHeader(stringResource(R.string.settings_section_connection))
            }
            items(items.subList(0, 2)) { item ->
                SettingsRow(item = item, onClick = { editing = item })
                HorizontalDivider(color = Color(0xFF1F232C))
            }
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.settings_section_display))
            }
            item {
                BlurredBackgroundRow(
                    enabled = blurredBackground,
                    onToggle = { scope.launch { settingsRepository.setBlurredBackground(it) } },
                )
                HorizontalDivider(color = Color(0xFF1F232C))
            }
            item {
                CropLandscapeRow(
                    enabled = cropLandscape,
                    onToggle = { scope.launch { settingsRepository.setCropLandscape(it) } },
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.settings_section_weather))
            }
            item {
                WeatherLocationRow(
                    cityName = weatherCity,
                    onClick = { showCityPicker = true },
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.settings_section_storage))
            }
            item {
                SettingsRow(
                    item = items[2],
                    onClick = { showClearConfirm = true },
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.settings_section_sleep))
            }
            item {
                SleepEnabledRow(
                    enabled = sleepEnabled,
                    onToggle = { scope.launch { settingsRepository.setSleepEnabled(it) } },
                )
                HorizontalDivider(color = Color(0xFF1F232C))
            }
            item {
                SleepTimeRow(
                    title = stringResource(R.string.settings_sleep_at),
                    summary = sleepOffTime,
                    icon = Icons.Filled.Bedtime,
                    enabled = sleepEnabled,
                    onClick = { editingTime = TimeEdit.Off(sleepOffTime) },
                )
                HorizontalDivider(color = Color(0xFF1F232C))
            }
            item {
                SleepTimeRow(
                    title = stringResource(R.string.settings_wake_at),
                    summary = sleepOnTime,
                    icon = Icons.Filled.WbSunny,
                    enabled = sleepEnabled,
                    onClick = { editingTime = TimeEdit.On(sleepOnTime) },
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
                SectionHeader(stringResource(R.string.settings_section_device))
            }
            item {
                SettingsRow(
                    item = SettingItem.OpenSystemSettings(
                        title = stringResource(R.string.settings_open_android_title),
                        summary = stringResource(R.string.settings_open_android_summary),
                    ),
                    trailingIcon = Icons.Filled.OpenInNew,
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    },
                )
            }
        }
    }

    when (val current = editing) {
        is SettingItem.ServerUrl -> EditDialog(
            title = stringResource(R.string.settings_server_url),
            initialValue = serverUrl,
            placeholder = stringResource(R.string.settings_server_url_placeholder),
            keyboardType = KeyboardType.Uri,
            masked = false,
            onDismiss = { editing = null },
            onConfirm = { newValue ->
                scope.launch {
                    settingsRepository.update(newValue.trim(), apiKey)
                    if (newValue.trim() != serverUrl) {
                        settingsRepository.clearSelectedAlbum()
                        immichRepository.clearAll()
                    }
                    editing = null
                }
            },
        )
        is SettingItem.ApiKey -> EditDialog(
            title = stringResource(R.string.settings_api_key),
            initialValue = apiKey,
            placeholder = "",
            keyboardType = KeyboardType.Password,
            masked = true,
            onDismiss = { editing = null },
            onConfirm = { newValue ->
                scope.launch {
                    settingsRepository.update(serverUrl, newValue.trim())
                    if (newValue.trim() != apiKey) {
                        settingsRepository.clearSelectedAlbum()
                        immichRepository.clearAll()
                    }
                    editing = null
                }
            },
        )
        is SettingItem.ClearCache, is SettingItem.OpenSystemSettings, null -> Unit
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.settings_clear_cache_dialog_title)) },
            text = { Text(stringResource(R.string.settings_clear_cache_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        immichRepository.clearAll()
                        showClearConfirm = false
                    }
                }) { Text(stringResource(R.string.common_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showCityPicker) {
        CityPickerDialog(
            currentDisplayName = weatherCity,
            onDismiss = { showCityPicker = false },
            onSelected = { city ->
                scope.launch {
                    settingsRepository.setWeatherLocation(
                        cityName = city.displayName,
                        latitude = city.latitude,
                        longitude = city.longitude,
                    )
                    showCityPicker = false
                }
            },
        )
    }

    editingTime?.let { edit ->
        TimePickerDialog(
            title = if (edit is TimeEdit.Off)
                stringResource(R.string.settings_sleep_at)
            else stringResource(R.string.settings_wake_at),
            initial = edit.value,
            onDismiss = { editingTime = null },
            onConfirm = { newValue ->
                scope.launch {
                    when (edit) {
                        is TimeEdit.Off -> settingsRepository.setSleepOffTime(newValue)
                        is TimeEdit.On -> settingsRepository.setSleepOnTime(newValue)
                    }
                    editingTime = null
                }
            },
        )
    }
}

private sealed interface TimeEdit {
    val value: String
    data class Off(override val value: String) : TimeEdit
    data class On(override val value: String) : TimeEdit
}

@Composable
private fun WeatherLocationRow(cityName: String, onClick: () -> Unit) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.LocationCity,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(stringResource(R.string.settings_weather_location)) },
        supportingContent = {
            Text(
                text = cityName.ifBlank { stringResource(R.string.common_not_set) },
                color = Color(0xFFB5BAC5),
                maxLines = 2,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityPickerDialog(
    currentDisplayName: String,
    onDismiss: () -> Unit,
    onSelected: (CityResult) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CityResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    val weatherApi: WeatherApi = org.koin.compose.koinInject()
    LaunchedEffect(query, weatherApi) {
        if (query.length < 2) {
            results = emptyList()
            loading = false
            return@LaunchedEffect
        }
        loading = true
        delay(300) // debounce
        results = weatherApi.searchCities(query)
        loading = false
    }

    val notSet = stringResource(R.string.common_not_set)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_weather_location)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_weather_currently, currentDisplayName.ifBlank { notSet }),
                    color = Color(0xFFB5BAC5),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.settings_weather_search_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.heightIn(min = 80.dp, max = 320.dp)) {
                    when {
                        loading -> Text(
                            text = stringResource(R.string.settings_weather_searching),
                            color = Color(0xFFB5BAC5),
                            modifier = Modifier.padding(8.dp),
                        )
                        results.isEmpty() && query.length >= 2 -> Text(
                            text = stringResource(R.string.settings_weather_no_matches),
                            color = Color(0xFFB5BAC5),
                            modifier = Modifier.padding(8.dp),
                        )
                        else -> LazyColumn {
                            items(results, key = { it.id }) { city ->
                                ListItem(
                                    headlineContent = { Text(city.name) },
                                    supportingContent = {
                                        Text(
                                            text = listOf(city.admin1, city.country)
                                                .filter { it.isNotBlank() }
                                                .joinToString(", "),
                                            color = Color(0xFFB5BAC5),
                                        )
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelected(city) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun CropLandscapeRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.AspectRatio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(stringResource(R.string.settings_crop_title)) },
        supportingContent = {
            Text(
                text = if (enabled)
                    stringResource(R.string.settings_crop_summary_on)
                else stringResource(R.string.settings_crop_summary_off),
                color = Color(0xFFB5BAC5),
            )
        },
        trailingContent = {
            Switch(checked = enabled, onCheckedChange = onToggle)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) },
    )
}

@Composable
private fun BlurredBackgroundRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.BlurOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(stringResource(R.string.settings_blur_title)) },
        supportingContent = {
            Text(
                text = if (enabled)
                    stringResource(R.string.settings_blur_summary_on)
                else stringResource(R.string.settings_blur_summary_off),
                color = Color(0xFFB5BAC5),
            )
        },
        trailingContent = {
            Switch(checked = enabled, onCheckedChange = onToggle)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) },
    )
}

@Composable
private fun SleepEnabledRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = Icons.Filled.Bedtime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(stringResource(R.string.settings_sleep_enable_title)) },
        supportingContent = {
            Text(
                text = if (enabled)
                    stringResource(R.string.settings_sleep_enable_summary_on)
                else stringResource(R.string.settings_sleep_enable_summary_off),
                color = Color(0xFFB5BAC5),
            )
        },
        trailingContent = {
            Switch(checked = enabled, onCheckedChange = onToggle)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) },
    )
}

@Composable
private fun SleepTimeRow(
    title: String,
    summary: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (enabled) MaterialTheme.colorScheme.primary else Color(0xFF7A7F89)
    val textColor = if (enabled) Color.Unspecified else Color(0xFF7A7F89)
    ListItem(
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        },
        headlineContent = { Text(title, color = textColor) },
        supportingContent = {
            Text(text = summary, color = if (enabled) Color(0xFFB5BAC5) else Color(0xFF5A5F69))
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initialMinutes = SleepSchedule.parseMinutes(initial) ?: 0
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(SleepSchedule.formatMinutes(state.hour * 60 + state.minute))
            }) { Text(stringResource(R.string.common_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SettingsRow(
    item: SettingItem,
    onClick: () -> Unit,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(item.title) },
        supportingContent = {
            Text(
                text = item.summary,
                color = Color(0xFFB5BAC5),
                maxLines = 2,
            )
        },
        trailingContent = trailingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = Color(0xFFB5BAC5),
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun EditDialog(
    title: String,
    initialValue: String,
    placeholder: String,
    keyboardType: KeyboardType,
    masked: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    placeholder = { Text(placeholder) },
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = if (masked) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onConfirm(value) },
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}

private fun maskApiKey(value: String, notSetText: String): String = when {
    value.isBlank() -> notSetText
    value.length <= 4 -> "•".repeat(value.length)
    else -> "•".repeat(value.length - 4) + value.takeLast(4)
}
