package dtv.mobile.platform.douyin

import java.io.ByteArrayOutputStream

internal object DouyinProtoLite {
  private const val WIRE_VARINT = 0
  private const val WIRE_LEN = 2

  internal data class PushFrameLite(
    val logId: Long,
    val payloadType: String,
    val payloadEncoding: String?,
    val payload: ByteArray,
  )

  internal data class ResponseLite(
    val needAck: Boolean,
    val internalExt: String,
    val messages: List<MessageLite>,
  )

  internal data class MessageLite(
    val method: String,
    val payload: ByteArray,
  )

  internal data class ChatLite(
    val nick: String,
    val content: String,
    val userLevel: Int,
    val fansClubLevel: Int,
  )

  internal class Reader(private val data: ByteArray) {
    private var pos = 0

    fun isEof(): Boolean = pos >= data.size

    private fun readByte(): Int {
      if (pos >= data.size) error("protobuf eof")
      return data[pos++].toInt() and 0xFF
    }

    fun readVarint(): Long {
      var shift = 0
      var out = 0L
      while (shift < 64) {
        val b = readByte()
        out = out or ((b and 0x7F).toLong() shl shift)
        if ((b and 0x80) == 0) return out
        shift += 7
      }
      error("bad varint")
    }

    fun readTag(): Pair<Int, Int>? {
      if (isEof()) return null
      val key = readVarint().toInt()
      val field = key ushr 3
      val wire = key and 0x07
      return field to wire
    }

    fun readBytes(): ByteArray {
      val len = readVarint().toInt()
      if (len < 0 || pos + len > data.size) error("bad len=$len")
      val out = data.copyOfRange(pos, pos + len)
      pos += len
      return out
    }

    fun readString(): String = readBytes().toString(Charsets.UTF_8)

    fun skip(wire: Int) {
      when (wire) {
        WIRE_VARINT -> readVarint()
        WIRE_LEN -> {
          val len = readVarint().toInt()
          if (len < 0 || pos + len > data.size) error("bad skip len=$len")
          pos += len
        }
        else -> error("unsupported wire=$wire")
      }
    }
  }

  private fun writeVarint(out: ByteArrayOutputStream, value: Long) {
    var v = value
    while (true) {
      val b = (v and 0x7F).toInt()
      v = v ushr 7
      if (v == 0L) {
        out.write(b)
        return
      }
      out.write(b or 0x80)
    }
  }

  private fun writeKey(out: ByteArrayOutputStream, fieldNumber: Int, wireType: Int) {
    writeVarint(out, ((fieldNumber shl 3) or wireType).toLong())
  }

  private fun writeLenField(out: ByteArrayOutputStream, fieldNumber: Int, bytes: ByteArray) {
    writeKey(out, fieldNumber, WIRE_LEN)
    writeVarint(out, bytes.size.toLong())
    out.write(bytes)
  }

  private fun writeStringField(out: ByteArrayOutputStream, fieldNumber: Int, value: String) {
    writeLenField(out, fieldNumber, value.toByteArray(Charsets.UTF_8))
  }

  private fun writeVarintField(out: ByteArrayOutputStream, fieldNumber: Int, value: Long) {
    writeKey(out, fieldNumber, WIRE_VARINT)
    writeVarint(out, value)
  }

  internal fun encodePushFrame(payloadType: String, logId: Long, payload: ByteArray): ByteArray {
    val out = ByteArrayOutputStream(64 + payload.size)
    // Only encode the few fields we need:
    // logId = 2, payloadType = 7, payload = 8
    writeVarintField(out, 2, logId)
    writeStringField(out, 7, payloadType)
    if (payload.isNotEmpty()) {
      writeLenField(out, 8, payload)
    }
    return out.toByteArray()
  }

  internal fun decodePushFrame(data: ByteArray): PushFrameLite? {
    val r = Reader(data)
    var logId = 0L
    var payloadType = ""
    var payloadEncoding: String? = null
    var payload = ByteArray(0)

    while (!r.isEof()) {
      val (field, wire) = r.readTag() ?: break
      when (field) {
        2 -> if (wire == WIRE_VARINT) logId = r.readVarint() else r.skip(wire)
        6 -> if (wire == WIRE_LEN) payloadEncoding = r.readString() else r.skip(wire)
        7 -> if (wire == WIRE_LEN) payloadType = r.readString() else r.skip(wire)
        8 -> if (wire == WIRE_LEN) payload = r.readBytes() else r.skip(wire)
        else -> r.skip(wire)
      }
    }

    if (payloadType.isBlank()) return null
    return PushFrameLite(logId = logId, payloadType = payloadType, payloadEncoding = payloadEncoding, payload = payload)
  }

