package dtv.mobile.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dtv.mobile.model.Streamer
import dtv.mobile.ui.components.NetworkImage
import dtv.mobile.util.normalizeHttpUrl

/**
 * 音频播放模式覆盖层 — 仿网易云音乐 CD 旋转样式。
 *
 * @param streamer 当前主播信息
 * @param isPlaying 是否正在播放
 * @param onTogglePlayPause 切换播放/暂停
 * @param onBackToVideo 返回视频模式
 * @param onOpenSleepTimer 打开定时关闭对话框
 * @param sleepTimerText 定时器倒计时文字（如 "12:34"），null 表示未启用
 */
@Composable
fun AudioPlayerOverlay(
  streamer: Streamer,
  isPlaying: Boolean,
  onTogglePlayPause: () -> Unit,
  onBackToVideo: () -> Unit,
  onOpenSleepTimer: () -> Unit,
  sleepTimerText: String? = null,
  modifier: Modifier = Modifier,
) {
  val bgColor = Color(0xFF0D0D0D)
  val accentColor = MaterialTheme.colorScheme.primary

  // CD 旋转动画
  val infiniteTransition = rememberInfiniteTransition(label = "cdRotation")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 20000, easing = LinearEasing),
    ),
    label = "cdRotationAngle",
  )

  val coverUrl = normalizeHttpUrl(streamer.avatarUrl) ?: normalizeHttpUrl(streamer.coverUrl)

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(bgColor),
  ) {
    // 背景渐变装饰
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(
            accentColor.copy(alpha = 0.12f),
            Color.Transparent,
          ),
          center = Offset(size.width * 0.5f, size.height * 0.35f),
          radius = size.width * 0.8f,
        ),
        radius = size.width * 0.8f,
        center = Offset(size.width * 0.5f, size.height * 0.35f),
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      // CD 唱片区域
      Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center,
      ) {
        // 外圈 CD 纹理
        Box(
          modifier = Modifier
            .fillMaxSize()
            .rotate(if (isPlaying) rotation else 0f),
        ) {
          // CD 底盘
          Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2

            // 外圈
            drawCircle(
              color = Color(0xFF1A1A1A),
              radius = radius,
              center = center,
            )
            // CD 纹理线
            for (i in 1..8) {
              val r = radius * (0.4f + i * 0.07f)
              drawCircle(
                color = Color.White.copy(alpha = 0.04f),
                radius = r,
                center = center,
                style = Stroke(width = 1f),
              )
            }
            // 内圈孔
            drawCircle(
              color = bgColor,
              radius = radius * 0.15f,
              center = center,
            )
            // 内圈装饰环
            drawCircle(
              color = Color.White.copy(alpha = 0.15f),
              radius = radius * 0.18f,
              center = center,
              style = Stroke(width = 2f),
            )
          }

          // 封面图
          Box(
            modifier = Modifier
              .align(Alignment.Center)
              .size(160.dp)
              .clip(CircleShape)
              .border(3.dp, Color.White.copy(alpha = 0.10f), CircleShape),
          ) {
            if (coverUrl != null) {
              NetworkImage(
                url = coverUrl,
                contentDescription = streamer.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
              )
            } else {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.MusicNote,
                  contentDescription = null,
                  modifier = Modifier.size(60.dp),
                  tint = Color.White.copy(alpha = 0.4f),
                )
              }
            }
          }
        }

        // 中心小圆点（CD 轴心）
        Box(
          modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.dp, Color.White.copy(alpha = 0.20f), CircleShape),
        )
      }

      Spacer(modifier = Modifier.height(36.dp))

      // 主播信息
      Text(
        text = streamer.name,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = Color.White.copy(alpha = 0.95f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = streamer.title.trim().ifBlank { "直播中" },
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.60f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
      )

      // 定时器状态
      if (sleepTimerText != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color.White.copy(alpha = 0.08f),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = null,
              modifier = Modifier.size(14.dp),
              tint = accentColor.copy(alpha = 0.85f),
            )
            Text(
              text = sleepTimerText,
              style = MaterialTheme.typography.labelSmall,
              color = accentColor.copy(alpha = 0.85f),
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(40.dp))

      // 控制按钮
      Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // 返回视频模式
        AudioControlButton(
          icon = Icons.Default.VideoSettings,
          label = "视频",
          onClick = onBackToVideo,
        )

        // 播放/暂停
        Surface(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable(onClick = onTogglePlayPause),
          shape = CircleShape,
          color = accentColor,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isPlaying) "暂停" else "播放",
              modifier = Modifier.size(36.dp),
              tint = Color.White,
            )
          }
        }

        // 定时关闭
        AudioControlButton(
          icon = Icons.Default.Timer,
          label = "定时",
          onClick = onOpenSleepTimer,
        )
      }
    }
  }
}

@Composable
private fun AudioControlButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Surface(
      modifier = Modifier.size(44.dp),
      shape = CircleShape,
      color = Color.White.copy(alpha = 0.08f),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = label,
          modifier = Modifier.size(22.dp),
          tint = Color.White.copy(alpha = 0.85f),
        )
      }
    }
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = Color.White.copy(alpha = 0.60f),
    )
  }
}
