package dtv.mobile.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dtv.mobile.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 音频播放前台服务。
 *
 * 严格按照 NeriPlayer 的 AudioPlayerService 实现方式：
 * - 使用 MediaSessionCompat 管理媒体会话
 * - NotificationCompat + MediaStyle 通知
 * - 前台服务 FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
 * - 通知在锁屏界面可见
 * - 通知显示主播头像
 * - 支持播放/暂停/停止 运输控制
 */
class MediaPlaybackService : Service() {

  companion object {
    const val TAG = "DTV-MediaService"
    const val CHANNEL_ID = "dtv_audio_playback"
    const val NOTIFICATION_ID = 1

    const val ACTION_PLAY_PAUSE = "dtv.mobile.PLAY_PAUSE"
    const val ACTION_STOP = "dtv.mobile.STOP"

    private const val EXTRA_STREAMER_NAME = "streamer_name"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_AVATAR_URL = "avatar_url"

    fun start(context: Context, streamerName: String, title: String, avatarUrl: String?) {
      val intent = Intent(context, MediaPlaybackService::class.java).apply {
        putExtra(EXTRA_STREAMER_NAME, streamerName)
        putExtra(EXTRA_TITLE, title)
        putExtra(EXTRA_AVATAR_URL, avatarUrl)
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stop(context: Context) {
      context.stopService(Intent(context, MediaPlaybackService::class.java))
    }
  }

  private var mediaSession: MediaSessionCompat? = null
  private val binder = LocalBinder()
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

  private var streamerName: String = ""
  private var title: String = ""
  private var avatarUrl: String? = null
  private var isPlaying: Boolean = true
  private var avatarBitmap: Bitmap? = null

  var onPlayPause: (() -> Unit)? = null
  var onStop: (() -> Unit)? = null

  inner class LocalBinder : Binder() {
    val service: MediaPlaybackService get() = this@MediaPlaybackService
  }

  override fun onBind(intent: Intent?): IBinder = binder

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    initMediaSession()
    AppLog.i(TAG, "service created")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_PLAY_PAUSE -> {
        isPlaying = !isPlaying
        onPlayPause?.invoke()
        updatePlaybackState()
        syncNotification()
      }
      ACTION_STOP -> {
        onStop?.invoke()
        stopSelf()
      }
      else -> {
        streamerName = intent?.getStringExtra(EXTRA_STREAMER_NAME).orEmpty()
        title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        avatarUrl = intent?.getStringExtra(EXTRA_AVATAR_URL)
        isPlaying = true

        updatePlaybackState()
        // 先显示不带头像的通知（bootstrap）
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
          startForeground(NOTIFICATION_ID, notification)
        }
        // 异步加载头像后更新通知
        loadAvatarAndUpdate()
      }
    }
    return START_STICKY
  }

  override fun onDestroy() {
    serviceScope.cancel()
    mediaSession?.isActive = false
    mediaSession?.release()
    mediaSession = null
    avatarBitmap?.recycle()
    avatarBitmap = null
    AppLog.i(TAG, "service destroyed")
    super.onDestroy()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        "音频播放",
        NotificationManager.IMPORTANCE_LOW,
      ).apply {
        description = "DTTV 音频播放控制"
        setShowBadge(false)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager.createNotificationChannel(channel)
    }
  }

  private fun initMediaSession() {
    mediaSession = MediaSessionCompat(this, "DTV_AudioSession").apply {
      setCallback(object : MediaSessionCompat.Callback() {
        override fun onPlay() {
          isPlaying = true
          onPlayPause?.invoke()
          updatePlaybackState()
          syncNotification()
        }

        override fun onPause() {
          isPlaying = false
          onPlayPause?.invoke()
          updatePlaybackState()
          syncNotification()
        }

        override fun onStop() {
          onStop?.invoke()
          stopSelf()
        }
      })
      isActive = true
    }
  }

  private fun updatePlaybackState() {
    val state = if (isPlaying) {
      PlaybackStateCompat.STATE_PLAYING
    } else {
      PlaybackStateCompat.STATE_PAUSED
    }
    // 使用 PlaybackStateCompat.Builder 设置状态
    // position 设为 PLAYBACK_POSITION_UNKNOWN(-1) 表示直播流，不显示进度条
    // 不包含 ACTION_SEEK_TO 防止系统显示 seek bar
    mediaSession?.setPlaybackState(
      PlaybackStateCompat.Builder()
        .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
        .setActions(
          PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_STOP
        )
        .build()
    )
  }

  fun updatePlaybackInfo(streamerName: String, title: String, avatarUrl: String?, isPlaying: Boolean) {
    this.streamerName = streamerName
    this.title = title
    if (avatarUrl != this.avatarUrl) {
      this.avatarUrl = avatarUrl
      loadAvatarAndUpdate()
    }
    this.isPlaying = isPlaying
    updatePlaybackState()
    syncNotification()
  }

  /**
   * 异步加载主播头像，加载完成后更新通知。
   */
  private fun loadAvatarAndUpdate() {
    val url = avatarUrl
    if (url.isNullOrBlank()) {
      avatarBitmap = null
      syncNotification()
      return
    }
    serviceScope.launch {
      try {
        val request = Request.Builder()
          .url(url)
          .header("User-Agent", "Mozilla/5.0")
          .build()
        val response = httpClient.newCall(request).execute()
        val bytes = response.body?.bytes()
        if (bytes != null) {
          val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
          if (bmp != null) {
            // 裁剪为正方形
            val size = minOf(bmp.width, bmp.height)
            val x = (bmp.width - size) / 2
            val y = (bmp.height - size) / 2
            avatarBitmap = Bitmap.createBitmap(bmp, x, y, size, size)
            if (bmp !== avatarBitmap) bmp.recycle()
            AppLog.i(TAG, "avatar loaded: ${avatarBitmap?.width}x${avatarBitmap?.height}")
            syncNotification()
          }
        }
      } catch (e: Exception) {
        AppLog.e(TAG, "load avatar failed: $url", e)
      }
    }
  }

  private fun syncNotification() {
    try {
      NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
    } catch (e: Exception) {
      AppLog.e(TAG, "update notification failed", e)
    }
  }

  /**
   * 构建通知 — 参照 NeriPlayer。
   *
   * - setLargeIcon() 显示主播头像
   * - VISIBILITY_PUBLIC 锁屏可见
   * - MediaStyle 关联 MediaSession
   * - 不设置进度条（直播无需进度）
   */
  private fun buildNotification(): Notification {
    val packageIntent = packageManager.getLaunchIntentForPackage(packageName)
    val contentIntent = PendingIntent.getActivity(
      this, 0, packageIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val playPauseIntent = Intent(this, MediaPlaybackService::class.java).apply {
      action = ACTION_PLAY_PAUSE
    }
    val playPausePending = PendingIntent.getService(
      this, 1, playPauseIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val stopIntent = Intent(this, MediaPlaybackService::class.java).apply {
      action = ACTION_STOP
    }
    val stopPending = PendingIntent.getService(
      this, 2, stopIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
    val playPauseText = if (isPlaying) "暂停" else "播放"

    val builder = NotificationCompat.Builder(this, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.ic_media_play)
      .setContentTitle(streamerName.ifBlank { "DTTV" })
      .setContentText(title.ifBlank { "音频播放中" })
      .setContentIntent(contentIntent)
      .setOngoing(true)
      .setShowWhen(false)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .addAction(playPauseIcon, playPauseText, playPausePending)
      .addAction(android.R.drawable.ic_delete, "关闭", stopPending)
      // MediaStyle 关联 MediaSession，锁屏可见
      .setStyle(
        androidx.media.app.NotificationCompat.MediaStyle()
          .setMediaSession(mediaSession?.sessionToken)
          .setShowActionsInCompactView(0, 1)
      )

    // 主播头像
    avatarBitmap?.let { builder.setLargeIcon(it) }

    return builder.build()
  }
}
