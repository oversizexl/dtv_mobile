package dtv.mobile.android

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.core.view.WindowCompat
import dtv.mobile.App
import dtv.mobile.pip.PipBridge
import dtv.mobile.repo.android.AndroidDtvRepository
import dtv.mobile.state.SubscriptionStoreAndroid
import dtv.mobile.util.AppLog

class MainActivity : ComponentActivity() {

  companion object {
    private const val TAG = "DTV-MainActivity"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppLog.init(applicationContext)
    WindowCompat.setDecorFitsSystemWindows(window, false)

    PipBridge.enterPipRequest = { enterPipMode() }

    setContent {
      val repo = remember { AndroidDtvRepository(applicationContext) }
      val subscriptionStore = remember { SubscriptionStoreAndroid(applicationContext) }
      App(repo = repo, subscriptionStore = subscriptionStore)
    }
  }

  override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    // 按 Home 键时，如果有活跃播放器，进入画中画
    if (PipBridge.pipIsPlaying) {
      enterPipMode()
    }
  }

  override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode)
    PipBridge.isInPipMode = isInPictureInPictureMode
    AppLog.i(TAG, "PiP mode changed: $isInPictureInPictureMode")
  }

  private fun enterPipMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val params = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
        .build()
      try {
        enterPictureInPictureMode(params)
        AppLog.i(TAG, "entered PiP mode")
      } catch (e: Exception) {
        AppLog.e(TAG, "enter PiP failed", e)
      }
    }
  }

  override fun onDestroy() {
    PipBridge.enterPipRequest = null
    super.onDestroy()
  }
}
