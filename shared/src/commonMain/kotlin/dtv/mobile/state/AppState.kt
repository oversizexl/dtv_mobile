package dtv.mobile.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dtv.mobile.model.Platform
import dtv.mobile.model.Streamer
import dtv.mobile.repo.DtvRepository
import dtv.mobile.repo.fake.FakeDtvRepository

class AppState(
  val repo: DtvRepository,
) {
  var themeMode: ThemeMode by mutableStateOf(ThemeMode.System)
  var selectedPlatform: Platform by mutableStateOf(Platform.Douyu)
  var currentScreen: Screen by mutableStateOf(Screen.Home)
  var currentStreamer: Streamer? by mutableStateOf(null)

  fun toggleTheme() {
    themeMode = when (themeMode) {
      ThemeMode.System -> ThemeMode.Dark
      ThemeMode.Dark -> ThemeMode.Light
      ThemeMode.Light -> ThemeMode.System
    }
  }

  fun selectPlatform(platform: Platform) {
    selectedPlatform = platform
    currentScreen = Screen.Home
  }

  fun openPlayer(streamer: Streamer) {
    currentStreamer = streamer
    currentScreen = Screen.Player
  }

  fun back() {
    when (currentScreen) {
      Screen.Home -> Unit
      Screen.Player -> {
        currentScreen = Screen.Home
        currentStreamer = null
      }
    }
  }
}

enum class ThemeMode { System, Light, Dark }

enum class Screen { Home, Player }

@Composable
fun rememberAppState(repo: DtvRepository = FakeDtvRepository()): AppState {
  return remember(repo) { AppState(repo = repo) }
}
