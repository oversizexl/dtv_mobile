package dtv.mobile.platform.bilibili

import dtv.mobile.model.Platform
import dtv.mobile.model.Streamer
import dtv.mobile.util.normalizeHttpUrl
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

class BilibiliLiveListApiAndroid(
  private val client: HttpClient,
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

  suspend fun fetchLiveList(
    parentAreaId: Int,
    areaId: Int,
    page: Int,
    pageSize: Int,
  ): List<Streamer> {
    val url =
      "https://api.live.bilibili.com/xlive/web-interface/v1/index/getList?platform=web&parent_area_id=$parentAreaId&area_id=$areaId&page=$page&page_size=$pageSize"
    val text = client.get(url) {
      headers {
        append("User-Agent", "Mozilla/5.0")
        append("Referer", "https://live.bilibili.com/")
      }
    }.bodyAsText()

    val root = json.parseToJsonElement(text).jsonObject
    val code = root["code"]?.jsonPrimitive?.longOrNull ?: -1
    if (code != 0L) return emptyList()
    val data = root["data"]?.jsonObject ?: return emptyList()

    fun extractFromModules(arr: JsonArray?): List<JsonObject> {
      if (arr == null) return emptyList()
      return arr.flatMap { mod ->
        val obj = mod.jsonObject
        val list = obj["list"]?.jsonArray ?: return@flatMap emptyList()
        list.mapNotNull { it as? JsonObject }
      }
    }

    val rooms = buildList {
      addAll(extractFromModules(data["recommend_room_list"] as? JsonArray))
      addAll(extractFromModules(data["room_list"] as? JsonArray))
    }

    return rooms.mapNotNull { r ->
      val roomId = r["roomid"].longValueOrNull()?.toString().orEmpty()
      if (roomId.isBlank()) return@mapNotNull null
      val name = r["uname"].stringValueOrNull()?.trim().orEmpty()
      val title = r["title"].stringValueOrNull()?.trim().orEmpty()
      val cover = normalizeHttpUrl(r["cover"].stringValueOrNull() ?: r["keyframe"].stringValueOrNull())
      val avatar = normalizeHttpUrl(r["face"].stringValueOrNull())
      val online = r["online"].longValueOrNull() ?: 0L
      val viewerStr = if (online >= 10_000) String.format("%.1f万", online / 10_000.0) else online.toString()

      Streamer(
        platform = Platform.Bilibili,
        roomId = roomId,
        name = name.ifBlank { "B站主播" },
        title = title.ifBlank { "直播中" },
        viewerText = viewerStr,
        avatarUrl = avatar,
        coverUrl = cover,
        isLive = true,
      )
    }
  }
}
