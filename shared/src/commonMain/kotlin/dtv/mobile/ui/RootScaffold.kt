package dtv.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dtv.mobile.state.AppState
import dtv.mobile.state.Screen
import dtv.mobile.state.ThemeMode
import dtv.mobile.ui.screens.HomeScreen
import dtv.mobile.ui.screens.PlatformScreen
import dtv.mobile.ui.screens.PlayerScreen
import dtv.mobile.ui.screens.SearchScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScaffold(appState: AppState) {
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      when (appState.currentScreen) {
        Screen.Player -> Unit
        Screen.Search -> {
          CenterAlignedTopAppBar(
            title = { Text(text = "搜索") },
            navigationIcon = {
              IconButton(onClick = { appState.back() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
              }
            },
            actions = {
              IconButton(onClick = { appState.toggleTheme() }) {
                val icon = when (appState.themeMode) {
                  ThemeMode.System -> Icons.Default.MoreVert
                  ThemeMode.Light -> Icons.Default.LightMode
                  ThemeMode.Dark -> Icons.Default.DarkMode
                }
                Icon(icon, contentDescription = "主题")
              }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            ),
          )
        }
        else -> {
          HubTopBar(
            title = if (appState.currentScreen == Screen.Home) "关注列表" else appState.selectedPlatform.title,
            onSearchClick = appState::openSearch,
            onThemeToggle = appState::toggleTheme,
            themeMode = appState.themeMode,
            showSearch = appState.currentScreen != Screen.Home,
          )
        }
      }
    },
    bottomBar = {
      if (appState.currentScreen != Screen.Player) {
        PlatformBottomBar(
          selectedScreen = appState.dockSelectedScreen,
          selectedPlatform = appState.selectedPlatform,
          onHomeClick = appState::openHome,
          onPlatformClick = appState::selectPlatform,
        )
      }
    },
  ) { padding ->
    when (appState.currentScreen) {
      Screen.Home -> HomeScreen(
        modifier = Modifier.padding(padding),
        appState = appState,
      )
      Screen.Platform -> PlatformScreen(
        modifier = Modifier.padding(padding),
        appState = appState,
      )
      Screen.Player -> PlayerScreen(
        modifier = Modifier.padding(padding),
        appState = appState,
        streamer = appState.currentStreamer,
      )
      Screen.Search -> SearchScreen(
        modifier = Modifier.padding(padding),
        appState = appState,
      )
    }
  }
}

@Composable
private fun HubTopBar(
  title: String,
  themeMode: ThemeMode,
  onSearchClick: () -> Unit,
  onThemeToggle: () -> Unit,
  showSearch: Boolean,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Black,
          fontStyle = FontStyle.Italic,
        ),
        modifier = Modifier.padding(top = 4.dp),
      )

      if (showSearch) {
        Surface(
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable { onSearchClick() },
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
          tonalElevation = 0.dp,
          shadowElevation = 0.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        ) {
          Row(modifier = Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "搜索",
              tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
              modifier = Modifier.padding(top = 12.dp),
            )
            Text(
              text = "搜索直播、主播",
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(top = 12.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
          }
        }
      } else {
        Spacer(modifier = Modifier.weight(1f))
      }

      IconButton(
        onClick = onThemeToggle,
        modifier = Modifier.clip(CircleShape),
      ) {
        val icon = when (themeMode) {
          ThemeMode.System -> Icons.Default.MoreVert
          ThemeMode.Light -> Icons.Default.LightMode
          ThemeMode.Dark -> Icons.Default.DarkMode
        }
        Icon(icon, contentDescription = "主题")
      }
    }
  }
}
