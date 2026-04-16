package dtv.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
actual fun NetworkImage(
  url: String?,
  contentDescription: String?,
  modifier: Modifier,
  contentScale: ContentScale,
) {
  AsyncImage(
    model = url,
    contentDescription = contentDescription,
    modifier = modifier,
    contentScale = contentScale,
  )
}

