package dtv.mobile.platform.huya

import dtv.mobile.model.Platform
import dtv.mobile.model.Streamer
import dtv.mobile.util.formatViewerCountWanIfNeeded
import dtv.mobile.util.normalizeHttpUrl
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class HuyaLiveListApiAndroid(
  private val client: HttpClient,
) {
  private val json = Json { ignoreUnknownKeys = true; isLenient = true }

  private fun JsonElement?.stringValueOrNull(): String? {
    val p = this as? kotlinx.serialization.json.JsonPrimitive ?: return null
    if (p is JsonNull) return null
    return p.content
  }

  suspend fun fetchLiveList(gid: String, page: Int, pageSize: Int): List<Streamer> {
    val url = "https://live.huya.com/liveHttpUI/getLiveList?iGid=$gid&iPageNo=$page&iPageSize=$pageSize"
    val text = client.get(url) {
      headers { append("User-Agent", "Mozilla/5.0") }
    }.bodyAsText()

    val root = json.parseToJsonElement(text).jsonObject
    val vList = (root["vList"] ?: root["data"]?.jsonObject?.get("vList"))?.jsonArray ?: return emptyList()

    return vList.mapNotNull { el ->
      val obj = el.jsonObject
      val roomId = obj["lProfileRoom"]?.jsonPrimitive?.longOrNull?.toString().orEmpty()
      if (roomId.isBlank()) return@mapNotNull null
      val nickname = obj["sNick"].stringValueOrNull().orEmpty()
      val title = obj["sIntroduction"].stringValueOrNull().orEmpty()
      val avatar = normalizeHttpUrl(obj["sAvatar180"].stringValueOrNull())
      val cover = normalizeHttpUrl(obj["sScreenshot"].stringValueOrNull())
      val userCount = obj["lUserCount"]?.jsonPrimitive?.longOrNull ?: 0L
      val viewerStr = formatViewerCountWanIfNeeded(userCount.toString())

      Streamer(
        platform = Platform.Huya,
        roomId = roomId,
        name = nickname.ifBlank { "虎牙主播" },
        title = title.ifBlank { "直播中" },
        viewerText = viewerStr,
        avatarUrl = avatar,
        coverUrl = cover,
        isLive = true,
      )
    }
  }
}
