package app.watercheck.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.watercheck.data.Drink
import app.watercheck.data.DrinkType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.sin

private val CUPS = listOf(100, 200, 250, 330, 500)
private val TIME = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun HomeScreen(state: UiState, vm: WaterViewModel) {
    val settings = state.settings
    val drink = DrinkType.of(settings.drinkType)
    var customCup by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 8.dp)) {
                WaterCircle(
                    progress = state.todayMl.toFloat() / settings.goalMl,
                    modifier = Modifier.size(230.dp),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${state.todayMl}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("z ${settings.goalMl} ml", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${state.todayMl * 100 / settings.goalMl} %",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Text(
                when {
                    state.todayMl >= settings.goalMl -> "Cíl splněn 🎉"
                    else -> "Zbývá ${settings.goalMl - state.todayMl} ml"
                } + if (state.streak > 0) "  ·  🔥 ${state.streak} dní v řadě" else "",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp),
            )

            ChipRow(Modifier.padding(top = 16.dp)) {
                DrinkType.entries.forEach { type ->
                    FilterChip(
                        selected = type == drink,
                        onClick = { vm.update { it.copy(drinkType = type.id) } },
                        label = { Text("${type.emoji} ${type.label}") },
                    )
                }
            }
            ChipRow {
                CUPS.forEach { ml ->
                    FilterChip(
                        selected = settings.cupMl == ml,
                        onClick = { vm.update { it.copy(cupMl = ml) } },
                        label = { Text("$ml ml") },
                    )
                }
                FilterChip(
                    selected = settings.cupMl !in CUPS,
                    onClick = { customCup = true },
                    label = { Text(if (settings.cupMl in CUPS) "Vlastní" else "${settings.cupMl} ml") },
                )
            }
            Button(
                onClick = { vm.addDrink(settings.cupMl, drink) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).height(56.dp),
            ) {
                Text("${drink.emoji}  Vypít ${settings.cupMl} ml", style = MaterialTheme.typography.titleMedium)
            }
        }

        if (state.todayDrinks.isNotEmpty()) {
            item {
                Text(
                    "Dnešní záznamy",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
        items(state.todayDrinks, key = { it.id }) { DrinkRow(it) { vm.deleteDrink(it.id) } }
    }

    if (customCup) {
        NumberDialog(
            title = "Vlastní objem (ml)",
            initial = settings.cupMl,
            range = 10..2000,
            onDismiss = { customCup = false },
            onConfirm = { ml ->
                vm.update { it.copy(cupMl = ml) }
                customCup = false
            },
        )
    }
}

@Composable
private fun ChipRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) { content() }
}

@Composable
private fun DrinkRow(drink: Drink, onDelete: () -> Unit) {
    val type = DrinkType.of(drink.type)
    val time = Instant.ofEpochMilli(drink.timestamp).atZone(ZoneId.systemDefault()).format(TIME)
    ListItem(
        leadingContent = { Text(type.emoji, style = MaterialTheme.typography.headlineSmall) },
        headlineContent = { Text("${drink.amountMl} ml · ${type.label}") },
        supportingContent = {
            Text(if (drink.effectiveMl != drink.amountMl) "$time · započteno ${drink.effectiveMl} ml" else time)
        },
        trailingContent = {
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Smazat") }
        },
    )
}

/** Circle that fills with an animated wave up to [progress]. */
@Composable
fun WaterCircle(progress: Float, modifier: Modifier = Modifier) {
    val level by animateFloatAsState(progress.coerceIn(0f, 1f), tween(800), label = "level")
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "phase",
    )
    val track = MaterialTheme.colorScheme.surfaceVariant
    val water = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val border = MaterialTheme.colorScheme.primary

    Canvas(modifier) {
        val radius = size.minDimension / 2
        val circle = Path().apply { addOval(Rect(center, radius)) }
        drawCircle(track, radius)
        clipPath(circle) {
            val top = size.height * (1 - level)
            val amplitude = if (level > 0f && level < 1f) size.height * 0.02f else 0f
            val wave = Path().apply {
                moveTo(0f, top)
                var x = 0f
                while (x <= size.width) {
                    lineTo(x, top + amplitude * sin(x / size.width * 3 * PI.toFloat() + phase))
                    x += 6f
                }
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(wave, water)
        }
        drawCircle(border, radius - 2.dp.toPx(), style = Stroke(4.dp.toPx()))
    }
}

@Composable
fun NumberDialog(
    title: String,
    initial: Int,
    range: IntRange,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(initial.toString()) }
    val value = text.toIntOrNull()?.takeIf { it in range }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter(Char::isDigit).take(4) },
                isError = value == null,
                supportingText = { Text("${range.first}–${range.last}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let(onConfirm) }, enabled = value != null) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Zrušit") } },
    )
}
