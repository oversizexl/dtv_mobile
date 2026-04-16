package dtv.mobile.backend

/**
 * Kotlin-native replacement for the desktop Tauri(Rust) command layer.
 *
 * For now this is an interface + stubs so we can migrate UI first, then port Rust implementations
 * feature-by-feature (stream URL, live lists, danmaku listeners, proxy, etc.).
 */
interface DtvBackend {
  suspend fun getStreamUrl(roomId: String): String

  suspend fun getStreamUrlWithQuality(roomId: String, quality: String, line: String? = null): String

  suspend fun searchDouyuAnchors(keyword: String): String
}

