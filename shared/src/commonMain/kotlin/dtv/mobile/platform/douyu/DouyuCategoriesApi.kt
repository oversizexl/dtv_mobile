package dtv.mobile.platform.douyu

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DouyuCategoriesApi(
  private val client: HttpClient,
) {
  suspend fun fetchCateList(): DouyuCateListResponse {
    return client.get("https://m.douyu.com/api/cate/list").body()
  }
}

@Serializable
data class DouyuCateListResponse(
  // Douyu uses `code` (sometimes aliased as error)
  @SerialName("code") val code: Int = -1,
  val msg: String? = null,
  val data: DouyuCateListData? = null,
)

@Serializable
data class DouyuCateListData(
  @SerialName("cate1Info") val cate1Info: List<DouyuCate1Raw> = emptyList(),
  @SerialName("cate2Info") val cate2Info: List<DouyuCate2Raw> = emptyList(),
)

@Serializable
data class DouyuCate1Raw(
  @SerialName("cate1Id") val cate1Id: Int,
  @SerialName("cate1Name") val cate1Name: String,
)

@Serializable
data class DouyuCate2Raw(
  @SerialName("cate1Id") val cate1Id: Int,
  @SerialName("cate2Id") val cate2Id: Int,
  @SerialName("cate2Name") val cate2Name: String,
  @SerialName("shortName") val shortName: String = "",
  val icon: String? = null,
)

