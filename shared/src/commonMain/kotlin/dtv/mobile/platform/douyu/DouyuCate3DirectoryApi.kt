package dtv.mobile.platform.douyu

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DouyuCate3DirectoryApi(
  private val client: HttpClient,
) {
  suspend fun fetchMixListV1(
    cate3Id: String,
    page: Int,
    limit: Int,
  ): DouyuMixListV1Response {
    val currentPage = if (page <= 0) 1 else page
    val path = "https://www.douyu.com/gapi/rkc/directory/mixListV1/3_${cate3Id}/$currentPage"
    val url = URLBuilder(path).apply { parameters.append("limit", limit.toString()) }.buildString()
    return client.get(url).body()
  }
}

@Serializable
data class DouyuMixListV1Response(
  val code: Int = -1,
  val msg: String? = null,
  val data: DouyuMixListV1Data? = null,
)

@Serializable
data class DouyuMixListV1Data(
  @SerialName("rl") val rl: List<DouyuMixListV1Streamer> = emptyList(),
)

@Serializable
data class DouyuMixListV1Streamer(
  val rid: Long,
  @SerialName("rn") val rn: String = "",
  @SerialName("nn") val nn: String = "",
  @SerialName("av") val av: String = "",
  @SerialName("ol") val ol: Long = 0,
  @SerialName("rs16") val rs16: String = "",
  @SerialName("type") val type: Int? = null,
)

