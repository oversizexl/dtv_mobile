package dtv.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dtv.mobile.model.Streamer
import dtv.mobile.theme.DtvColors
import dtv.mobile.util.normalizeHttpUrl

@Composable
fun StreamerCard(
  streamer: Streamer,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(16.dp)
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(shape)
      .clickable(onClick = onClick),
    color = MaterialTheme.colorScheme.surface,
    shape = shape,
    tonalElevation = 0.dp,
    shadowElevation = 2.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
  ) {
    Column {
      val cover = normalizeHttpUrl(streamer.coverUrl) ?: normalizeHttpUrl(streamer.avatarUrl)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .aspectRatio(16f / 9f)
          .background(MaterialTheme.colorScheme.secondary),
      ) {
        if (cover != null) {
          NetworkImage(
            url = cover,
            contentDescription = streamer.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
          )
        } else {
          Text(
            text = streamer.name.take(1),
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f),
            style = MaterialTheme.typography.titleLarge,
          )
        }

        val gradient = Brush.verticalGradient(
          colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
        )
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .align(Alignment.BottomCenter)
            .background(gradient),
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .align(Alignment.TopStart),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val avatar = normalizeHttpUrl(streamer.avatarUrl)
            Box(
              modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.25f)),
              contentAlignment = Alignment.Center,
            ) {
              if (avatar != null) {
                NetworkImage(url = avatar, contentDescription = streamer.name, modifier = Modifier.matchParentSize())
              } else {
                Text(streamer.name.take(1), color = Color.White, style = MaterialTheme.typography.labelLarge)
              }
            }
            Text(
              text = streamer.name,
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            if (streamer.isLive) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(DtvColors.StatusLive),
              )
            }
          }

          if (streamer.viewerText.isNotBlank()) {
            Text(
              text = streamer.viewerText,
              style = MaterialTheme.typography.labelMedium,
              color = Color.White.copy(alpha = 0.92f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }

      Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(
          text = streamer.title,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = streamer.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          if (streamer.viewerText.isNotBlank()) {
            Text(
              text = streamer.viewerText,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}
