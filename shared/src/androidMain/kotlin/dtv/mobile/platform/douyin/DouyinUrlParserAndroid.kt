package dtv.mobile.platform.douyin

import dtv.mobile.util.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 解析抖音直播间链接，提取 webRid（房间号）。
 *
 * 支持的链接格式：
 * - https://v.douyin.com/xxxxx  （短链接，需 HTTP 重定向）
 * - https://live.douyin.com/xxxxx （直播间页面）
 * - https://webcast.amemv.com/douyin/webcast/reflow/xxxxx （重定向链接）
 * - 纯数字房间号
 */
object DouyinUrlParserAndroid {

  private val client = OkHttpClient.Builder()
    .followRedirects(true)
    .followSslRedirects(true)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  private const val TAG = "DTV-DouyinUrlParser"

  /**
   * 从用户输入中提取抖音 webRid。
   * 返回 null 表示不是有效的抖音链接/房间号。
   */
  suspend fun parseWebRid(input: String): String? = withContext(Dispatchers.IO) {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return@withContext null

    try {
      when {
        // 短链接：v.douyin.com
        "v.douyin.com" in trimmed -> resolveShortUrl(trimmed)
        // 直播间页面：live.douyin.com
        "live.douyin.com" in trimmed -> extractFromLiveUrl(trimmed)
        // 重定向链接：webcast.amemv.com
        "webcast.amemv.com" in trimmed -> extractFromReflowUrl(trimmed)
        // 纯数字（6位以上，大概率是房间号）
        trimmed.matches(Regex("^\\d{6,}$")) -> trimmed
        else -> null
      }
    } catch (e: Exception) {
      AppLog.e(TAG, "parseWebRid failed input=$trimmed", e)
      null
    }
  }

  /**
   * 解析短链接：跟随重定向，然后从页面内容中提取 webRid。
   *
   * 抖音短链接重定向到 webcast.amemv.com/.../reflow/{roomId}，
   * 但我们需要的是 webRid（用户可见房间号），它在页面的 RSC 数据中。
   */
  private fun resolveShortUrl(shortUrl: String): String? {
    val request = Request.Builder()
      .url(shortUrl)
      .header("User-Agent", DouyinWebApiAndroid.DEFAULT_USER_AGENT)
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      .header("Accept-Language", "zh-CN,zq=0.9,en;q=0.8")
      .get()
      .build()

    val response = client.newCall(request).execute()
    val finalUrl = response.request.url.toString()
    AppLog.i(TAG, "short url resolved: $shortUrl -> $finalUrl")

    // 先尝试从最终 URL 直接提取（如果是 live.douyin.com 格式）
    val fromUrl = extractFromLiveUrl(finalUrl)
    if (fromUrl != null) return fromUrl

    // 从页面内容中提取 webRid（RSC 数据中包含 \"webRid":"xxx"）
    val body = response.body?.string()
    if (body != null) {
      val webRid = extractWebRidFromPageContent(body)
      if (webRid != null) {
        AppLog.i(TAG, "extracted webRid from page content: $webRid")
        return webRid
      }
      // 如果找不到 webRid，尝试从页面提取 roomId（idStr）并用它作为 fallback
      val roomId = extractRoomIdFromPageContent(body)
      if (roomId != null) {
        AppLog.w(TAG, "webRid not found, using roomId as fallback: $roomId")
        return roomId
      }
    }

    AppLog.e(TAG, "failed to extract webRid from short url: $shortUrl")
    return null
  }

  /**
   * 从 live.douyin.com URL 中提取数字部分作为 webRid。
   * 例如: https://live.douyin.com/123456789 -> "123456789"
   */
  private fun extractFromLiveUrl(url: String): String? {
    val match = Regex("live\\.douyin\\.com/(\\d{6,})").find(url)
    return match?.groupValues?.get(1)
  }

  /**
   * 从 reflow URL 中获取页面并提取 webRid。
   * reflow URL 只包含内部 roomId，webRid 在页面内容中。
   */
  private fun extractFromReflowUrl(url: String): String? {
    val request = Request.Builder()
      .url(url)
      .header("User-Agent", DouyinWebApiAndroid.DEFAULT_USER_AGENT)
      .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
      .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
      .get()
      .build()

    val response = client.newCall(request).execute()
    val body = response.body?.string() ?: return null

    val webRid = extractWebRidFromPageContent(body)
    if (webRid != null) {
      AppLog.i(TAG, "extracted webRid from reflow page: $webRid")
      return webRid
    }

    // fallback: 从 URL 中提取 roomId
    val roomId = Regex("reflow/(\\d{6,})").find(url)?.groupValues?.get(1)
    if (roomId != null) {
      AppLog.w(TAG, "webRid not found in reflow page, using roomId: $roomId")
    }
    return roomId
  }

  /**
   * 从页面 HTML 内容中提取 webRid。
   *
   * 抖音页面使用 React Server Components (RSC) 格式，
   * 数据在 self.__rsc_f.push([1,"..."]) 的 script 标签中。
   * webRid 在 \"owner\":{...\"webRid\":\"xxx\"...} 结构中。
   */
  private fun extractWebRidFromPageContent(html: String): String? {
    // 方法1: 从 RSC 数据中查找 \"webRid\":{...\"webRid\":\"xxx\"
    // 页面中数据是 JS 转义的: \"webRid\":\"138946520035\"
    val patterns = listOf(
      // 转义引号格式: \"webRid\":\"xxx\"
      Regex("\\\\?\"webRid\\\\?\"\\s*:\\s*\\\\?\"(\\d+)\\\\?\""),
      // 无转义格式: "webRid":"xxx"
      Regex("\"webRid\"\\s*:\\s*\"(\\d+)\""),
      // webRid= 格式
      Regex("webRid[=:]\\s*\"?(\\d{6,})"),
    )

    for (pattern in patterns) {
      val match = pattern.find(html)
      if (match != null) {
        val webRid = match.groupValues[1]
        if (webRid.length >= 6) {
          return webRid
        }
      }
    }
    return null
  }

  /**
   * 从页面内容中提取内部 roomId (idStr)。
   */
  private fun extractRoomIdFromPageContent(html: String): String? {
    val patterns = listOf(
      Regex("\\\\?\"idStr\\\\?\"\\s*:\\s*\\\\?\"(\\d+)\\\\?\""),
      Regex("\"idStr\"\\s*:\\s*\"(\\d+)\""),
    )
    for (pattern in patterns) {
      val match = pattern.find(html)
      if (match != null) {
        return match.groupValues[1]
      }
    }
    return null
  }

  /**
   * 检测输入是否可能是抖音链接。
   */
  fun looksLikeDouyinLink(input: String): Boolean {
    val trimmed = input.trim()
    return "v.douyin.com" in trimmed ||
      "live.douyin.com" in trimmed ||
      "webcast.amemv.com" in trimmed ||
      "douyin.com" in trimmed
  }
}
