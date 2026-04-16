package dtv.mobile.platform.huya.purelive.tars

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class TarsOutputStream {
  private val out = ByteArrayOutputStream()
  private val utf8: Charset = Charsets.UTF_8

  private fun writeU8(v: Int) {
    out.write(v and 0xFF)
  }

  private fun writeI16(v: Int) {
    writeU8(v ushr 8)
    writeU8(v)
  }

  private fun writeI32(v: Int) {
    writeU8(v ushr 24)
    writeU8(v ushr 16)
    writeU8(v ushr 8)
    writeU8(v)
  }

  private fun writeI64(v: Long) {
    writeI32((v ushr 32).toInt())
    writeI32((v and 0xFFFFFFFFL).toInt())
  }

  private fun writeHead(type: Int, tag: Int) {
    require(tag >= 0) { "tag must be >= 0" }
    if (tag < 15) {
      writeU8((tag shl 4) or (type and 0x0F))
    } else {
      writeU8((15 shl 4) or (type and 0x0F))
      writeU8(tag)
    }
  }

  fun writeInt(value: Int, tag: Int) {
    when {
      value == 0 -> writeHead(TarsTypes.ZERO_TAG, tag)
      value in Byte.MIN_VALUE..Byte.MAX_VALUE -> {
        writeHead(TarsTypes.BYTE, tag)
        writeU8(value)
      }
      value in Short.MIN_VALUE..Short.MAX_VALUE -> {
        writeHead(TarsTypes.SHORT, tag)
        writeI16(value)
      }
      else -> {
        writeHead(TarsTypes.INT, tag)
        writeI32(value)
      }
    }
  }

  fun writeLong(value: Long, tag: Int) {
    when {
      value == 0L -> writeHead(TarsTypes.ZERO_TAG, tag)
      value in Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong() -> {
        writeHead(TarsTypes.BYTE, tag)
        writeU8(value.toInt())
      }
      value in Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong() -> {
        writeHead(TarsTypes.SHORT, tag)
        writeI16(value.toInt())
      }
      value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() -> {
        writeHead(TarsTypes.INT, tag)
        writeI32(value.toInt())
      }
      else -> {
        writeHead(TarsTypes.LONG, tag)
        writeI64(value)
      }
    }
  }

  fun writeBool(value: Boolean, tag: Int) {
    writeHead(TarsTypes.BYTE, tag)
    writeU8(if (value) 1 else 0)
  }

  fun writeString(value: String, tag: Int) {
    val bytes = value.toByteArray(utf8)
    if (bytes.size <= 255) {
      writeHead(TarsTypes.STRING1, tag)
      writeU8(bytes.size)
      out.write(bytes)
    } else {
      writeHead(TarsTypes.STRING4, tag)
      writeI32(bytes.size)
      out.write(bytes)
    }
  }

  fun writeByteArray(bytes: ByteArray, tag: Int) {
    writeHead(TarsTypes.SIMPLE_LIST, tag)
    writeHead(TarsTypes.BYTE, 0)
    // dart_tars_protocol: writeInt(length, 0) (带 head 的整型)，不是裸 int32
    writeInt(bytes.size, 0)
    out.write(bytes)
  }

  fun toByteArray(): ByteArray = out.toByteArray()
}

