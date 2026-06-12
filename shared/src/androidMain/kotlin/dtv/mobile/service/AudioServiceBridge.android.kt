package dtv.mobile.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import dtv.mobile.util.AppLog

/**
 * Android 平台的音频服务桥接实现。
 *
 * 使用静态 Context 引用（通过 [init] 设置）来启动/停止服务。
 * 通过 ServiceConnection 绑定服务以获取回调支持。
 */
actual object AudioServiceBridge {

  private const val TAG = "DTV-AudioBridge"

  private var appContext: Context? = null
  private var service: MediaPlaybackService? = null
  private var bound = false

  private var onPlayPauseCallback: (() -> Unit)? = null
  private var onStopCallback: (() -> Unit)? = null

  private var currentStreamerName: String = ""
  private var currentTitle: String = ""
  private var currentAvatarUrl: String? = null

  private val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
      val localBinder = binder as? MediaPlaybackService.LocalBinder ?: return
      service = localBinder.service
      bound = true

      // 设置回调
      service?.onPlayPause = {
        onPlayPauseCallback?.invoke()
      }
      service?.onStop = {
        onStopCallback?.invoke()
      }

      AppLog.i(TAG, "service bound")
    }

    override fun onServiceDisconnected(name: ComponentName?) {
      service = null
      bound = false
      AppLog.i(TAG, "service unbound")
    }
  }

  /**
   * 初始化桥接，需要在 Application 或 Activity 中调用。
   */
  fun init(context: Context) {
    appContext = context.applicationContext
  }

  actual fun startService(streamerName: String, title: String, avatarUrl: String?) {
    val ctx = appContext ?: run {
      AppLog.e(TAG, "startService failed: context not initialized")
      return
    }
    currentStreamerName = streamerName
    currentTitle = title
    currentAvatarUrl = avatarUrl

    MediaPlaybackService.start(ctx, streamerName, title, avatarUrl)

    // 绑定服务以获取回调
    val intent = Intent(ctx, MediaPlaybackService::class.java)
    ctx.bindService(intent, connection, Context.BIND_AUTO_CREATE)

    AppLog.i(TAG, "startService: $streamerName - $title")
  }

  actual fun updatePlaybackState(isPlaying: Boolean) {
    if (bound) {
      service?.updatePlaybackInfo(currentStreamerName, currentTitle, currentAvatarUrl, isPlaying)
    }
  }

  actual fun stopService() {
    val ctx = appContext ?: return
    if (bound) {
      ctx.unbindService(connection)
      bound = false
    }
    MediaPlaybackService.stop(ctx)
    service = null
    AppLog.i(TAG, "stopService")
  }

  actual fun setOnPlayPauseCallback(callback: (() -> Unit)?) {
    onPlayPauseCallback = callback
    service?.onPlayPause = callback
  }

  actual fun setOnStopCallback(callback: (() -> Unit)?) {
    onStopCallback = callback
    service?.onStop = callback
  }
}
