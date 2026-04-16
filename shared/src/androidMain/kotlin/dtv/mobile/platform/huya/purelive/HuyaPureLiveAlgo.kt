package dtv.mobile.platform.huya.purelive

import android.util.Base64
import dtv.mobile.platform.huya.purelive.tars.HYMessage
import dtv.mobile.platform.huya.purelive.tars.HYPushMessage
import dtv.mobile.platform.huya.purelive.tars.TarsInputStream
import dtv.mobile.platform.huya.purelive.tars.TarsOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

internal object HuyaPureLiveAlgo {
  const val SERVER_URL: String = "wss://cdnws.api.huya.com"

  // Heartbeat: copied 1:1 from `kotlin-huya-danmaku` (base64: ABQdAAwsNgBM)
  val HEARTBEAT_DATA: ByteArray = Base64.decode("ABQdAAwsNgBM", Base64.DEFAULT)

  private const val USER_AGENT: String =
    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.0.0 Safari/537.36"

  data class JoinArgs(
    val yyid: Long,
    val topSid: Int,
    val subSid: Int,
  )

  fun fetchJoinArgs(okHttpClient: OkHttpClient, roomId: String): JoinArgs? {
    val url = "https://mp.huya.com/cache.php?m=Live&do=profileRoom&roomid=$roomId&showSecret=1"
    val req = Request.Builder()
      .url(url)
      .header("Accept", "*/*")
      .header("Origin", "https://www.huya.com")
      .header("Referer", "https://www.huya.com/")
      .header("User-Agent", USER_AGENT)
      .build()

    val resp = okHttpClient.newCall(req).execute()
    resp.use {
      if (!it.isSuccessful) return null
      val body = it.body?.string() ?: return null
      val root = JSONObject(body)
      if (root.optInt("status") != 200) return null
      val data = root.optJSONObject("data") ?: return null

      val profile = data.optJSONObject("profileInfo") ?: return null
      val yyid = profile.optLong("yyid", 0L)
      if (yyid == 0L) return null

      val stream = data.optJSONObject("stream") ?: return null
      val base = stream.optJSONArray("baseSteamInfoList") ?: return null
      if (base.length() <= 0) return null
      val first = base.optJSONObject(0) ?: return null

      val topSid = first.optInt("lChannelId", 0)
      val subSid = first.optInt("lSubChannelId", 0)
      if (topSid == 0) return null

      return JoinArgs(yyid = yyid, topSid = topSid, subSid = if (subSid == 0) topSid else subSid)
    }
  }

  fun buildJoinPacket(yyid: Long, topSid: Int, subSid: Int): ByteArray {
    val inner = TarsOutputStream()
    inner.writeLong(yyid, tag = 0)
    inner.writeBool(true, tag = 1)
    inner.writeString("", tag = 2)
    inner.writeString("", tag = 3)
    inner.writeInt(topSid, tag = 4)
    inner.writeInt(subSid, tag = 5)
    inner.writeInt(0, tag = 6)
    inner.writeInt(0, tag = 7)

    val outer = TarsOutputStream()
    outer.writeInt(1, tag = 0)
    outer.writeByteArray(inner.toByteArray(), tag = 1)
    return outer.toByteArray()
  }

  data class ChatDecoded(
    val user: String,
    val content: String,
  )

  fun decodeChatOrNull(data: ByteArray): ChatDecoded? {
    val stream = TarsInputStream(data)
    val type = stream.readInt(tag = 0, required = false, defaultValue = 0)
    if (type != 7) return null

    val innerBytes = stream.readByteArray(tag = 1, required = false) ?: return null
    // Huya ws push：payload 不是一个带 STRUCT_BEGIN/END 包裹的 struct，而是“直接写入字段”的数据块
    val pushStream = TarsInputStream(innerBytes)
    val push = HYPushMessage().also { it.readFrom(pushStream) }
    if (push.uri != 1400) return null

    val msgStream = TarsInputStream(push.msg)
    val msg = HYMessage().also { it.readFrom(msgStream) }
    val user = msg.userInfo.nickName
    val content = msg.content
    if (user.isBlank() || content.isBlank()) return null
    return ChatDecoded(user = user, content = content)
  }
}
