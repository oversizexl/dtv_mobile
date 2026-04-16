package dtv.mobile.platform.douyu

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.javascript.Context as RhinoContext
import org.mozilla.javascript.Scriptable

class DouyuJsSignerAndroid(
  private val appContext: Context,
) {
  private val cryptoJs: String by lazy {
    appContext.assets.open("douyu/cryptojs.min.js").bufferedReader(Charsets.UTF_8).use { it.readText() }
  }

  suspend fun signParams(
    homeH5EncScript: String,
    roomId: String,
    did: String,
    tsSeconds: Long,
  ): String = withContext(Dispatchers.Default) {
    val ctx = RhinoContext.enter()
    try {
      // Android 上必须禁用优化，否则容易触发 bytecode 生成限制
      ctx.optimizationLevel = -1
      val scope: Scriptable = ctx.initStandardObjects()
      ctx.evaluateString(scope, cryptoJs, "cryptojs.min.js", 1, null)
      ctx.evaluateString(scope, homeH5EncScript, "homeH5Enc.js", 1, null)

      val ridJs = jsString(roomId)
      val didJs = jsString(did)
      val call = "ub98484234($ridJs,$didJs,$tsSeconds);"
      val result = ctx.evaluateString(scope, call, "sign-call", 1, null)
      RhinoContext.toString(result)
    } finally {
      RhinoContext.exit()
    }
  }
}

private fun jsString(input: String): String {
  val escaped = buildString {
    input.forEach { ch ->
      when (ch) {
        '\\' -> append("\\\\")
        '"' -> append("\\\"")
        '\n' -> append("\\n")
        '\r' -> append("\\r")
        '\t' -> append("\\t")
        else -> append(ch)
      }
    }
  }
  return "\"$escaped\""
}

