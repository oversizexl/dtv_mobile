package dtv.mobile.platform.douyu

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DouyuMobileApi(
  private val client: HttpClient,
) {
  suspend fun fetchNewRecList(
    cate2: String,
    offset: Int,
    limit: Int,
  ): NewRecListResponse {
    val url = URLBuilder("https://m.douyu.com/hgapi/live/cate/newRecList")
      .apply {
        parameters.append("offset", offset.toString())
        parameters.append("cate2", cate2)
        parameters.append("limit", limit.toString())
      }
      .buildString()

    return client.get(url).body()
  }
}

@Serializable
data class NewRecListResponse(
  val error: Int = -1,
  val msg: String? = null,
  val data: NewRecListData? = null,
)

@Serializable
data class NewRecListData(
  val list: List<NewRecStreamer> = emptyList(),
  val total: Int = 0,
)

@Serializable
data class NewRecStreamer(
  val rid: Long,
  @SerialName("roomName") val roomName: String = "",
  val nickname: String = "",
  @SerialName("roomSrc") val roomSrc: String = "",
  val avatar: String = "",
  val hn: String = "0",
)

