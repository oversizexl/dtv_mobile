package dtv.mobile.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import dtv.mobile.model.Platform
import dtv.mobile.model.Streamer
import dtv.mobile.repo.DanmakuMessage
import dtv.mobile.repo.DouyuPlayInfo
import dtv.mobile.state.AppState
import dtv.mobile.ui.components.NetworkImage
import dtv.mobile.ui.player.StreamPlayer
import dtv.mobile.ui.system.FullscreenEffect
import dtv.mobile.ui.system.PlatformBackHandler
import dtv.mobile.util.normalizeHttpUrl
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import kotlin.math.roundToInt
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
  appState: AppState,
  streamer: Streamer?,
  modifier: Modifier = Modifier,
) {
  var url by remember(streamer?.roomId) { mutableStateOf<String?>(null) }
  var error by remember(streamer?.roomId) { mutableStateOf<String?>(null) }
  var loading by remember(streamer?.roomId) { mutableStateOf(false) }
  var playInfo by remember(streamer?.roomId) { mutableStateOf<DouyuPlayInfo?>(null) }
  var selectedQuality by remember(streamer?.roomId) { mutableStateOf<String?>(null) }
  var selectedCdn by remember(streamer?.roomId) { mutableStateOf<String?>(null) }
  var showSettings by remember(streamer?.roomId) { mutableStateOf(false) }

  var danmakuEnabled by remember(streamer?.roomId) { mutableStateOf(true) }
  var danmakuMessages by remember(streamer?.roomId) { mutableStateOf<List<DanmakuMessage>>(emptyList()) }
  var danmakuMax by remember { mutableIntStateOf(200) }
  var videoAspectRatio by remember(streamer?.roomId) { mutableStateOf<Float?>(null) }
  var fullscreen by remember(streamer?.roomId) { mutableStateOf(false) }

  val scope = rememberCoroutineScope()

  FullscreenEffect(enabled = fullscreen)
  PlatformBackHandler(enabled = fullscreen) { fullscreen = false }

  DisposableEffect(Unit) {
    onDispose { appState.playerFullscreen = false }
  }
  LaunchedEffect(fullscreen) {
    appState.playerFullscreen = fullscreen
  }

  LaunchedEffect(streamer?.roomId) {
    val s = streamer ?: return@LaunchedEffect
    loading = true
    error = null
    url = null
    playInfo = null
    selectedQuality = null
    selectedCdn = null
    when (s.platform) {
      Platform.Douyu -> {
        runCatching { appState.repo.fetchDouyuPlayInfo(roomId = s.roomId) }
          .onSuccess { playInfo = it }
          .onFailure { error = it.message ?: "获取清晰度信息失败" }
        runCatching { appState.repo.resolveDouyuStreamUrl(roomId = s.roomId) }
          .onSuccess { url = it }
          .onFailure { error = it.message ?: "获取播放地址失败" }
      }
      Platform.Huya -> {
        runCatching { appState.repo.resolveHuyaStreamUrl(roomId = s.roomId) }
          .onSuccess { url = it }
          .onFailure { error = it.message ?: "获取虎牙播放地址失败" }
      }
      Platform.Douyin -> {
        runCatching { appState.repo.resolveDouyinStreamUrl(webRid = s.roomId) }
          .onSuccess { url = it }
          .onFailure { error = it.message ?: "获取抖音播放地址失败" }
      }
      Platform.Bilibili -> {
        runCatching { appState.repo.resolveBilibiliStreamUrl(roomId = s.roomId) }
          .onSuccess { url = it }
          .onFailure { error = it.message ?: "获取B站播放地址失败" }
      }
      else -> {
        error = "暂不支持的平台：${s.platform.title}"
      }
    }
    loading = false
  }

  LaunchedEffect(streamer?.roomId, streamer?.platform, danmakuEnabled) {
    val s = streamer ?: return@LaunchedEffect
    if (!danmakuEnabled) return@LaunchedEffect

    val flow = when (s.platform) {
      Platform.Douyu -> appState.repo.observeDouyuDanmaku(s.roomId)
      Platform.Huya -> appState.repo.observeHuyaDanmaku(s.roomId)
      Platform.Douyin -> appState.repo.observeDouyinDanmaku(s.roomId)
      Platform.Bilibili -> appState.repo.observeBilibiliDanmaku(s.roomId)
      else -> null
    } ?: return@LaunchedEffect

    danmakuMessages = emptyList()
    flow.collectLatest { msg ->
      danmakuMessages = (danmakuMessages + msg).takeLast(danmakuMax)
    }
  }

  fun reloadUrl() {
    val s = streamer ?: return
    scope.launch {
      loading = true
      error = null
      url = null
      val result = when (s.platform) {
        Platform.Douyu -> runCatching {
          appState.repo.resolveDouyuStreamUrl(
            roomId = s.roomId,
            quality = selectedQuality,
            cdn = selectedCdn,
          )
        }
        Platform.Huya -> runCatching { appState.repo.resolveHuyaStreamUrl(roomId = s.roomId) }
        Platform.Douyin -> runCatching { appState.repo.resolveDouyinStreamUrl(webRid = s.roomId) }
        Platform.Bilibili -> runCatching { appState.repo.resolveBilibiliStreamUrl(roomId = s.roomId) }
        else -> Result.failure(IllegalStateException("暂不支持的平台：${s.platform.title}"))
      }
      result
        .onSuccess { url = it }
        .onFailure { error = it.message ?: "获取播放地址失败" }
      loading = false
    }
  }

  if (showSettings && streamer != null && streamer.platform == Platform.Douyu) {
    ModalBottomSheet(onDismissRequest = { showSettings = false }) {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("播放设置", style = MaterialTheme.typography.titleMedium)

        Text("清晰度", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        RowWrap(
          items = listOf("自动" to null) + (playInfo?.variants.orEmpty().map { it.name to it.name }),
          selected = selectedQuality,
          onSelect = { selectedQuality = it },
        )

        Text("线路", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        RowWrap(
          items = listOf("自动" to null) + (playInfo?.cdns.orEmpty().map { it to it }),
          selected = selectedCdn,
          onSelect = { selectedCdn = it },
        )

        Text("弹幕", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        RowWrap(
          items = listOf("开" to "on", "关" to "off"),
          selected = if (danmakuEnabled) "on" else "off",
          onSelect = { danmakuEnabled = it == "on" },
        )

        TextButton(onClick = { showSettings = false; reloadUrl() }, modifier = Modifier.fillMaxWidth()) { Text("应用并重载") }
        SpacerLine()
      }
    }
  }

  val layoutAspect = videoAspectRatio?.takeIf { it > 0f } ?: (16f / 9f)
  val videoAspectForDanmaku = videoAspectRatio?.takeIf { it > 0f } ?: 0.75f
  val isVerticalVideo = videoAspectForDanmaku < 1f
  val isHorizontalVideo = !isVerticalVideo

  Column(
    modifier = Modifier
      .fillMaxSize()
      .then(if (fullscreen) Modifier.background(Color.Black) else Modifier)
      .then(modifier),
  ) {
    if (!fullscreen) {
      PlayerHeader(
        streamer = streamer,
        partition = appState.currentPartition,
        onBack = appState::back,
        followed = streamer?.let(appState::isFollowed) == true,
        onToggleFollow = { s -> appState.toggleFollow(s) },
        partitionSubscribed = appState.currentPartition?.let(appState::isPartitionSubscribed) == true,
        onTogglePartition = { p -> appState.togglePartition(p) },
        modifier = Modifier.fillMaxWidth(),
      )
    }

    val videoSurfaceShape = RoundedCornerShape(0.dp)
    val videoSurfaceColor = Color.Black
    val videoSurfaceModifier = if (fullscreen) {
      Modifier.fillMaxSize()
    } else {
      Modifier.fillMaxWidth().aspectRatio(layoutAspect)
    }

    Surface(shape = videoSurfaceShape, color = videoSurfaceColor, modifier = videoSurfaceModifier) {
      Box(modifier = Modifier.fillMaxSize()) {
        if (url != null) {
          StreamPlayer(
            url = url!!,
            fullscreen = fullscreen,
            onVideoAspectRatioChanged = { videoAspectRatio = it },
            onError = {
              if (it.startsWith("__retry_http__:")) {
                error = null
                url = it.removePrefix("__retry_http__:")
              } else {
                error = it
                url = null
              }
            },
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = when {
                streamer == null -> "未选择直播间"
                loading -> "正在获取播放地址…"
                error != null -> error!!
                else -> "准备播放…"
              },
              style = MaterialTheme.typography.titleMedium,
              color = if (fullscreen) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f),
            )
          }
        }

        val overlayDanmaku = danmakuEnabled && danmakuMessages.isNotEmpty() && (fullscreen || isVerticalVideo)
        if (overlayDanmaku) {
          if (fullscreen && isHorizontalVideo) {
            ScrollingDanmakuOverlay(
              resetKey = streamer?.roomId,
              messages = danmakuMessages,
              modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            )
          } else {
            DanmakuOverlay(
              messages = danmakuMessages,
              modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            )
          }
        }

        PlayerControlsOverlay(
          streamer = streamer,
          fullscreen = fullscreen,
          onToggleFullscreen = { fullscreen = !fullscreen },
          onOpenSettings = { showSettings = true },
          onReload = { reloadUrl() },
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 16.dp),
        )

      }
    }

    if (!fullscreen) {
      if (danmakuEnabled && danmakuMessages.isNotEmpty() && isHorizontalVideo) {
        HubDanmakuPanel(
          messages = danmakuMessages,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
}
}

@Composable
private fun PlayerHeader(
  streamer: Streamer?,
  partition: dtv.mobile.state.SubscribedPartition?,
  onBack: () -> Unit,
  followed: Boolean,
  onToggleFollow: (Streamer) -> Unit,
  partitionSubscribed: Boolean,
  onTogglePartition: (dtv.mobile.state.SubscribedPartition) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = Color.Black,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
  ) {
    Column(
      modifier = Modifier
        .statusBarsPadding()
        .padding(horizontal = 18.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Surface(
            modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.10f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = Color.White.copy(alpha = 0.92f),
              )
            }
          }

          val avatar = normalizeHttpUrl(streamer?.avatarUrl)
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
          ) {
            if (avatar != null) {
              NetworkImage(url = avatar, contentDescription = streamer?.name, modifier = Modifier.matchParentSize())
            } else {
              Text(
                text = streamer?.name?.take(1).orEmpty(),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleSmall,
              )
            }
          }

          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = streamer?.name.orEmpty(),
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
              Text(
                text = streamer?.platform?.title.orEmpty(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF9CA3AF),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        if (streamer != null) {
          val buttonShape = RoundedCornerShape(999.dp)
          val btnBg = if (followed) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.primary
          val btnFg = if (followed) Color.White else Color.Black
          Surface(
            modifier = Modifier
              .clip(buttonShape)
              .clickable { onToggleFollow(streamer) },
            shape = buttonShape,
            color = btnBg,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = if (followed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (followed) "已订阅" else "订阅",
                tint = btnFg,
              )
              Text(
                text = if (followed) "已订阅" else "订阅",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black),
                color = btnFg,
              )
            }
          }
        }
      }

      if (partition != null) {
        val shape = RoundedCornerShape(18.dp)
        Surface(
          modifier = Modifier
            .clip(shape)
            .clickable { onTogglePartition(partition) },
          shape = shape,
          color = Color.White.copy(alpha = 0.10f),
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
          tonalElevation = 0.dp,
          shadowElevation = 0.dp,
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = if (partitionSubscribed) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = if (partitionSubscribed) "已订阅分区" else "订阅分区",
              tint = if (partitionSubscribed) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
            )
            Text(
              text = if (partitionSubscribed) "已订阅分区：${partition.name}" else "订阅分区：${partition.name}",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
              color = Color.White.copy(alpha = 0.92f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun PlayerControlsOverlay(
  streamer: Streamer?,
  fullscreen: Boolean,
  onToggleFullscreen: () -> Unit,
  onOpenSettings: () -> Unit,
  onReload: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    ControlFab(icon = Icons.Default.FullscreenExit.takeIf { fullscreen } ?: Icons.Default.Fullscreen, onClick = onToggleFullscreen)
    if (streamer?.platform == Platform.Douyu) {
      ControlFab(icon = Icons.Default.Settings, onClick = onOpenSettings)
    }
    ControlFab(icon = Icons.Default.Refresh, onClick = onReload)
  }
}

@Composable
private fun ControlFab(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick),
    shape = CircleShape,
    color = Color.White.copy(alpha = 0.10f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White.copy(alpha = 0.92f),
      )
    }
  }
}

