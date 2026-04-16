package dtv.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import dtv.mobile.model.Platform

@Composable
fun PlatformBottomBar(
  selected: Platform,
  onSelected: (Platform) -> Unit,
  accentGradient: Brush,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
      .padding(horizontal = 10.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Platform.entries.forEach { platform ->
      val isSelected = platform == selected
      val indicatorBrush = if (isSelected) {
        accentGradient
      } else {
        val c = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        Brush.linearGradient(listOf(c, c))
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .clip(CircleShape)
          .clickable { onSelected(platform) }
          .padding(vertical = 6.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        val icon = when (platform) {
          Platform.Custom -> Icons.Default.Bookmarks
          Platform.Douyu -> Icons.Default.SportsEsports
          Platform.Huya -> Icons.Default.SmartDisplay
          Platform.Douyin -> Icons.Default.Public
          Platform.Bilibili -> Icons.Default.VideoLibrary
        }
        Icon(
          imageVector = icon,
          contentDescription = platform.title,
          tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Text(
          text = platform.title,
          style = MaterialTheme.typography.labelSmall,
          color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Spacer(
          modifier = Modifier
            .size(width = 34.dp, height = 4.dp)
            .clip(CircleShape)
            .background(indicatorBrush),
        )
      }
    }
  }
}
