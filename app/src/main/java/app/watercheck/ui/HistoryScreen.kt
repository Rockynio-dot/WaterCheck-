package app.watercheck.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle as DayStyle
import java.util.Locale

private val CS = Locale.forLanguageTag("cs")

@Composable
fun HistoryScreen(state: UiState) {
    var days by rememberSaveable { mutableIntStateOf(7) }
    val goal = state.settings.goalMl
    val range = (days - 1 downTo 0).map { state.today.minusDays(it.toLong()) }
    val values = range.map { state.totals[it] ?: 0 }
    val met = values.count { it >= goal }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(7, 30).forEachIndexed { i, n ->
                SegmentedButton(
                    selected = days == n,
                    onClick = { days = n },
                    shape = SegmentedButtonDefaults.itemShape(i, 2),
                ) { Text("$n dní") }
            }
        }

        Card { BarChart(range, values, goal, Modifier.fillMaxWidth().height(220.dp).padding(12.dp)) }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Průměr", "${values.average().toInt()} ml", Modifier.weight(1f))
            Stat("Série", "🔥 ${state.streak} dní", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Cíl splněn", "$met z $days dní", Modifier.weight(1f))
            Stat("Celkem", "%.1f l".format(CS, values.sum() / 1000.0), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BarChart(days: List<LocalDate>, values: List<Int>, goal: Int, modifier: Modifier) {
    val measurer = rememberTextMeasurer()
    val barDone = MaterialTheme.colorScheme.primary
    val barOpen = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    val labelStyle = TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)

    Canvas(modifier) {
        val labelHeight = 18.dp.toPx()
        val chartHeight = size.height - labelHeight
        val max = maxOf(values.max(), goal) * 1.1f
        val slot = size.width / days.size
        val barWidth = slot * 0.6f

        values.forEachIndexed { i, v ->
            val h = chartHeight * v / max
            drawRoundRect(
                color = if (v >= goal) barDone else barOpen,
                topLeft = Offset(i * slot + (slot - barWidth) / 2, chartHeight - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 4),
            )
            // Weekday names for a week; every fifth day of month for longer ranges.
            val text = when {
                days.size <= 7 -> days[i].dayOfWeek.getDisplayName(DayStyle.SHORT, CS)
                (days.size - 1 - i) % 5 == 0 -> "${days[i].dayOfMonth}."
                else -> null
            }
            if (text != null) {
                val layout = measurer.measure(text, labelStyle)
                drawText(
                    layout,
                    topLeft = Offset(
                        (i * slot + (slot - layout.size.width) / 2)
                            .coerceIn(0f, size.width - layout.size.width),
                        chartHeight + 4.dp.toPx(),
                    ),
                )
            }
        }

        val goalY = chartHeight - chartHeight * goal / max
        drawLine(
            line, Offset(0f, goalY), Offset(size.width, goalY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
        )
    }
}
