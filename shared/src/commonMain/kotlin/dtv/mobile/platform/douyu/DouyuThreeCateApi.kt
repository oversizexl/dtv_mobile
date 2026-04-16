package dtv.mobile.platform.douyu

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DouyuThreeCateApi(
  private val client: HttpClient,
) {
  suspend fun fetchThreeCate(tagId: String): DouyuThreeCateResponse {
    val url = URLBuilder("https://capi.douyucdn.cn/api/v1/getThreeCate")
      .apply {
        parameters.append("tag_id", tagId)
        parameters.append("client_sys", "android")
      }
      .buildString()
    return client.get(url).body()
  }
}

@Serializable
data class DouyuThreeCateResponse(
  val error: Int = -1,
  val msg: String? = null,
  val data: List<DouyuThreeCateItemRaw>? = null,
)

@Serializable
data class DouyuThreeCateItemRaw(
  @SerialName("tagId") val tagId: String? = null,
  @SerialName("cateId") val cateId: String? = null,
  @SerialName("tagName") val tagName: String? = null,
  @SerialName("cateName") val cateName: String? = null,
  @SerialName("icon") val icon: String? = null,
  @SerialName("pic") val pic: String? = null,
  @SerialName("iconUrl") val iconUrl: String? = null,
) {
  val id: String
    get() = tagId ?: cateId ?: ""

  val name: String
    get() = tagName ?: cateName ?: ""

  val iconResolved: String?
    get() = iconUrl ?: pic ?: icon
}

