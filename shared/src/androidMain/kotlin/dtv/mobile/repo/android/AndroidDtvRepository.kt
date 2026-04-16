package dtv.mobile.repo.android

import android.content.Context
import dtv.mobile.util.AppLog
import dtv.mobile.model.Platform
import dtv.mobile.model.Streamer
import dtv.mobile.net.createHttpClient
import dtv.mobile.platform.bilibili.BilibiliAuthApiAndroid
import dtv.mobile.platform.bilibili.BilibiliCookieStoreAndroid
import dtv.mobile.platform.bilibili.BilibiliDanmakuClientAndroid
import dtv.mobile.platform.bilibili.BilibiliLiveListApiAndroid
import dtv.mobile.platform.bilibili.BilibiliStreamUrlResolverAndroid
import dtv.mobile.platform.douyin.DouyinWebApiAndroid
import dtv.mobile.platform.douyin.DouyinDanmakuClientAndroid
import dtv.mobile.platform.douyu.DouyuCate2DirectoryApi
import dtv.mobile.platform.douyu.DouyuCate3DirectoryApi
import dtv.mobile.platform.douyu.DouyuCategoriesApi
import dtv.mobile.platform.douyu.DouyuMobileApi
import dtv.mobile.platform.douyu.DouyuDanmakuClientAndroid
import dtv.mobile.platform.douyu.DouyuThreeCateApi
import dtv.mobile.platform.douyu.DouyuStreamUrlResolverAndroid
import dtv.mobile.platform.huya.HuyaDanmakuClientAndroid
import dtv.mobile.platform.huya.HuyaLiveListApiAndroid
import dtv.mobile.platform.huya.HuyaStreamUrlResolverAndroid
import dtv.mobile.repo.BilibiliQrCode
import dtv.mobile.repo.BilibiliQrPollResult
import dtv.mobile.repo.DouyuCate1
import dtv.mobile.repo.DouyuCate2
import dtv.mobile.repo.DouyuCate3
import dtv.mobile.repo.DouyuCategories
import dtv.mobile.repo.DouyuPlayInfo
import dtv.mobile.repo.DtvRepository
import dtv.mobile.repo.DanmakuMessage
import dtv.mobile.repo.PagedResult
import dtv.mobile.repo.fake.FakeDtvRepository
import dtv.mobile.util.normalizeHttpUrl
import kotlinx.coroutines.flow.Flow

