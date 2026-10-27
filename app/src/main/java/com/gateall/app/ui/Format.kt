package com.gateall.app.ui

fun humanSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"

    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes / 1024.0
    var idx = 0

    while (value >= 1024 && idx < units.size - 1) {
        value /= 1024
        idx++
    }

    return "%.1f %s".format(value, units[idx])
}
