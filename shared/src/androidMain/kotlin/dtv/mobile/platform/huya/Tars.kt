package dtv.mobile.platform.huya

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal TARS/JCE codec, aligned with `huya-danmaku-kotlin`.
 *
 * Covers only what Huya danmaku needs:
 * - int32/int64, string, list<string>, bytes(simple_list), struct decoding
 */
internal object Tars {
  private const val TYPE_BYTE = 0
  private const val TYPE_SHORT = 1
  private const val TYPE_INT = 2
  private const val TYPE_LONG = 3
  private const val TYPE_STRING1 = 6
  private const val TYPE_STRING4 = 7
  private const val TYPE_LIST = 9
  private const val TYPE_STRUCT_BEGIN = 10
  private const val TYPE_STRUCT_END = 11
  private const val TYPE_ZERO_TAG = 12
  private const val TYPE_SIMPLE_LIST = 13

  data class Head(val tag: Int, val type: Int)

  class Output {
    private val out = ByteArrayOutputStream()

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun writeHead(type: Int, tag: Int) {
      require(tag >= 0) { "tag must be >= 0" }
      if (tag < 15) {
        out.write(((tag shl 4) or (type and 0x0f)) and 0xff)
      } else {
        out.write(((15 shl 4) or (type and 0x0f)) and 0xff)
        out.write(tag and 0xff)
      }
    }

    private fun writeI16(v: Short) {
      val buf = ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN)
      buf.putShort(v)
      out.write(buf.array())
    }

