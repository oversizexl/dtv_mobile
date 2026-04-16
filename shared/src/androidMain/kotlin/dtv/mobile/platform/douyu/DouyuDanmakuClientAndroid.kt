package dtv.mobile.platform.douyu

import dtv.mobile.repo.DanmakuMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

class DouyuDanmakuClientAndroid(
  private val okHttp: OkHttpClient = OkHttpClient(),
) {
  fun observe(roomId: String): Flow<DanmakuMessage> = callbackFlow {
    fun encode(msg: String): ByteString {
      val msgBytes = msg.toByteArray(Charsets.UTF_8)
      val packetLen = msgBytes.size + 9
      val buf = ByteBuffer.allocate(4 + 4 + 2 + 1 + 1 + msgBytes.size + 1)
      buf.order(ByteOrder.LITTLE_ENDIAN)
      buf.putInt(packetLen)
      buf.putInt(packetLen)
      buf.putShort(689.toShort())
      buf.put(0)
      buf.put(0)
      buf.put(msgBytes)
      buf.put(0)
      return ByteString.of(*buf.array())
    }

    fun decodeDouyuEscapes(value: String): String {
      return value.replace("@S", "/").replace("@A", "@")
    }

    fun parseMap(content: String): Map<String, String> {
      val map = HashMap<String, String>(16)
      content.split('/').forEach { item ->
        if (item.isBlank()) return@forEach
        val idx = item.indexOf("@=")
        if (idx <= 0) return@forEach
        val k = item.substring(0, idx)
        val v = item.substring(idx + 2)
        map[k] = decodeDouyuEscapes(v)
      }
      return map
    }

    // Reconnect loop with backoff (matches desktop behavior)
    launch {
      var backoff = 1000L
      while (isActive) {
        val done = CompletableDeferred<Unit>()
        var socket: WebSocket? = null
        var heartbeatJob: kotlinx.coroutines.Job? = null
        try {
          val req = Request.Builder()
            .url("wss://danmuproxy.douyu.com:8506/")
            .addHeader("Sec-WebSocket-Protocol", "binary")
            .build()

          val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
              socket = webSocket
              webSocket.send(encode("type@=loginreq/roomid@=$roomId/"))
              webSocket.send(encode("type@=joingroup/rid@=$roomId/gid@=1/"))

              heartbeatJob = launch {
                while (isActive) {
                  delay(45_000)
                  socket?.send(encode("type@=mrkl/")) ?: break
                }
              }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
              val data = bytes.toByteArray()
              if (data.size < 13) return
              val payload = data.copyOfRange(12, data.size - 1)
              val text = runCatching { payload.toString(Charsets.UTF_8) }.getOrNull() ?: return
              val m = parseMap(text)

              if (m["type"] == "chatmsg") {
                val user = m["nn"].orEmpty().ifBlank { "unknown" }
                val content = m["txt"].orEmpty()
                val userLevel = m["level"]?.toIntOrNull() ?: 0
                val fans = m["bl"]?.toIntOrNull() ?: 0
                val color = m["col"]
                trySend(
                  DanmakuMessage(
                    roomId = roomId,
                    user = user,
                    content = content,
                    userLevel = userLevel,
                    fansClubLevel = fans,
                    color = color,
                  ),
                )
              }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
              webSocket.close(code, reason)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
              done.complete(Unit)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
              done.complete(Unit)
            }
          }

          socket = okHttp.newWebSocket(req, listener)
          done.await()
        } catch (ce: CancellationException) {
          throw ce
        } catch (t: Throwable) {
          // ignore
        } finally {
          heartbeatJob?.cancel()
          socket?.cancel()
        }
        delay(backoff)
        backoff = min(backoff * 2, 30_000L)
      }
    }

    awaitClose {
      // Child coroutines are cancelled automatically when the flow collector is cancelled.
    }
  }
}
