package cn.bujian.wstunnel

import android.content.Context
import android.system.Os
import java.io.File
import java.io.FileOutputStream

/**
 * Resolves the wstunnel executable.
 *
 * Primary path: the binary is packaged as a native library (`jniLibs/libwstunnel.so`), so the
 * system extracts it to `nativeLibraryDir` at install time with mode 755 — always executable.
 * Fallback: extract from assets and chmod. Needed because some OEM ROMs (observed on Samsung
 * One UI) silently strip the exec bit from files created in app-private storage, and
 * `File.setExecutable` reports success while `execve` fails with EACCES.
 */
object NativeBin {
    @Synchronized
    fun ensure(context: Context): File {
        val fromLib = File(context.applicationInfo.nativeLibraryDir, "libwstunnel.so")
        if (fromLib.canExecute()) return fromLib

        val dir = File(context.filesDir, "bin").apply { mkdirs() }
        val out = File(dir, "wstunnel")
        context.assets.open("wstunnel").use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        }
        runCatching { Os.chmod(out.absolutePath, "755".toInt(8)) }
        if (!out.canExecute()) {
            throw IllegalStateException(
                "wstunnel binary not executable: ${out.absolutePath} " +
                    "(libPath=${fromLib.absolutePath} libExists=${fromLib.exists()} libExec=${fromLib.canExecute()})"
            )
        }
        return out
    }
}
