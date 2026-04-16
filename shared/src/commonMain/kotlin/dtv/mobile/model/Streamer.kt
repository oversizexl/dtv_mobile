package dtv.mobile.model

data class Streamer(
  val platform: Platform,
  val roomId: String,
  val name: String,
  val title: String,
  val viewerText: String,
  val avatarUrl: String? = null,
  val coverUrl: String? = null,
  val isLive: Boolean = true,
)