class AndroidDtvRepository(
  private val appContext: Context,
) : DtvRepository {
  private val client = createHttpClient()

  private val douyuMobileApi = DouyuMobileApi(client)
  private val douyuCategoriesApi = DouyuCategoriesApi(client)
  private val douyuThreeCateApi = DouyuThreeCateApi(client)
  private val douyuCate2DirectoryApi = DouyuCate2DirectoryApi(client)
  private val douyuCate3DirectoryApi = DouyuCate3DirectoryApi(client)
  private val douyuStreamResolver = DouyuStreamUrlResolverAndroid(appContext, client)
  private val douyuDanmakuClient = DouyuDanmakuClientAndroid()

  private val huyaLiveListApi = HuyaLiveListApiAndroid(client)
  private val huyaStreamResolver = HuyaStreamUrlResolverAndroid(client)
  private val huyaDanmakuClient = HuyaDanmakuClientAndroid()

  private val douyinWebApi = DouyinWebApiAndroid(client)
  private val douyinDanmakuClient = DouyinDanmakuClientAndroid(appContext, douyinWebApi)

  private val bilibiliCookieStore = BilibiliCookieStoreAndroid(appContext)
  private val bilibiliAuthApi = BilibiliAuthApiAndroid(client, bilibiliCookieStore)
  private val bilibiliLiveListApi = BilibiliLiveListApiAndroid(client)
  private val bilibiliStreamResolver = BilibiliStreamUrlResolverAndroid(client) { bilibiliCookieStore.getCookie() }
  private val bilibiliDanmakuClient = BilibiliDanmakuClientAndroid(
    httpClient = client,
    cookieProvider = { bilibiliCookieStore.getCookie() },
  )

  private val fallback = FakeDtvRepository()

  override suspend fun fetchDouyuCategories(): DouyuCategories {
    return runCatching {
      val resp = douyuCategoriesApi.fetchCateList()
      if (resp.code != 0) return fallback.fetchDouyuCategories()
      val data = resp.data ?: return fallback.fetchDouyuCategories()

      val cate2ByCate1 = data.cate2Info.groupBy { it.cate1Id }
      val cate1List = data.cate1Info.map { c1 ->
        val rawC2 = cate2ByCate1[c1.cate1Id].orEmpty()
        val c2List = rawC2.map { c2 ->
          DouyuCate2(
            id = c2.cate2Id.toString(),
            name = c2.cate2Name,
            shortName = c2.shortName,
            iconUrl = c2.icon,
          )
        }.toMutableList()

        // 与桌面端 Rust 保持一致：娱乐天地(cate1Id=2) 下硬编码加入“一起看”
        if (c1.cate1Id == 2) {
          c2List.add(
            DouyuCate2(
              id = "208",
              name = "一起看",
              shortName = "yqk",
              iconUrl = "https://sta-op.douyucdn.cn/dycatr/7c723d30bfb4399be7592c9fa12026e3.png",
            ),
          )
        }

        DouyuCate1(
          id = c1.cate1Id.toString(),
          name = c1.cate1Name,
          cate2List = c2List,
        )
      }

      DouyuCategories(cate1List = cate1List)
    }.getOrElse { fallback.fetchDouyuCategories() }
  }

  override suspend fun fetchDouyuThreeCate(cate2Id: String): List<DouyuCate3> {
    return runCatching {
      val resp = douyuThreeCateApi.fetchThreeCate(cate2Id)
      if (resp.error != 0) return emptyList()
      resp.data.orEmpty()
        .mapNotNull { raw ->
          val id = raw.id.trim()
          val name = raw.name.trim()
          if (id.isEmpty() || name.isEmpty()) return@mapNotNull null
          DouyuCate3(id = id, name = name, iconUrl = raw.iconResolved)
        }
    }.getOrDefault(emptyList())
  }

  override suspend fun fetchDouyuLiveListByCate2(cate2Id: String, offset: Int, limit: Int): PagedResult<Streamer> {
    return runCatching {
      // Douyu 的 `m.douyu.com/hgapi/.../newRecList` 近期经常返回 “系统异常”，改用网页目录接口 mixListV1 更稳定。
      val page = (offset / limit) + 1
      val resp = douyuCate2DirectoryApi.fetchMixListV1(cate2Id = cate2Id, page = page, limit = limit)
      if (resp.code != 0) return PagedResult(emptyList(), total = null)
      val data = resp.data ?: return PagedResult(emptyList(), total = null)
      val items = data.rl.map { s ->
        Streamer(
          platform = Platform.Douyu,
          roomId = s.rid.toString(),
          name = s.nn,
          title = s.rn,
          viewerText = s.ol.toString(),
          avatarUrl = normalizeHttpUrl(s.av.ifBlank { null }),
          coverUrl = normalizeHttpUrl(s.rs16.ifBlank { null }),
          isLive = s.type?.let { it == 1 } ?: true,
        )
      }
      PagedResult(items = items, total = null)
    }.getOrElse {
      PagedResult(items = emptyList(), total = null)
    }
  }

  override suspend fun fetchDouyuLiveListByCate3(cate3Id: String, page: Int, limit: Int): PagedResult<Streamer> {
    return runCatching {
      val resp = douyuCate3DirectoryApi.fetchMixListV1(cate3Id = cate3Id, page = page, limit = limit)
      if (resp.code != 0) return PagedResult(emptyList(), total = null)
      val data = resp.data ?: return PagedResult(emptyList(), total = null)
      val items = data.rl.map { s ->
        Streamer(
          platform = Platform.Douyu,
          roomId = s.rid.toString(),
          name = s.nn,
          title = s.rn,
          viewerText = s.ol.toString(),
          avatarUrl = normalizeHttpUrl(s.av.ifBlank { null }),
          coverUrl = normalizeHttpUrl(s.rs16.ifBlank { null }),
          isLive = s.type?.let { it == 1 } ?: true,
        )
      }
      PagedResult(items = items, total = null)
    }.getOrElse {
      PagedResult(items = emptyList(), total = null)
    }
  }

  override suspend fun fetchDouyuPlayInfo(roomId: String): DouyuPlayInfo {
    return runCatching { douyuStreamResolver.fetchPlayInfo(roomId) }
      .onFailure { AppLog.e("DTV-Douyu", "fetchPlayInfo failed roomId=$roomId", it) }
      .getOrThrow()
  }

  override suspend fun resolveDouyuStreamUrl(roomId: String, quality: String?, cdn: String?): String {
    return runCatching {
      douyuStreamResolver.resolve(roomId = roomId, quality = quality, cdn = cdn)
    }
      .onSuccess { AppLog.i("DTV-Douyu", "resolved stream url roomId=$roomId quality=$quality cdn=$cdn url=$it") }
      .onFailure { AppLog.e("DTV-Douyu", "resolve stream url failed roomId=$roomId quality=$quality cdn=$cdn", it) }
      .getOrThrow()
  }

  override fun observeDouyuDanmaku(roomId: String): Flow<DanmakuMessage> {
    return douyuDanmakuClient.observe(roomId)
  }

  override fun observeHuyaDanmaku(roomId: String): Flow<DanmakuMessage> {
    return huyaDanmakuClient.observe(roomId)
  }

  override fun observeDouyinDanmaku(webRid: String): Flow<DanmakuMessage> {
    return douyinDanmakuClient.observe(webRid)
  }

  override suspend fun fetchHuyaLiveList(gid: String, page: Int, limit: Int): PagedResult<Streamer> {
    return runCatching {
      val items = huyaLiveListApi.fetchLiveList(gid = gid, page = page, pageSize = limit)
      PagedResult(items = items, total = null)
    }.getOrElse { PagedResult(items = emptyList(), total = null) }
  }

  override suspend fun resolveHuyaStreamUrl(roomId: String): String {
    return runCatching { huyaStreamResolver.resolve(roomId) }
      .onFailure { AppLog.e("DTV-Huya", "resolve stream url failed roomId=$roomId", it) }
      .getOrThrow()
  }

  override suspend fun fetchDouyinPartitionLiveList(
    partition: String,
    partitionType: String,
    offset: Int,
    limit: Int,
    msToken: String,
  ): PagedResult<Streamer> {
    return runCatching {
      AppLog.i("DTV-Douyin", "fetch partition list partition=$partition type=$partitionType offset=$offset limit=$limit")
      val resp = douyinWebApi.fetchPartitionRooms(
        partition = partition,
        partitionType = partitionType,
        offset = offset,
        limit = limit,
        msToken = msToken,
      )
      val items = resp.rooms.map { r ->
        Streamer(
          platform = Platform.Douyin,
          roomId = r.webRid,
          name = r.ownerNickname,
          title = r.title,
          viewerText = r.userCountStr,
          avatarUrl = normalizeHttpUrl(r.avatarUrl),
          coverUrl = normalizeHttpUrl(r.coverUrl),
          isLive = true,
        )
      }
      AppLog.i("DTV-Douyin", "fetch partition list ok partition=$partition offset=$offset got=${items.size} hasMore=${resp.hasMore}")
      PagedResult(items = items, total = null)
    }.getOrElse { err ->
      AppLog.e("DTV-Douyin", "fetch partition live list failed partition=$partition type=$partitionType offset=$offset", err)
      PagedResult(items = emptyList(), total = null)
    }
  }

  override suspend fun resolveDouyinStreamUrl(webRid: String, desiredQuality: String?): String {
    return runCatching {
      val room = douyinWebApi.fetchRoomEnter(webRid)
      room.roomId?.takeIf { it.isNotBlank() }?.let { roomId ->
        douyinDanmakuClient.prime(webRid = webRid, roomId = roomId, msToken = room.msToken)
      }
      val hls = room.hlsPullUrlMap["ORIGIN"] ?: room.hlsPullUrlMap.values.firstOrNull()
      val flv = room.flvPullUrlMap["ORIGIN"] ?: room.flvPullUrlMap.values.firstOrNull()
      // Mobile reality: some Douyin FLV streams may be H.265-in-FLV which Media3/ExoPlayer may not render
      // (audio-only on many devices). Prefer HLS first; fallback to FLV.
      (hls ?: flv) ?: error("未找到抖音播放地址")
    }
      .onFailure { AppLog.e("DTV-Douyin", "resolve stream url failed webRid=$webRid", it) }
      .getOrThrow()
  }

  override suspend fun fetchBilibiliLiveList(parentAreaId: Int, areaId: Int, page: Int, pageSize: Int): PagedResult<Streamer> {
    return runCatching {
      val items = bilibiliLiveListApi.fetchLiveList(parentAreaId = parentAreaId, areaId = areaId, page = page, pageSize = pageSize)
      PagedResult(items = items, total = null)
    }.getOrElse { err ->
      AppLog.e("DTV-Bilibili", "fetch live list failed parent=$parentAreaId area=$areaId page=$page", err)
      PagedResult(items = emptyList(), total = null)
    }
  }

  override suspend fun resolveBilibiliStreamUrl(roomId: String, qn: Int?): String {
    return runCatching { bilibiliStreamResolver.resolve(roomId = roomId, qn = qn) }
      .onFailure { AppLog.e("DTV-Bilibili", "resolve stream url failed roomId=$roomId", it) }
      .getOrThrow()
  }

  override fun observeBilibiliDanmaku(roomId: String): Flow<DanmakuMessage> = bilibiliDanmakuClient.observe(roomId)

  override suspend fun generateBilibiliQrCode(): BilibiliQrCode {
    return bilibiliAuthApi.generateQrCode()
  }

  override suspend fun pollBilibiliQrCode(qrcodeKey: String): BilibiliQrPollResult {
    return bilibiliAuthApi.pollQrCode(qrcodeKey)
  }

  override suspend fun getBilibiliCookie(): String? {
    return bilibiliCookieStore.getCookie()
  }

  override suspend fun clearBilibiliCookie() {
    bilibiliCookieStore.clear()
  }
}
