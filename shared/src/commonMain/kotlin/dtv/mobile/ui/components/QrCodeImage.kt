package dtv.mobile.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun QrCodeImage(
  data: String,
  modifier: Modifier = Modifier,
)

