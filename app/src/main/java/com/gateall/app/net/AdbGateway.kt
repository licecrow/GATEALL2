package com.gateall.app.net

import com.gateall.app.data.RemoteEntry
import dadb.Dadb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Ponte ADB over TCP. dadb fala o protocolo ADB nativo — sem precisar
 * do adb.exe no PC e sem instalar nada no aparelho alvo.
 */
class AdbGateway(private val ip: String, private val port: Int = 5555) {

    private var dadb: Dadb? = null

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            dadb = Dadb.create(ip, port)
            shell("echo gateall_ok").contains("gateall_ok")
        }.getOrDefault(false)
    }

    /**
     * Executa um comando no aparelho alvo e devolve sempre texto.
     * Nao assume o tipo de retorno do dadb — converte de forma defensiva.
     */
    suspend fun shell(cmd: String): String = withContext(Dispatchers.IO) {
        val raw: Any? = runCatching { dadb!!.shell(cmd) }.getOrNull()
        asText(raw)
    }

    private fun asText(raw: Any?): String {
        if (raw == null) return ""
        if (raw is String) return raw

        val obj: Any = raw

        // Alguns builds do dadb devolvem um stream em vez de String.
        for (nome in listOf("readString", "readUtf8", "read")) {
            val v = runCatching {
                obj.javaClass.getMethod(nome).invoke(obj)
            }.getOrNull()
            if (v is String) return v
        }

        return obj.toString()
    }

    suspend fun props(): Map<String, String> = withContext(Dispatchers.IO) {
        val wanted = setOf(
            "ro.product.brand",
            "ro.product.model",
            "ro.product.manufacturer",
            "ro.build.version.release",
            "ro.product.device"
        )

        val raw = shell("getprop")
        if (raw.isBlank()) return@withContext emptyMap()

        raw.split("\n")
            .mapNotNull { line ->
                val m = Regex("\\[(.*?)\\]:\\s*\\[(.*?)\\]").find(line)
                    ?: return@mapNotNull null
                m.groupValues[1] to m.groupValues[2]
            }
            .filter { it.first in wanted }
            .toMap()
    }

    suspend fun list(path: String): List<RemoteEntry> = withContext(Dispatchers.IO) {
        val safe = path.replace("\"", "\\\"")
        shell("ls -la \"$safe\"")
            .split("\n")
            .mapNotNull { parseLine(it, path) }
    }

    private fun parseLine(line: String, parent: String): RemoteEntry? {
        val parts = line.trim().split(Regex("\\s+"))
        if (parts.size < 7) return null

        val perms = parts[0]
        if (perms.length < 10) return null
        if (perms.startsWith("l")) return null

        val isDir = perms.startsWith("d")
        val size = parts[4].toLongOrNull() ?: 0L

        val nameIdx = if (parts.size > 8 && parts[5].matches(Regex("[A-Za-z]{3}"))) 8 else 7
        if (parts.size <= nameIdx) return null

        val name = parts.drop(nameIdx).joinToString(" ")
        if (name == "." || name == "..") return null

        val full = if (parent.endsWith("/")) "$parent$name" else "$parent/$name"

        return RemoteEntry(
            name = name,
            path = full,
            isDir = isDir,
            size = if (isDir) 0 else size,
            mime = Mime.guess(name)
        )
    }

    suspend fun pull(remote: String, dest: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            dest.parentFile?.mkdirs()
            dadb!!.pull(dest, remote)
            dest.exists() && dest.length() > 0
        }.getOrDefault(false)
    }

    suspend fun push(local: File, remote: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            dadb!!.push(local, remote)
            true
        }.getOrDefault(false)
    }

    fun close() {
        runCatching { dadb?.close() }
        dadb = null
    }
}
