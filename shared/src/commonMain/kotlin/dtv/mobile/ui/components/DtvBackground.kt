package dtv.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance

@Composable
fun DtvBackground(content: @Composable () -> Unit) {
  val bg = MaterialTheme.colorScheme.background
  val accent = MaterialTheme.colorScheme.primary
  val isDark = bg.luminance() < 0.35f

  Box(modifier = Modifier.fillMaxSize()) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawRect(color = bg)
      drawMesh(
        topLeft = Offset(0f, 0f),
        bottomRight = Offset(size.width, size.height),
        accent = accent,
        isDark = isDark,
      )
    }
    content()
  }
}

private fun DrawScope.drawMesh(
  topLeft: Offset,
  bottomRight: Offset,
  accent: Color,
  isDark: Boolean,
) {
  val w = bottomRight.x - topLeft.x
  val h = bottomRight.y - topLeft.y

  // Two subtle radial "mesh" blobs inspired by `--page-mesh` in the desktop CSS.
  val a1 = if (isDark) accent.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
  val a2 = if (isDark) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.04f)

  fun blob(center: Offset, radius: Float, color: Color) {
    drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(color, Color.Transparent),
        center = center,
        radius = radius,
      ),
      radius = radius,
      center = center,
    )
  }

  blob(center = Offset(w * 0.12f, h * 0.10f), radius = w * 0.58f, color = a1)
  blob(center = Offset(w * 0.92f, h * 0.92f), radius = w * 0.62f, color = a2)
}
