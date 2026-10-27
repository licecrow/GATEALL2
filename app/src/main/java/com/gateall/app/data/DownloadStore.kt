package com.gateall.app.data

import android.os.Environment
import java.io.File

/**
 * Toda transferência cai em Documentos/GATEALL DOWNLOAD.
 * No Android 11+ essa pasta é gravável sem permissão.
 */
object DownloadStore {

    private const val FOLDER = "GATEALL DOWNLOAD"

    fun root(): File {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(docs, FOLDER)
        if (!dir.exists()) dir.mkdirs()
        val marker = File(dir, ".gateall")
        if (!marker.exists()) runCatching { marker.writeText("GATEALL") }
        return dir
    }

    fun sub(vararg parts: String): File {
        var f = root()
        parts.forEach { f = File(f, it) }
        if (!f.exists()) f.mkdirs()
        return f
    }

    fun unique(file: File): File {
        if (!file.exists()) return file
        val base = file.nameWithoutExtension
        val ext = file.extension
        var i = 1
        while (true) {
            val name = if (ext.isEmpty()) "$base ($i)" else "$base ($i).$ext"
            val candidate = File(file.parentFile, name)
            if (!candidate.exists()) return candidate
            i++
        }
    }

    fun listing(): List<File> =
        root().listFiles()
            ?.filter { it.isFile && !it.name.startsWith(".") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
}
