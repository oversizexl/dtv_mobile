package dtv.mobile.pip

/**
 * 画中画模式控制器。
 *
 * 由 PlayerScreen 设置播放状态和回调，由 MainActivity 读取并执行 PiP 操作。
 */
object PipBridge {

  /** 当前是否处于画中画模式 */
  var isInPipMode: Boolean = false

  /** 当前是否有活跃的播放器 */
  var pipIsPlaying: Boolean = false

  /** 进入画中画的回调（由 MainActivity 注册） */
  var enterPipRequest: (() -> Unit)? = null

  /** 画中画中播放/暂停回调（由 PlayerScreen 注册） */
  var onPipPlayPause: (() -> Unit)? = null

  /** 画中画中全屏回调（由 PlayerScreen 注册） */
  var onPipFullscreen: (() -> Unit)? = null

  fun requestEnterPip() {
    enterPipRequest?.invoke()
  }

  fun notifyPlayPause() {
    onPipPlayPause?.invoke()
  }

  fun notifyFullscreen() {
    onPipFullscreen?.invoke()
  }
}
