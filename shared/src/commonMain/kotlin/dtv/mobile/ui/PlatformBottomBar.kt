package dtv.mobile.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import dtv.mobile.model.Platform
import dtv.mobile.state.Screen

@Composable
fun PlatformBottomBar(
  selectedScreen: Screen,
  selectedPlatform: Platform,
  onHomeClick: () -> Unit,
  onPlatformClick: (Platform) -> Unit,
) {
  val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  val containerColor = if (isDark) Color.Black.copy(alpha = 0.60f) else Color.White.copy(alpha = 0.86f)
  val containerBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color(0xFFE5E7EB)
  val activeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
  val inactiveIcon = if (isDark) Color(0xFF6B7280) else Color(0xFF9CA3AF)
  val inactiveLabel = if (isDark) Color(0xFF6B7280) else Color(0xFF9CA3AF)
  val activeLabel = if (isDark) Color.White else Color.Black

  Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
    Surface(
      modifier = Modifier
        .widthIn(max = 520.dp)
        .align(androidx.compose.ui.Alignment.Center)
        .clip(RoundedCornerShape(26.dp)),
      shape = RoundedCornerShape(26.dp),
      color = containerColor,
      tonalElevation = 0.dp,
      shadowElevation = if (isDark) 0.dp else 18.dp,
      border = BorderStroke(1.dp, containerBorder),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
      ) {
        DockItem(
          title = "主页",
          selected = selectedScreen == Screen.Home,
          activeBackground = activeBg,
          activeLabelColor = activeLabel,
          inactiveLabelColor = inactiveLabel,
          onClick = onHomeClick,
        ) {
          Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "主页",
            tint = if (selectedScreen == Screen.Home) MaterialTheme.colorScheme.primary else inactiveIcon,
          )
        }

        Platform.entries.filter { it != Platform.Custom }.forEach { platform ->
          val isSelected = selectedScreen == Screen.Platform && platform == selectedPlatform
          DockItem(
            title = platform.title,
            selected = isSelected,
            activeBackground = activeBg,
            activeLabelColor = activeLabel,
            inactiveLabelColor = inactiveLabel,
            onClick = { onPlatformClick(platform) },
          ) {
            val icon = when (platform) {
              Platform.Douyu -> Icons.Default.SportsEsports
              Platform.Huya -> Icons.Default.SmartDisplay
              Platform.Douyin -> Icons.Default.Public
              Platform.Bilibili -> Icons.Default.VideoLibrary
              Platform.Custom -> Icons.Default.Home
            }
            Icon(
              imageVector = icon,
              contentDescription = platform.title,
              tint = if (isSelected) MaterialTheme.colorScheme.primary else inactiveIcon,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun RowScope.DockItem(
  title: String,
  selected: Boolean,
  activeBackground: Color,
  activeLabelColor: Color,
  inactiveLabelColor: Color,
  onClick: () -> Unit,
  icon: @Composable () -> Unit,
) {
  Column(
    modifier = Modifier
      .weight(1f)
      .height(64.dp)
      .clip(RoundedCornerShape(20.dp))
      .clickable { onClick() }
      .padding(vertical = 10.dp),
    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      if (selected) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          shape = RoundedCornerShape(20.dp),
          color = activeBackground,
          tonalElevation = 0.dp,
          shadowElevation = 0.dp,
        ) {}
      }

      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        icon()
        Text(
          text = title,
          style = MaterialTheme.typography.labelSmall,
          color = if (selected) activeLabelColor else inactiveLabelColor,
        )
        Spacer(modifier = Modifier.height(2.dp))
      }
    }
  }
}
