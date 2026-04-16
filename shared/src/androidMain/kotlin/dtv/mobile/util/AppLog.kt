package dtv.mobile.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLog {
  private val lock = Any()
  @Volatile private var file: File? = null

  fun init(context: Context) {
    if (file != null) return
    synchronized(lock) {
      if (file != null) return
      val dir = File(context.filesDir, "dtv-logs").apply { mkdirs() }
      val name = "dtv-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())}.log"
      file = File(dir, name)
      cleanupOldLogs(dir, keep = 10)
      Log.i("DTV-LOG", "log file: ${file!!.absolutePath}")
      appendRaw("I", "DTV-LOG", "log file: ${file!!.absolutePath}", null)
    }
  }

  fun currentLogFilePath(): String? = file?.absolutePath

  fun d(tag: String, message: String) {
    Log.d(tag, message)
    appendRaw("D", tag, message, null)
  }

  fun i(tag: String, message: String) {
    Log.i(tag, message)
    appendRaw("I", tag, message, null)
  }

  fun w(tag: String, message: String, t: Throwable? = null) {
    if (t != null) Log.w(tag, message, t) else Log.w(tag, message)
    appendRaw("W", tag, message, t)
  }

  fun e(tag: String, message: String, t: Throwable? = null) {
    if (t != null) Log.e(tag, message, t) else Log.e(tag, message)
    appendRaw("E", tag, message, t)
  }

  private fun cleanupOldLogs(dir: File, keep: Int) {
    val logs = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".log") }.orEmpty()
      .sortedByDescending { it.lastModified() }
    logs.drop(keep).forEach { runCatching { it.delete() } }
  }

  private fun appendRaw(level: String, tag: String, message: String, t: Throwable?) {
    val f = file ?: return
    val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
    val line = buildString {
      append(now)
      append(" [")
      append(level)
      append("] ")
      append(tag)
      append(": ")
      append(message)
      if (t != null) {
        append('\n')
        append(t.stackTraceStringCompat())
      }
      append('\n')
    }

    synchronized(lock) {
      runCatching {
        FileWriter(f, true).use { it.write(line) }
      }
    }
  }
}

private fun Throwable.stackTraceStringCompat(): String {
  val sw = StringWriter()
  PrintWriter(sw).use { pw -> this.printStackTrace(pw) }
  return sw.toString()
}

