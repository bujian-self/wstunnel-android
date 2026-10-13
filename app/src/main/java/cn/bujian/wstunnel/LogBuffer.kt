package cn.bujian.wstunnel

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cross-process file-backed log. TProxyService runs in the ":native" process while
 * MainActivity runs in the main process, so an in-memory buffer cannot be shared.
 */
object LogBuffer {
    @Volatile
    private var file: File? = null
    private const val MAX_BYTES = 128 * 1024

    @JvmStatic
    @Synchronized
    fun init(dir: File) {
        val f = File(dir, "wstunnel.log")
        file = f
        if (f.length() > MAX_BYTES) {
            val keep = f.readText().takeLast(MAX_BYTES / 2)
            f.writeText(keep.substringAfter('\n', keep))
        }
    }

    @JvmStatic
    fun append(line: String) {
        val f = file ?: return
        val ts = SimpleDateFormat("MM-dd HH:mm:ss", Locale.US).format(Date())
        synchronized(this) {
            runCatching { f.appendText("$ts $line\n") }
        }
    }

    @JvmStatic
    fun tail(max: Int = 300): String {
        val f = file ?: return ""
        return synchronized(this) {
            runCatching {
                f.readText().lineSequence().toList().takeLast(max).joinToString("\n")
            }.getOrDefault("")
        }
    }
}
