package dtv.mobile.player

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 定时关闭管理器。
 *
 * 支持倒计时模式：设定分钟后自动触发回调。
 */
class SleepTimerManager {

  private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var timerJob: Job? = null

  private val _remainingSeconds = MutableStateFlow(0)
  val remainingSeconds: StateFlow<Int> = _remainingSeconds

  private val _isActive = MutableStateFlow(false)
  val isActive: StateFlow<Boolean> = _isActive

  private var onExpired: (() -> Unit)? = null

  /**
   * 启动定时器。
   * @param minutes 倒计时分钟数
   * @param onExpired 倒计时结束时的回调
   */
  fun start(minutes: Int, onExpired: () -> Unit) {
    cancel()
    this.onExpired = onExpired
    _remainingSeconds.value = minutes * 60
    _isActive.value = true

    timerJob = scope.launch {
      while (_remainingSeconds.value > 0) {
        delay(1000)
        _remainingSeconds.value -= 1
      }
      // 倒计时结束
      _isActive.value = false
      onExpired()
    }
  }

  /**
   * 取消定时器。
   */
  fun cancel() {
    timerJob?.cancel()
    timerJob = null
    _remainingSeconds.value = 0
    _isActive.value = false
    onExpired = null
  }

  /**
   * 获取剩余时间的格式化文字，如 "12:34"。
   */
  fun getRemainingText(): String {
    val total = _remainingSeconds.value
    if (total <= 0) return ""
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
  }

  /**
   * 获取剩余时间的详细格式化文字，如 "12分34秒后关闭"。
   */
  fun getRemainingDetailText(): String {
    val total = _remainingSeconds.value
    if (total <= 0) return ""
    val minutes = total / 60
    val seconds = total % 60
    return if (minutes > 0) {
      "${minutes}分${seconds}秒后关闭"
    } else {
      "${seconds}秒后关闭"
    }
  }

  /**
   * 释放资源。
   */
  fun release() {
    cancel()
    scope.cancel()
  }
}
