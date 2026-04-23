package dtv.mobile.util

import java.util.Locale
import kotlin.math.floor

/**
 * For raw viewer counts like "1739892", format to "173.9万" (truncate to 1 decimal).
 * If the server already returns a "万" string, keep it as-is.
 */
fun formatViewerCountWanIfNeeded(raw: String): String {
  val t = raw.trim()
  if (t.isBlank()) return t
  if (t.contains('万')) return t
  if (!t.all { it.isDigit() }) return t

  val value = t.toLongOrNull() ?: return t
  if (value < 10_000L) return t

  val wan = value / 10_000.0
  val truncated = floor(wan * 10.0) / 10.0
  return String.format(Locale.US, "%.1f万", truncated)
}

