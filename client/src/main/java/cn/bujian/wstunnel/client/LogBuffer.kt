package cn.bujian.wstunnel.client

/** Thread-safe ring buffer holding recent log lines from wstunnel and the VPN layer. */
object LogBuffer {
    private val lock = Any()
    private val lines = ArrayDeque<String>()
    private const val MAX_LINES = 500

    fun append(line: String) {
        synchronized(lock) {
            lines.addLast(line)
            while (lines.size > MAX_LINES) lines.removeFirst()
        }
    }

    fun tail(max: Int = 200): String = synchronized(lock) {
        lines.takeLast(max).joinToString("\n")
    }
}
