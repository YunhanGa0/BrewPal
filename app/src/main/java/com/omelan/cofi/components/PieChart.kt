import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

@Composable
fun PieChart(data: List<Pair<String, Int>>, colors: List<Color>, modifier: Modifier = Modifier) {
    val total = data.sumOf { it.second }
    var startAngle = 0f

    Canvas(modifier = modifier) {

        data.forEachIndexed { index, (label, value) ->
            val sweepAngle = (value.toFloat() / total) * 360
            drawArc(
                color = colors[index],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true
            )
            startAngle += sweepAngle
        }
    }
}
