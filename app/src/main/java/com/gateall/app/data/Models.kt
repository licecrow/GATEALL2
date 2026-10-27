package com.gateall.app.data

enum class Protocol { NONE, ADB, AGENT, SMB }

enum class OsKind { ANDROID, IOS, WINDOWS, MAC, LINUX, UNKNOWN }

data class Device(
    val ip: String,
    val mac: String? = null,
    val hostname: String? = null,
    val vendor: String? = null,
    val model: String? = null,
    val os: OsKind = OsKind.UNKNOWN,
    val openPorts: List<Int> = emptyList(),
    val protocol: Protocol = Protocol.NONE,
    val agentPort: Int? = null,
    val label: String = "Desconhecido",
    val lastSeen: Long = System.currentTimeMillis(),
    val reachable: Boolean = true
) {
    val isAndroid: Boolean get() = os == OsKind.ANDROID
    val key: String get() = mac ?: ip
}

data class RemoteEntry(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val size: Long,
    val mime: String?
) {
    val isImage: Boolean get() = mime?.startsWith("image/") == true
    val isVideo: Boolean get() = mime?.startsWith("video/") == true
    val isMedia: Boolean get() = isImage || isVideo
}

data class SavedTarget(
    val device: Device,
    val protocol: Protocol,
    val user: String? = null,
    val password: String? = null,
    val share: String? = null,
    val agentPort: Int? = null,
    val savedAt: Long = System.currentTimeMillis()
)