    private fun writeI32Raw(v: Int) {
      val buf = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN)
      buf.putInt(v)
      out.write(buf.array())
    }

    private fun writeI64Raw(v: Long) {
      val buf = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
      buf.putLong(v)
      out.write(buf.array())
    }

    fun writeInt32(tag: Int, value: Int): Output {
      when {
        value == 0 -> writeHead(TYPE_ZERO_TAG, tag)
        value in -128..127 -> {
          writeHead(TYPE_BYTE, tag)
          out.write((value and 0xff))
        }
        value in -32768..32767 -> {
          writeHead(TYPE_SHORT, tag)
          writeI16(value.toShort())
        }
        else -> {
          writeHead(TYPE_INT, tag)
          writeI32Raw(value)
        }
      }
      return this
    }

    fun writeInt64(tag: Int, value: Long): Output {
      when {
        value == 0L -> writeHead(TYPE_ZERO_TAG, tag)
        value in -128L..127L -> {
          writeHead(TYPE_BYTE, tag)
          out.write((value.toInt() and 0xff))
        }
        value in -32768L..32767L -> {
          writeHead(TYPE_SHORT, tag)
          writeI16(value.toShort())
        }
        value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() -> {
          writeHead(TYPE_INT, tag)
          writeI32Raw(value.toInt())
        }
        else -> {
          writeHead(TYPE_LONG, tag)
          writeI64Raw(value)
        }
      }
      return this
    }

    fun writeString(tag: Int, value: String): Output {
      val bytes = value.toByteArray(Charsets.UTF_8)
      if (bytes.size < 255) {
        writeHead(TYPE_STRING1, tag)
        out.write(bytes.size and 0xff)
        out.write(bytes)
      } else {
        writeHead(TYPE_STRING4, tag)
        writeI32Raw(bytes.size)
        out.write(bytes)
      }
      return this
    }

    fun writeBytes(tag: Int, value: ByteArray): Output {
      writeHead(TYPE_SIMPLE_LIST, tag)
      writeHead(TYPE_BYTE, 0) // element head: BYTE with tag 0
      writeInt32(0, value.size)
      out.write(value)
      return this
    }

    fun writeStringList(tag: Int, value: List<String>): Output {
      writeHead(TYPE_LIST, tag)
      writeInt32(0, value.size)
      for (s in value) {
        writeString(0, s)
      }
      return this
    }
  }

  class Input(private val data: ByteArray) {
    private var pos: Int = 0

    private fun remaining(): Int = data.size - pos
    private fun readU8(): Int = data[pos++].toInt() and 0xff

    private fun readI16Raw(): Short {
      val v = ByteBuffer.wrap(data, pos, 2).order(ByteOrder.BIG_ENDIAN).short
      pos += 2
      return v
    }

    private fun readI32Raw(): Int {
      val v = ByteBuffer.wrap(data, pos, 4).order(ByteOrder.BIG_ENDIAN).int
      pos += 4
      return v
    }

    private fun readI64Raw(): Long {
      val v = ByteBuffer.wrap(data, pos, 8).order(ByteOrder.BIG_ENDIAN).long
      pos += 8
      return v
    }

    private fun peekHead(): Head? {
      if (remaining() <= 0) return null
      val b = data[pos].toInt() and 0xff
      val type = b and 0x0f
      var tag = (b ushr 4) and 0x0f
      var p = pos + 1
      if (tag == 15) {
        if (p >= data.size) return null
        tag = data[p].toInt() and 0xff
      }
      return Head(tag, type)
    }

    private fun readHead(): Head {
      val b = readU8()
      val type = b and 0x0f
      var tag = (b ushr 4) and 0x0f
      if (tag == 15) tag = readU8()
      return Head(tag, type)
    }

    private fun skipField(type: Int) {
      when (type) {
        TYPE_BYTE -> pos += 1
        TYPE_SHORT -> pos += 2
        TYPE_INT -> pos += 4
        TYPE_LONG -> pos += 8
        TYPE_STRING1 -> pos += 1 + (data[pos].toInt() and 0xff)
        TYPE_STRING4 -> {
          val len = readI32Raw()
          pos += len
        }
        TYPE_LIST -> {
          val len = readInt32(0, required = true, defaultValue = 0)
          for (i in 0 until len) {
            val h = readHead()
            skipField(h.type)
          }
        }
        TYPE_STRUCT_BEGIN -> {
          while (true) {
            val h = readHead()
            if (h.type == TYPE_STRUCT_END) break
            skipField(h.type)
          }
        }
        TYPE_ZERO_TAG -> Unit
        TYPE_SIMPLE_LIST -> {
          val elem = readHead()
          require(elem.type == TYPE_BYTE) { "simple_list elem type must be BYTE, got ${elem.type}" }
          val len = readInt32(0, required = true, defaultValue = 0)
          pos += len
        }
        else -> error("Unsupported TARS type: $type")
      }
    }

    private fun skipToTag(targetTag: Int): Int? {
      while (true) {
        val head = peekHead() ?: return null
        if (head.type == TYPE_STRUCT_END) return null
        when {
          head.tag < targetTag -> {
            val h = readHead()
            skipField(h.type)
          }
          head.tag == targetTag -> {
            val h = readHead()
            return h.type
          }
          else -> return null
        }
      }
    }

    fun readInt32(tag: Int, required: Boolean, defaultValue: Int): Int {
      val type = skipToTag(tag) ?: return if (required) error("Missing required tag=$tag") else defaultValue
      return when (type) {
        TYPE_ZERO_TAG -> 0
        TYPE_BYTE -> data[pos++].toInt()
        TYPE_SHORT -> readI16Raw().toInt()
        TYPE_INT -> readI32Raw()
        else -> error("Tag=$tag expected int32, got type=$type")
      }
    }

    fun readInt64(tag: Int, required: Boolean, defaultValue: Long): Long {
      val type = skipToTag(tag) ?: return if (required) error("Missing required tag=$tag") else defaultValue
      return when (type) {
        TYPE_ZERO_TAG -> 0L
        TYPE_BYTE -> data[pos++].toLong()
        TYPE_SHORT -> readI16Raw().toLong()
        TYPE_INT -> readI32Raw().toLong()
        TYPE_LONG -> readI64Raw()
        else -> error("Tag=$tag expected int64, got type=$type")
      }
    }

    fun readStringBytes(tag: Int, required: Boolean, defaultValue: ByteArray): ByteArray {
      val type = skipToTag(tag) ?: return if (required) error("Missing required tag=$tag") else defaultValue
      val len = when (type) {
        TYPE_STRING1 -> readU8()
        TYPE_STRING4 -> readI32Raw()
        else -> error("Tag=$tag expected string, got type=$type")
      }
      val b = data.copyOfRange(pos, pos + len)
      pos += len
      return b
    }

    fun readBytes(tag: Int, required: Boolean, defaultValue: ByteArray): ByteArray {
      val type = skipToTag(tag) ?: return if (required) error("Missing required tag=$tag") else defaultValue
      return when (type) {
        TYPE_SIMPLE_LIST -> {
          val elem = readHead()
          require(elem.type == TYPE_BYTE) { "simple_list elem type must be BYTE, got ${elem.type}" }
          val len = readInt32(0, required = true, defaultValue = 0)
          val b = data.copyOfRange(pos, pos + len)
          pos += len
          b
        }
        TYPE_LIST -> {
          val len = readInt32(0, required = true, defaultValue = 0)
          val out = ByteArray(len)
          for (i in 0 until len) {
            val h = readHead()
            require(h.type == TYPE_BYTE) { "byte list elem type must be BYTE, got ${h.type}" }
            out[i] = data[pos++]
          }
          out
        }
        else -> error("Tag=$tag expected bytes, got type=$type")
      }
    }

    fun <T> readStruct(tag: Int, required: Boolean, reader: (StructReader) -> T): T? {
      val type = skipToTag(tag) ?: return if (required) error("Missing required tag=$tag") else null
      if (type != TYPE_STRUCT_BEGIN) error("Tag=$tag expected struct_begin, got type=$type")
      val sr = StructReader(this)
      val res = reader(sr)
      sr.finish()
      return res
    }

    class StructReader(private val input: Input) {
      fun readStringBytes(tag: Int, defaultValue: ByteArray): ByteArray =
        input.readStringBytes(tag, required = false, defaultValue = defaultValue)

      fun finish() {
        while (true) {
          val h = input.readHead()
          if (h.type == TYPE_STRUCT_END) break
          input.skipField(h.type)
        }
      }
    }
  }
}

