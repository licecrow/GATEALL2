package com.gateall.app.net

import com.gateall.app.data.RemoteEntry
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Properties

class SmbGateway(
    private val ip: String,
    private val user: String? = null,
    private val password: String? = null
) {

    private val ctx: CIFSContext by lazy {
        val props = Properties().apply {
            setProperty("jcifs.smb.client.minVersion", "SMB202")
            setProperty("jcifs.smb.client.maxVersion", "SMB311")
            setProperty("jcifs.smb.client.responseTimeout", "8000")
        }

        val base = BaseContext(PropertyConfiguration(props))
        if (user != null) {
            base.withCredentials(NtlmPasswordAuthenticator(null, user, password.orEmpty()))
        } else {
            base.withAnonymousCredentials()
        }
    }

    suspend fun shares(): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            SmbFile("smb://$ip/", ctx).listFiles().map { it.name.trimEnd('/') }
        }.getOrDefault(emptyList())
    }

    suspend fun list(path: String): List<RemoteEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val url = path.removeSuffix("/") + "/"
            SmbFile(url, ctx).listFiles().map { f ->
                RemoteEntry(
                    name = f.name.trimEnd('/'),
                    path = f.path,
                    isDir = f.isDirectory,
                    size = if (f.isDirectory) 0L else runCatching { f.length() }.getOrDefault(0L),
                    mime = Mime.guess(f.name)
                )
            }.sortedWith(
                compareByDescending<RemoteEntry> { it.isDir }.thenBy { it.name.lowercase() }
            )
        }.getOrDefault(emptyList())
    }

    suspend fun openStream(path: String): InputStream? = withContext(Dispatchers.IO) {
        runCatching { SmbFile(path, ctx).openInputStream() }.getOrNull()
    }

    suspend fun download(path: String, dest: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            dest.parentFile?.mkdirs()
            SmbFile(path, ctx).openInputStream().use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            true
        }.getOrDefault(false)
    }
}
