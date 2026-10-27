package com.gateall.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DeviceStore(context: Context) {

    private val sp = context.getSharedPreferences("gateall_saved_v1", Context.MODE_PRIVATE)

    fun all(): List<SavedTarget> {
        val raw = sp.getString("targets", "[]") ?: "[]"
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun save(target: SavedTarget) {
        val list = all().filterNot { it.device.key == target.device.key }.toMutableList()
        list.add(target)
        persist(list)
    }

    fun remove(key: String) {
        persist(all().filterNot { it.device.key == key })
    }

    private fun persist(list: List<SavedTarget>) {
        val arr = JSONArray()
        list.forEach { arr.put(dump(it)) }
        sp.edit().putString("targets", arr.toString()).apply()
    }

    private fun dump(t: SavedTarget) = JSONObject().apply {
        put("ip", t.device.ip)
        put("mac", t.device.mac)
        put("host", t.device.hostname)
        put("vendor", t.device.vendor)
        put("model", t.device.model)
        put("os", t.device.os.name)
        put("label", t.device.label)
        put("protocol", t.protocol.name)
        put("user", t.user)
        put("pass", t.password)
        put("share", t.share)
        put("agentPort", t.agentPort)
        put("savedAt", t.savedAt)
    }

    private fun parse(o: JSONObject): SavedTarget {
        val proto = runCatching { Protocol.valueOf(o.optString("protocol")) }
            .getOrDefault(Protocol.NONE)

        return SavedTarget(
            device = Device(
                ip = o.optString("ip"),
                mac = o.optString("mac").ifBlank { null },
                hostname = o.optString("host").ifBlank { null },
                vendor = o.optString("vendor").ifBlank { null },
                model = o.optString("model").ifBlank { null },
                os = runCatching { OsKind.valueOf(o.optString("os")) }
                    .getOrDefault(OsKind.UNKNOWN),
                label = o.optString("label"),
                protocol = proto,
                agentPort = o.optInt("agentPort").takeIf { it > 0 }
            ),
            protocol = proto,
            user = o.optString("user").ifBlank { null },
            password = o.optString("pass").ifBlank { null },
            share = o.optString("share").ifBlank { null },
            agentPort = o.optInt("agentPort").takeIf { it > 0 },
            savedAt = o.optLong("savedAt", System.currentTimeMillis())
        )
    }
}
