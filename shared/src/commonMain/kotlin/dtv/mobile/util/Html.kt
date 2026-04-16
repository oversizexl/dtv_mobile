package dtv.mobile.util

fun decodeHtmlEntities(input: String): String {
  // Enough for Douyu `rtmp_live` which often contains `&amp;`
  return input
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
}

