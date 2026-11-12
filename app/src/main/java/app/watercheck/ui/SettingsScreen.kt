package app.watercheck.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings as SystemSettings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.watercheck.Hydration
import app.watercheck.data.Settings
import app.watercheck.reminders.Reminders

private val INTERVALS = listOf(30, 45, 60, 90, 120)

private enum class Editing { WEIGHT, ACTIVITY, GOAL, WAKE, SLEEP }

@Composable
fun SettingsScreen(settings: Settings, vm: WaterViewModel) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<Editing?>(null) }
    val autoGoal = Hydration.dailyGoalMl(settings.weightKg, settings.activityMin, settings.hotWeather)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("Denní cíl")
        Item("Váha", "${settings.weightKg} kg") { editing = Editing.WEIGHT }
        Item("Pohyb za den", "${settings.activityMin} min") { editing = Editing.ACTIVITY }
        SwitchItem("Horký den", "+500 ml", settings.hotWeather) { on ->
            vm.update { it.copy(hotWeather = on) }
        }
        SwitchItem("Vlastní cíl", "Vypočtený cíl: $autoGoal ml", settings.customGoalMl > 0) { on ->
            vm.update { it.copy(customGoalMl = if (on) autoGoal else 0) }
        }
        if (settings.customGoalMl > 0) {
            Item("Cíl", "${settings.customGoalMl} ml") { editing = Editing.GOAL }
        }

        HorizontalDivider()
        Header("Připomínky")
        SwitchItem("Připomínat pití", null, settings.reminders) { on ->
            vm.update { it.copy(reminders = on) }
        }
        if (settings.reminders) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Každých")
                INTERVALS.forEach { min ->
                    FilterChip(
                        selected = settings.intervalMin == min,
                        onClick = { vm.update { it.copy(intervalMin = min) } },
                        label = { Text("$min min") },
                    )
                }
            }
            Item("Vstávám", formatTime(settings.wakeMin)) { editing = Editing.WAKE }
            Item("Jdu spát", formatTime(settings.sleepMin)) { editing = Editing.SLEEP }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Aby připomínky chodily spolehlivě (hlavně na Xiaomi), vypněte pro aplikaci " +
                        "optimalizaci baterie a v nastavení aplikace povolte Automatické spouštění.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedButton(onClick = { openBatterySettings(context) }, Modifier.fillMaxWidth()) {
                    Text("Vypnout optimalizaci baterie")
                }
                OutlinedButton(onClick = { openAppSettings(context) }, Modifier.fillMaxWidth()) {
                    Text("Nastavení aplikace v systému")
                }
                OutlinedButton(
                    onClick = { Reminders.notify(context, 0, settings) },
                    Modifier.fillMaxWidth(),
                ) { Text("Poslat zkušební připomínku") }
            }
        }
    }

    when (editing) {
        Editing.WEIGHT -> NumberDialog("Váha (kg)", settings.weightKg, 20..250, { editing = null }) { v ->
            vm.update { it.copy(weightKg = v) }; editing = null
        }
        Editing.ACTIVITY -> NumberDialog("Pohyb za den (min)", settings.activityMin, 0..600, { editing = null }) { v ->
            vm.update { it.copy(activityMin = v) }; editing = null
        }
        Editing.GOAL -> NumberDialog("Denní cíl (ml)", settings.customGoalMl, 500..9999, { editing = null }) { v ->
            vm.update { it.copy(customGoalMl = v) }; editing = null
        }
        Editing.WAKE -> TimeDialog(settings.wakeMin, { editing = null }) { v ->
            vm.update { it.copy(wakeMin = v) }; editing = null
        }
        Editing.SLEEP -> TimeDialog(settings.sleepMin, { editing = null }) { v ->
            vm.update { it.copy(sleepMin = v) }; editing = null
        }
        null -> {}
    }
}

private fun formatTime(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun Item(title: String, value: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = { Text(value, style = MaterialTheme.typography.bodyLarge) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun SwitchItem(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked, onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(initialMin: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val picker = rememberTimePickerState(initialMin / 60, initialMin % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(picker) },
        confirmButton = {
            TextButton(onClick = { onConfirm(picker.hour * 60 + picker.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}

private fun openBatterySettings(context: Context) {
    val power = context.getSystemService(PowerManager::class.java)
    val intent = if (power.isIgnoringBatteryOptimizations(context.packageName)) {
        Intent(SystemSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else {
        @Suppress("BatteryLife") // personal sideloaded app; reminders need it on HyperOS
        Intent(
            SystemSettings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}"),
        )
    }
    runCatching { context.startActivity(intent) }.onFailure { openAppSettings(context) }
}

private fun openAppSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(
                SystemSettings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${context.packageName}"),
            )
        )
    }
}
