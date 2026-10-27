package com.gateall.app.net

import com.gateall.app.data.Device
import com.gateall.app.data.OsKind
import com.gateall.app.data.Protocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class DeviceIdentifier(private val scanner: NetworkScanner) {

    private val probes = listOf(
        5555, 445, 139, 62078, 8765, 5000, 548, 3389, 5900, 22, 80, 8080
    )

    suspend fun identify(ip: String, mac: String?): Device = coroutineScope {
        val open = mutableListOf<Int>()

        probes.map { port ->
            async(Dispatchers.IO) {
                if (scanner.tcpOpen(ip, port, 300)) {
                    synchronized(open) { open.add(port) }
                }
            }
        }.awaitAll()

        val host = scanner.hostname(ip)
        val vendor = MacVendors.lookup(mac)
        val ports = open.sorted()

        val os = when {
            ports.contains(8765) || ports.contains(5555) -> OsKind.ANDROID
            ports.contains(62078) -> OsKind.IOS
            ports.contains(445) || ports.contains(139) || ports.contains(3389) -> OsKind.WINDOWS
            ports.contains(548) -> OsKind.MAC
            ports.contains(22) || ports.contains(5900) -> OsKind.LINUX
            else -> OsKind.UNKNOWN
        }

        val protocol = when {
            ports.contains(8765) -> Protocol.AGENT
            ports.contains(5555) -> Protocol.ADB
            ports.contains(445) || ports.contains(139) -> Protocol.SMB
            else -> Protocol.NONE
        }

        val model = inferModel(host, vendor)

        Device(
            ip = ip,
            mac = mac,
            hostname = host,
            vendor = vendor,
            model = model,
            os = os,
            openPorts = ports,
            protocol = protocol,
            agentPort = if (ports.contains(8765)) 8765 else null,
            label = buildLabel(model, vendor, host, os, ip)
        )
    }

    private fun inferModel(host: String?, vendor: String?): String? {
        val h = host?.lowercase() ?: return null
        return when {
            h.contains("poco") -> "POCO"
            h.contains("redmi") -> "Redmi"
            h.contains("mi-") || h.contains("xiaomi") -> "Xiaomi"
            h.contains("sm-") || h.contains("galaxy") -> "Samsung Galaxy"
            h.contains("moto") -> "Motorola"
            h.contains("pixel") -> "Pixel"
            h.contains("iphone") -> "iPhone"
            h.contains("ipad") -> "iPad"
            h.contains("desktop") || h.contains("laptop") -> "PC ${vendor.orEmpty()}".trim()
            h.contains("raspberry") -> "Raspberry Pi"
            else -> null
        }
    }

    private fun buildLabel(
        model: String?,
        vendor: String?,
        host: String?,
        os: OsKind,
        ip: String
    ): String {
        val osName = when (os) {
            OsKind.ANDROID -> "Android"
            OsKind.IOS -> "iOS"
            OsKind.WINDOWS -> "Windows"
            OsKind.MAC -> "macOS"
            OsKind.LINUX -> "Linux"
            OsKind.UNKNOWN -> "Desconhecido"
        }

        val parts = listOfNotNull(vendor, model, host).distinct()
        return if (parts.isEmpty()) "$osName · $ip" else parts.joinToString(" ") + " · $osName"
    }

    suspend fun grabHttpTitle(ip: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = java.net.URL("http://$ip/").openConnection() as java.net.HttpURLConnection
            conn.connectTimeout = 1200
            conn.readTimeout = 1200
            conn.connect()
            val html = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            Regex("<title>(.*?)</title>", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)?.trim()
        }.getOrNull()
    }
}