@Composable
private fun DanmakuOverlay(
  messages: List<DanmakuMessage>,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Bottom,
  ) {
    messages.takeLast(14).forEach { msg ->
      DanmakuBubble(
        user = msg.user,
        content = msg.content,
        modifier = Modifier.fillMaxWidth(0.92f),
        maxLines = 1,
        compact = true,
      )
      SpacerLine(6.dp)
    }
  }
}

@Composable
private fun DanmakuBubble(
  user: String,
  content: String,
  modifier: Modifier = Modifier,
  maxLines: Int = 1,
  compact: Boolean = false,
) {
  val displayUser = user.trim().ifBlank { "匿名" }
  val displayContent = content.trim()

  val text = buildAnnotatedString {
    withStyle(
      SpanStyle(
        color = Color(0xFFFFE082),
        fontWeight = FontWeight.SemiBold,
      ),
    ) {
      append(displayUser)
    }
    append("  ")
    append(displayContent)
  }

  val hPad = if (compact) 10.dp else 12.dp
  val vPad = if (compact) 6.dp else 8.dp

  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = Color.Black.copy(alpha = 0.26f),
    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
    tonalElevation = 0.dp,
    shadowElevation = 1.dp,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = Color.White.copy(alpha = 0.95f),
      modifier = Modifier.padding(horizontal = hPad, vertical = vPad),
      maxLines = maxLines,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun HubDanmakuPanel(
  messages: List<DanmakuMessage>,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size) {
    val size = messages.size
    if (size > 0) {
      listState.scrollToItem(index = size - 1)
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxWidth()
      .height(260.dp),
    state = listState,
    contentPadding = PaddingValues(0.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    items(messages, key = { it.roomId + ":" + it.user + ":" + it.content }) { msg ->
      HubDanmakuRow(
        user = msg.user.trim().ifBlank { "匿名" },
        content = msg.content.trim(),
      )
    }
  }
}

@Composable
private fun HubDanmakuRow(
  user: String,
  content: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = Color.White.copy(alpha = 0.10f),
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
    ) {
      Text(
        text = user,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
        color = Color(0xFF9CA3AF),
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Text(
      text = content,
      style = MaterialTheme.typography.bodySmall,
      color = Color.White.copy(alpha = 0.92f),
      modifier = Modifier
        .weight(1f)
        .padding(top = 1.dp),
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun ScrollingDanmakuOverlay(
  resetKey: Any?,
  messages: List<DanmakuMessage>,
  modifier: Modifier = Modifier,
) {
  data class Active(
    val id: Long,
    val user: String,
    val content: String,
    val track: Int,
  )

  val active = remember(resetKey) { mutableStateListOf<Active>() }
  var lastCount by remember(resetKey) { mutableIntStateOf(0) }
  var nextTrack by remember(resetKey) { mutableIntStateOf(0) }
  val trackCount = 8
  val maxActive = 30

  LaunchedEffect(messages.size) {
    if (messages.size <= lastCount) {
      lastCount = messages.size
      return@LaunchedEffect
    }
    val newItems = messages.subList(lastCount, messages.size)
    newItems.forEach { msg ->
      val user = msg.user.trim().ifBlank { "匿名" }
      val content = msg.content.trim()
      if (content.isNotEmpty()) {
        if (active.size >= maxActive) active.removeAt(0)
        active.add(
          Active(
            id = System.nanoTime(),
            user = user,
            content = content,
            track = nextTrack,
          ),
        )
        nextTrack = (nextTrack + 1) % trackCount
      }
    }
    lastCount = messages.size
  }

  BoxWithConstraints(modifier = modifier) {
    val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
    val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
    val topPaddingPx = heightPx * 0.18f
    val trackHeightPx = (heightPx * 0.52f) / trackCount

    active.forEach { item ->
      key(item.id) {
        ScrollingDanmakuItem(
          user = item.user,
          content = item.content,
          startX = widthPx,
          endX = -widthPx,
          y = topPaddingPx + trackHeightPx * item.track,
          onFinished = { active.remove(item) },
        )
      }
    }
  }
}

@Composable
private fun ScrollingDanmakuItem(
  user: String,
  content: String,
  startX: Float,
  endX: Float,
  y: Float,
  onFinished: () -> Unit,
) {
  val x = remember { Animatable(startX) }
  LaunchedEffect(user, content, startX, endX) {
    x.snapTo(startX)
    x.animateTo(
      targetValue = endX,
      animationSpec = tween(durationMillis = 9000, easing = LinearEasing),
    )
    onFinished()
  }

  Box(
    modifier = Modifier.offset {
      IntOffset(x.value.roundToInt(), y.roundToInt())
    },
  ) {
    DanmakuBubble(
      user = user,
      content = content,
      maxLines = 1,
      compact = true,
    )
  }
}

@Composable
private fun SpacerLine(height: androidx.compose.ui.unit.Dp = 10.dp) {
  androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(height))
}

@Composable
private fun RowWrap(
  items: List<Pair<String, String?>>,
  selected: String?,
  onSelect: (String?) -> Unit,
) {
  LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(items, key = { it.first }) { (label, value) ->
      val isSelected = value == selected || (value == null && selected == null)
      FilterChip(
        selected = isSelected,
        onClick = { onSelect(value) },
        label = { Text(label) },
      )
    }
  }
}
