package com.gateall.app.fs

import android.content.Context
import com.gateall.app.data.Device
import com.gateall.app.data.Protocol
import com.gateall.app.data.RemoteEntry
import com.gateall.app.data.SavedTarget
import com.gateall.app.net.AdbGateway
import com.gateall.app.net.AgentGateway
import com.gateall.app.net.SmbGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Interface única pra ADB, agente HTTP e SMB. */
interface RemoteFs {
    val device: Device
    val protocol: Protocol
    val rootPath: String

    suspend fun list(path: String): List<RemoteEntry>

    suspend fun download(
        path: String,
        dest: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Boolean

    suspend fun openStream(path: String): InputStream?
    fun close()
}

private fun List<RemoteEntry>.sortedEntries(): List<RemoteEntry> =
    sortedWith(compareByDescending<RemoteEntry> { it.isDir }.thenBy { it.name.lowercase() })

class AdbFs(override val device: Device) : RemoteFs {

    override val protocol = Protocol.ADB
    override val rootPath = "/sdcard"

    private val gw = AdbGateway(device.ip)
    private var ready = false

    override suspend fun list(path: String): List<RemoteEntry> {
        if (!ready) {
            ready = gw.connect()
            if (!ready) return emptyList()
        }
        return gw.list(path).sortedEntries()
    }

    override suspend fun download(
        path: String,
        dest: File,
        onProgress: (Long, Long) -> Unit
    ): Boolean {
        if (!ready) ready = gw.connect()
        return gw.pull(path, dest)
    }

    override suspend fun openStream(path: String): InputStream? {
        val tmp = File.createTempFile("gateall", ".tmp")
        return if (gw.pull(path, tmp)) tmp.inputStream() else null
    }

    override fun close() = gw.close()
}

class AgentFs(override val device: Device, port: Int) : RemoteFs {

    override val protocol = Protocol.AGENT
    override val rootPath = "/sdcard"

    private val gw = AgentGateway(device.ip, port)

    override suspend fun list(path: String): List<RemoteEntry> = gw.list(path).sortedEntries()

    override suspend fun download(
        path: String,
        dest: File,
        onProgress: (Long, Long) -> Unit
    ): Boolean = runCatching { gw.download(path, dest, onProgress); true }.getOrDefault(false)

    override suspend fun openStream(path: String): InputStream? = gw.openStream(path)

    override fun close() = gw.close()
}

class SmbFs(
    override val device: Device,
    private val user: String?,
    private val password: String?,
    private val share: String?
) : RemoteFs {

    override val protocol = Protocol.SMB
    override val rootPath = "smb://${device.ip}/${share.orEmpty()}"

    private val gw = SmbGateway(device.ip, user, password)

    override suspend fun list(path: String): List<RemoteEntry> = gw.list(path)

    override suspend fun download(
        path: String,
        dest: File,
        onProgress: (Long, Long) -> Unit
    ): Boolean = gw.download(path, dest)

    override suspend fun openStream(path: String): InputStream? = gw.openStream(path)

    override fun close() = Unit
}

object FsFactory {
    fun open(target: SavedTarget): RemoteFs? = when (target.protocol) {
        Protocol.AGENT -> AgentFs(target.device, target.agentPort ?: 8765)
        Protocol.ADB -> AdbFs(target.device)
        Protocol.SMB -> SmbFs(target.device, target.user, target.password, target.share)
        Protocol.NONE -> null
    }
}

/** Cache local pra pré-visualizar mídia de qualquer protocolo. */
object PreviewCache {

    private fun cacheFile(context: Context, fs: RemoteFs, path: String): File {
        val hash = MessageDigest.getInstance("MD5")
            .digest("${fs.device.ip}|$path".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val name = path.substringAfterLast('/')
        return File(File(context.cacheDir, "preview"), "$hash-$name")
    }

    suspend fun ensure(context: Context, fs: RemoteFs, path: String): File? =
        withContext(Dispatchers.IO) {
            val out = cacheFile(context, fs, path)
            if (out.exists() && out.length() > 0) return@withContext out
            val ok = fs.download(path, out)
            if (ok) out else null
        }
}
