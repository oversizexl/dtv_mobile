package dtv.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CategoryPill(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val selectedBg = MaterialTheme.colorScheme.primary
  val selectedFg = Color.Black
  val idleBg = if (isDark) Color(0xFF252525) else Color(0xFFE5E7EB)
  val idleFg = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)

  Surface(
    modifier = modifier
      .clip(CircleShape)
      .clickable(onClick = onClick),
    shape = CircleShape,
    color = if (selected) selectedBg else idleBg,
    tonalElevation = 0.dp,
    shadowElevation = if (selected) 10.dp else 0.dp,
  ) {
    Box(
      modifier = Modifier
        .background(Color.Transparent)
        .padding(horizontal = 18.dp, vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
        color = if (selected) selectedFg else idleFg,
        maxLines = 1,
      )
    }
  }
}

