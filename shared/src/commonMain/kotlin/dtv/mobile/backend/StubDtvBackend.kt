package dtv.mobile.backend

class StubDtvBackend : DtvBackend {
  override suspend fun getStreamUrl(roomId: String): String = "https://example.invalid/stream/$roomId.m3u8"

  override suspend fun getStreamUrlWithQuality(roomId: String, quality: String, line: String?): String {
    return "https://example.invalid/stream/$roomId/$quality.m3u8"
  }

  override suspend fun searchDouyuAnchors(keyword: String): String {
    return """{"keyword":"$keyword","items":[]}"""
  }
}

