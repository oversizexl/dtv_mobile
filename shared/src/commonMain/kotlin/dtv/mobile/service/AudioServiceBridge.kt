package dtv.mobile.service

/**
 * 音频播放服务桥接接口。
 *
 * 用于在 commonMain 中控制 Android 前台通知服务。
 */
expect object AudioServiceBridge {
  /**
   * 启动音频播放前台服务。
   * @param streamerName 主播名称
   * @param title 直播标题
   * @param avatarUrl 主播头像 URL
   */
  fun startService(streamerName: String, title: String, avatarUrl: String?)

  /**
   * 更新播放状态（播放/暂停）。
   */
  fun updatePlaybackState(isPlaying: Boolean)

  /**
   * 停止前台服务。
   */
  fun stopService()

  /**
   * 设置播放/暂停回调。
   * 当用户点击通知栏的播放/暂停按钮时触发。
   */
  fun setOnPlayPauseCallback(callback: (() -> Unit)?)

  /**
   * 设置停止回调。
   * 当用户点击通知栏的关闭按钮时触发。
   */
  fun setOnStopCallback(callback: (() -> Unit)?)
}
