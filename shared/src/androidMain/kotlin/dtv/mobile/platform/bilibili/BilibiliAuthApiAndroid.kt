package dtv.mobile.platform.bilibili

import dtv.mobile.repo.BilibiliQrCode
import dtv.mobile.repo.BilibiliQrPollResult
import dtv.mobile.repo.BilibiliQrStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class BilibiliAuthApiAndroid(
  private val client: HttpClient,
  private val cookieStore: BilibiliCookieStoreAndroid,
) {
  private val json = Json { ignoreUnknownKeys = true; isLenient = true }

  private fun JsonElement?.stringValueOrNull(): String? {
    val p = this as? kotlinx.serialization.json.JsonPrimitive ?: return null
    if (p is JsonNull) return null
    return p.content
  }

  suspend fun generateQrCode(): BilibiliQrCode {
    val url = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate"
    val text = client.get(url) {
      headers { append("User-Agent", "Mozilla/5.0") }
    }.bodyAsText()
    val root = json.parseToJsonElement(text).jsonObject
    val data = root["data"]?.jsonObject
    val qrUrl = data?.get("url").stringValueOrNull().orEmpty()
    val key = data?.get("qrcode_key").stringValueOrNull().orEmpty()
    if (qrUrl.isBlank() || key.isBlank()) error("B站二维码生成失败")
    return BilibiliQrCode(url = qrUrl, qrcodeKey = key)
  }

  suspend fun pollQrCode(qrcodeKey: String): BilibiliQrPollResult {
    val url = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=$qrcodeKey"
    val resp: HttpResponse = client.get(url) {
      headers { append("User-Agent", "Mozilla/5.0") }
    }
    val text = resp.bodyAsText()
    val root = json.parseToJsonElement(text).jsonObject
    val data = root["data"]?.jsonObject
    val code = data?.get("code")?.jsonPrimitive?.longOrNull ?: -1L
    val message = data?.get("message").stringValueOrNull()

    val status = when (code) {
      86101L -> BilibiliQrStatus.Waiting
      86090L -> BilibiliQrStatus.Scanned
      86038L -> BilibiliQrStatus.Expired
      0L -> BilibiliQrStatus.Confirmed
      else -> BilibiliQrStatus.Failed
    }

    if (status == BilibiliQrStatus.Confirmed) {
      val setCookie = resp.headers.getAll("Set-Cookie").orEmpty()
      cookieStore.mergeFromSetCookieHeaders(setCookie)
    }

    return BilibiliQrPollResult(status = status, message = message)
  }
}

