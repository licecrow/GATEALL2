package com.gateall.app.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class NetworkScanner {

    fun localIpv4(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(":") == false }
            ?.hostAddress
    }.getOrNull()

    fun prefix24(ip: String): String = ip.split(".").take(3).joinToString(".")

    /** Varredura da /24: ICMP + TCP em portas comuns. */
    suspend fun sweep(prefix: String, onFound: suspend (String) -> Unit): List<String> =
        coroutineScope {
            val found = mutableListOf<String>()

            (1..254).map { host ->
                async(Dispatchers.IO) {
                    val ip = "$prefix.$host"
                    if (isAlive(ip)) {
                        synchronized(found) { found.add(ip) }
                        onFound(ip)
                    }
                }
            }.awaitAll()

            found.sortedBy { it.split(".").last().toIntOrNull() ?: 0 }
        }

    private fun isAlive(ip: String): Boolean {
        val icmp = runCatching { InetAddress.getByName(ip).isReachable(700) }
            .getOrDefault(false)
        if (icmp) return true

        for (port in intArrayOf(5555, 445, 62078, 8765, 80, 22)) {
            if (tcpOpen(ip, port, 250)) return true
        }
        return false
    }

    fun tcpOpen(ip: String, port: Int, timeoutMs: Int = 400): Boolean = runCatching {
        Socket().use { it.connect(InetSocketAddress(ip, port), timeoutMs) }
        true
    }.getOrDefault(false)

    /** Lê a tabela ARP do kernel — dá MAC sem precisar de root. */
    fun arpTable(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        runCatching {
            File("/proc/net/arp").forEachLine { line ->
                val p = line.trim().split(Regex("\\s+"))
                if (p.size >= 4 && p[1].matches(Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}"))) {
                    if (p[3] != "00:00:00:00:00:00") map[p[0]] = p[3].uppercase()
                }
            }
        }
        return map
    }

    suspend fun hostname(ip: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val name = InetAddress.getByName(ip).canonicalHostName
            name.takeIf { it.isNotBlank() && it != ip }?.substringBefore('.')
        }.getOrNull()
    }
}