  internal fun decodeResponse(data: ByteArray): ResponseLite? {
    val r = Reader(data)
    val messages = ArrayList<MessageLite>(8)
    var needAck = false
    var internalExt = ""

    while (!r.isEof()) {
      val (field, wire) = r.readTag() ?: break
      when (field) {
        1 -> if (wire == WIRE_LEN) decodeMessage(r.readBytes())?.let(messages::add) else r.skip(wire)
        5 -> if (wire == WIRE_LEN) internalExt = r.readString() else r.skip(wire)
        9 -> if (wire == WIRE_VARINT) needAck = r.readVarint() != 0L else r.skip(wire)
        else -> r.skip(wire)
      }
    }
    return ResponseLite(needAck = needAck, internalExt = internalExt, messages = messages)
  }

  private fun decodeMessage(data: ByteArray): MessageLite? {
    val r = Reader(data)
    var method = ""
    var payload = ByteArray(0)
    while (!r.isEof()) {
      val (field, wire) = r.readTag() ?: break
      when (field) {
        1 -> if (wire == WIRE_LEN) method = r.readString() else r.skip(wire)
        2 -> if (wire == WIRE_LEN) payload = r.readBytes() else r.skip(wire)
        else -> r.skip(wire)
      }
    }
    if (method.isBlank() || payload.isEmpty()) return null
    return MessageLite(method = method, payload = payload)
  }

  internal fun decodeChatMessage(data: ByteArray): ChatLite? {
    val r = Reader(data)
    var content = ""
    var nick = ""
    var userLevel = 0
    var fansClubLevel = 0
    while (!r.isEof()) {
      val (field, wire) = r.readTag() ?: break
      when (field) {
        2 -> if (wire == WIRE_LEN) {
          val user = decodeUser(r.readBytes())
          nick = user.nick
          userLevel = user.userLevel
          fansClubLevel = user.fansClubLevel
        } else {
          r.skip(wire)
        }
        3 -> if (wire == WIRE_LEN) content = r.readString() else r.skip(wire)
        else -> r.skip(wire)
      }
    }
    if (content.isBlank()) return null
    return ChatLite(
      nick = nick.ifBlank { "匿名" },
      content = content,
      userLevel = userLevel,
      fansClubLevel = fansClubLevel,
    )
  }

  private data class UserLite(
    val nick: String,
    val userLevel: Int,
    val fansClubLevel: Int,
  )

  private fun decodeUser(data: ByteArray): UserLite {
    val r = Reader(data)
    var nick = ""
    var userLevel = 0
    var fansClubLevel = 0
    while (!r.isEof()) {
      val (field, wire) = r.readTag() ?: break
      when (field) {
        3 -> if (wire == WIRE_LEN) nick = r.readString() else r.skip(wire) // nickName
        23 -> if (wire == WIRE_LEN) userLevel = decodePayGradeLevel(r.readBytes()) else r.skip(wire) // payGrade
        24 -> if (wire == WIRE_LEN) fansClubLevel = decodeFansClubLevel(r.readBytes()) else r.skip(wire) // fansClub
        else -> r.skip(wire)
      }
    }
    return UserLite(nick = nick, userLevel = userLevel, fansClubLevel = fansClubLevel)
  }

  private fun decodePayGradeLevel(data: ByteArray): Int {
    val r = Reader(data)
    while (!r.isEof()) {
      val (field, wire) = r.readTag() ?: break
      when (field) {
        6 -> return if (wire == WIRE_VARINT) r.readVarint().toInt() else { r.skip(wire); 0 } // level
        else -> r.skip(wire)
      }
    }
    return 0
  }

  private fun decodeFansClubLevel(data: ByteArray): Int {
    val r = Reader(data)
    while (!r.isEof()) {
      val (field, wire) = r.readTag() ?: break
      when (field) {
        1 -> if (wire == WIRE_LEN) return decodeFansClubDataLevel(r.readBytes()) else r.skip(wire) // data
        else -> r.skip(wire)
      }
    }
    return 0
  }

  private fun decodeFansClubDataLevel(data: ByteArray): Int {
    val r = Reader(data)
    while (!r.isEof()) {
      val (field, wire) = r.readTag() ?: break
      when (field) {
        2 -> return if (wire == WIRE_VARINT) r.readVarint().toInt() else { r.skip(wire); 0 } // level
        else -> r.skip(wire)
      }
    }
    return 0
  }
}
