package dtv.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dtv.mobile.state.AppState
import dtv.mobile.ui.components.HomeStreamerCard
import dtv.mobile.ui.components.NetworkImage
import dtv.mobile.util.normalizeHttpUrl

@Composable
fun HomeScreen(
  appState: AppState,
  modifier: Modifier = Modifier,
) {
  val items = appState.followedStreamers

  if (items.isEmpty()) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      Text(
        text = "暂无订阅主播",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
      )
    }
    return
  }

  LazyVerticalGrid(
    modifier = modifier.fillMaxSize(),
    columns = GridCells.Fixed(2),
    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 92.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    verticalArrangement = Arrangement.spacedBy(28.dp),
  ) {
    item(span = { GridItemSpan(2) }) {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        lazyItems(items, key = { "${it.platform}-${it.roomId}" }) { streamer ->
          HomeAvatarItem(
            nickname = streamer.name,
            avatarUrl = streamer.avatarUrl,
            onClick = { appState.openPlayer(streamer) },
          )
        }
      }
    }

    items(items, key = { "${it.platform}-${it.roomId}" }) { streamer ->
      HomeStreamerCard(
        streamer = streamer,
        followed = true,
        onClick = { appState.openPlayer(streamer) },
        onToggleFollow = { appState.toggleFollow(streamer) },
      )
    }

    item(span = { GridItemSpan(2) }) {
      EndOfFeedCard(modifier = Modifier.padding(top = 8.dp))
    }
  }
}

@Composable
private fun HomeAvatarItem(
  nickname: String,
  avatarUrl: String?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val accent = MaterialTheme.colorScheme.primary
  val bg = MaterialTheme.colorScheme.background
  val avatar = normalizeHttpUrl(avatarUrl)

  Column(
    modifier = modifier
      .width(72.dp)
      .clickable(onClick = onClick),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    Box(
      modifier = Modifier
        .size(54.dp)
        .clip(CircleShape)
        .border(width = 2.dp, color = accent, shape = CircleShape)
        .padding(3.dp)
        .clip(CircleShape),
    ) {
      if (avatar != null) {
        NetworkImage(url = avatar, contentDescription = nickname, modifier = Modifier.matchParentSize())
      } else {
        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
          Text(text = nickname.take(1), style = MaterialTheme.typography.titleMedium)
        }
      }

      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .size(14.dp)
          .clip(CircleShape)
          .background(accent)
          .border(width = 2.dp, color = bg, shape = CircleShape),
      )
    }

    Text(
      text = nickname,
      fontSize = 9.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = (-0.2).sp,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
private fun EndOfFeedCard(
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(48.dp)
  val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .height(140.dp)
      .clip(shape)
      .drawBehind {
        val strokeWidth = 2.dp.toPx()
        drawRoundRect(
          color = borderColor,
          style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14.dp.toPx(), 10.dp.toPx()), 0f),
          ),
          cornerRadius = CornerRadius(x = 48.dp.toPx(), y = 48.dp.toPx()),
        )
      },
    shape = shape,
    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.06f),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
      Text(
        text = "THE END OF FEED",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Black,
          fontStyle = FontStyle.Italic,
          letterSpacing = 0.35.sp,
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
      )
    }
  }
}
