package dtv.mobile.platform.douyin

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import dtv.mobile.util.AppLog
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

internal class DouyinWebViewCookieAndroid(
  private val appContext: Context,
) {
  companion object {
    private const val TAG = "DTV-Douyin"
  }

  @SuppressLint("SetJavaScriptEnabled")
  suspend fun fetch(url: String, userAgent: String, timeoutMs: Long = 10_000L): String? =
    suspendCancellableCoroutine { cont ->
      val handler = Handler(Looper.getMainLooper())

      fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else handler.post(block)
      }

      runOnMain {
        val cm = CookieManager.getInstance()
        cm.setAcceptCookie(true)

        val provider = runCatching {
          if (Build.VERSION.SDK_INT >= 26) WebView.getCurrentWebViewPackage()?.packageName else null
        }.getOrNull()
        if (provider.isNullOrBlank()) {
          AppLog.w(TAG, "douyin webview cookie: no WebView provider")
        } else {
          AppLog.i(TAG, "douyin webview cookie: provider=$provider")
        }

        val webView = WebView(appContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          runCatching { cm.setAcceptThirdPartyCookies(webView, true) }
        }

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.userAgentString = userAgent

        var finished = false

        fun cleanup() {
          runCatching { webView.stopLoading() }
          runCatching { webView.destroy() }
        }

        fun pollAndComplete() {
          val cookie = runCatching { cm.getCookie(url) }.getOrNull().orEmpty()
          val hasNonce = cookie.contains("__ac_nonce=")
          val hasScid = cookie.contains("tt_scid=")
          if (cookie.isNotBlank() && (hasNonce || hasScid)) {
            AppLog.i(TAG, "douyin webview cookie ok nonce=$hasNonce scid=$hasScid len=${cookie.length}")
            finished = true
            cleanup()
            cont.resume(cookie)
            return
          }
          if (!finished) handler.postDelayed({ pollAndComplete() }, 350L)
        }

        webView.webViewClient = object : WebViewClient() {
          override fun onPageFinished(view: WebView?, u: String?) {
            if (finished) return
            handler.postDelayed({ pollAndComplete() }, 650L)
          }

          @Deprecated("Deprecated in Java")
          override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            if (finished) return
            AppLog.w(TAG, "douyin webview cookie page error code=$errorCode desc=${description.orEmpty()}")
          }

          override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            if (finished) return
            AppLog.w(TAG, "douyin webview cookie page error ${error?.errorCode} desc=${error?.description?.toString().orEmpty()}")
          }
        }

        val timeout = Runnable {
          if (finished) return@Runnable
          val cookie = runCatching { cm.getCookie(url) }.getOrNull()
          AppLog.w(TAG, "douyin webview cookie timeout len=${cookie?.length ?: 0}")
          finished = true
          cleanup()
          cont.resume(cookie)
        }
        handler.postDelayed(timeout, timeoutMs)

        cont.invokeOnCancellation {
          handler.removeCallbacks(timeout)
          cleanup()
        }

        runCatching { webView.loadUrl(url) }
          .onFailure { t ->
            handler.removeCallbacks(timeout)
            finished = true
            cleanup()
            cont.resumeWithException(t)
          }
      }
    }
}
