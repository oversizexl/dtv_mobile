package dtv.mobile.ui.system

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformBackHandler(
  enabled: Boolean,
  onBack: () -> Unit,
)

