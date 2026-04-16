package dtv.mobile.platform.bilibili

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class BilibiliStreamUrlResolverAndroid(
  private val client: HttpClient,
  private val cookieProvider: () -> String?,
) {
  private val json = Json { ignoreUnknownKeys = true; isLenient = true }

  private fun JsonElement?.stringValueOrNull(): String? {
    val p = this as? kotlinx.serialization.json.JsonPrimitive ?: return null
    if (p is JsonNull) return null
    return p.content
  }

  private fun JsonElement?.longValueOrNull(): Long? {
    return (this as? kotlinx.serialization.json.JsonPrimitive)?.longOrNull
  }

  suspend fun resolve(roomId: String, qn: Int? = null): String {
    val quality = qn ?: 10000
    val url =
      "https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo?room_id=$roomId&protocol=0,1&format=0,1,2&codec=0,1&qn=$quality&platform=web"
    val text = client.get(url) {
      headers {
        append("User-Agent", "Mozilla/5.0")
        append("Referer", "https://live.bilibili.com/")
        val cookie = cookieProvider()?.trim().orEmpty()
        if (cookie.isNotEmpty()) append("Cookie", cookie)
      }
    }.bodyAsText()

    val root = json.parseToJsonElement(text).jsonObject
    val code = root["code"].longValueOrNull() ?: -1L
    if (code != 0L) error("B站播放地址获取失败: code=$code")

    val streams =
      root["data"]?.jsonObject
        ?.get("playurl_info")?.jsonObject
        ?.get("playurl")?.jsonObject
        ?.get("stream")?.jsonArray
        ?: error("B站播放数据为空")

    data class Candidate(val baseUrl: String, val host: String, val extra: String)

    fun candidatesOfStream(stream: JsonObject): List<Candidate> {
      val formats = stream["format"]?.jsonArray ?: return emptyList()
      return formats.flatMap { f ->
        val formatObj = f.jsonObject
        val codecs = formatObj["codec"]?.jsonArray ?: return@flatMap emptyList()
        codecs.mapNotNull { c ->
          val codecObj = c.jsonObject
          val baseUrl = codecObj["base_url"].stringValueOrNull()?.trim().orEmpty()
          if (baseUrl.isBlank()) return@mapNotNull null
          val urlInfo0 = codecObj["url_info"]?.jsonArray?.firstOrNull()?.jsonObject
          val host = urlInfo0?.get("host").stringValueOrNull()?.trim().orEmpty()
          val extra = urlInfo0?.get("extra").stringValueOrNull()?.trim().orEmpty()
          if (host.isBlank()) return@mapNotNull null
          Candidate(baseUrl = baseUrl, host = host, extra = extra)
        }
      }
    }

    val all = streams.flatMap { candidatesOfStream(it.jsonObject) }
    if (all.isEmpty()) error("B站未解析到播放地址")

    fun score(c: Candidate): Int {
      val b = c.baseUrl.lowercase()
      return when {
        b.contains("index.m3u8") -> 0
        b.contains(".m3u8") -> 1
        b.contains(".flv") -> 2
        else -> 9
      }
    }

    val best = all.minBy { score(it) }
    val extraPart = best.extra.trimStart('?', '&')
    val base = best.baseUrl
    val joiner = when {
      extraPart.isEmpty() -> ""
      base.endsWith("?") || base.endsWith("&") -> ""
      base.contains("?") -> "&"
      else -> "?"
    }
    return buildString {
      append(best.host.trimEnd('/'))
      append(base)
      if (extraPart.isNotEmpty()) {
        append(joiner)
        append(extraPart)
      }
    }
  }
}
