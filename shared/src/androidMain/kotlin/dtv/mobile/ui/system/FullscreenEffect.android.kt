package dtv.mobile.ui.system

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
actual fun FullscreenEffect(enabled: Boolean) {
  val view = LocalView.current
  val activity = view.context.findActivity() ?: return
  val window = activity.window

  DisposableEffect(enabled) {
    val controller = WindowCompat.getInsetsController(window, view)
    val prevBehavior = controller.systemBarsBehavior
    val prevOrientation = activity.requestedOrientation
    controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    if (enabled) {
      controller.hide(WindowInsetsCompat.Type.systemBars())
      activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    } else {
      controller.show(WindowInsetsCompat.Type.systemBars())
      activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    onDispose {
      controller.systemBarsBehavior = prevBehavior
      controller.show(WindowInsetsCompat.Type.systemBars())
      activity.requestedOrientation = prevOrientation
    }
  }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}
