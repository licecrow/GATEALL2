package com.gateall.app.net

import com.gateall.app.data.RemoteEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Fala com o agente GATEALL rodando dentro do aparelho alvo (porta 8765).
 * O agente é um servidor HTTP minúsculo: lista pastas, serve arquivo com Range
 * (por isso vídeo dá seek) e responde /ping com identidade.
 */
class AgentGateway(private val ip: String, private val port: Int = 8765) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val base: String get() = "http://$ip:$port"

    suspend fun ping(): Map<String, String>? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("$base/ping").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val obj = JSONObject(resp.body!!.string())
                obj.keys().asSequence().associateWith { obj.getString(it) }
            }
        }.getOrNull()
    }

    suspend fun list(path: String): List<RemoteEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val url = "$base/list?path=" + URLEncoder.encode(path, "UTF-8")
            val req = Request.Builder().url(url).build()

            client.newCall(req).execute().use { resp ->
                val arr = JSONArray(resp.body!!.string())
                (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    RemoteEntry(
                        name = o.getString("name"),
                        path = o.getString("path"),
                        isDir = o.getBoolean("dir"),
                        size = o.optLong("size", 0L),
                        mime = o.optString("mime").ifBlank { Mime.guess(o.getString("name")) }
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun fileUrl(path: String): String =
        "$base/file?path=" + URLEncoder.encode(path, "UTF-8")

    suspend fun download(
        path: String,
        dest: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(fileUrl(path)).build()

        client.newCall(req).execute().use { resp ->
            val total = resp.body!!.contentLength()
            dest.parentFile?.mkdirs()

            resp.body!!.byteStream().use { input ->
                dest.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    while (input.read(buf).also { read = it } > 0) {
                        output.write(buf, 0, read)
                        done += read
                        onProgress(done, total)
                    }
                }
            }
        }
        dest
    }

    suspend fun openStream(path: String): InputStream? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(fileUrl(path)).build()
            client.newCall(req).execute().body!!.byteStream()
        }.getOrNull()
    }

    fun close() {
        runCatching { client.dispatcher.executorService.shutdown() }
    }
}
