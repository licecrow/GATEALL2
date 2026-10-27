package com.gateall.app.net

/** Base reduzida de OUI (prefixo MAC) -> fabricante. Serve pra nomear o aparelho. */
object MacVendors {

    private val table = mapOf(
        "3C:5A:B4" to "Google", "F4:F5:D8" to "Google", "DA:A1:19" to "Google",
        "F0:EF:86" to "Google", "A4:77:33" to "Google",
        "64:BC:0C" to "Xiaomi", "8C:BE:BE" to "Xiaomi", "F8:A4:5F" to "Xiaomi",
        "50:8F:4C" to "Xiaomi", "78:11:DC" to "Xiaomi", "28:D1:27" to "Xiaomi",
        "7C:DD:90" to "Samsung", "B4:79:A7" to "Samsung", "C8:19:F7" to "Samsung",
        "34:23:BA" to "Samsung", "00:16:32" to "Samsung",
        "2C:54:91" to "Microsoft", "00:15:5D" to "Microsoft", "7C:1E:52" to "Microsoft",
        "F0:18:98" to "Apple", "A4:83:E7" to "Apple", "00:1B:63" to "Apple",
        "DC:A6:32" to "Raspberry Pi", "B8:27:EB" to "Raspberry Pi",
        "44:65:0D" to "Amazon", "68:54:FD" to "Amazon",
        "E8:4E:06" to "TP-Link", "50:C7:BF" to "TP-Link",
        "2C:3A:FD" to "Intel", "00:1E:64" to "Intel", "34:13:E8" to "Intel",
        "BC:AE:C5" to "ASUS", "2C:56:DC" to "ASUS", "04:D4:C4" to "ASUS",
        "8C:1F:64" to "Realtek", "00:E0:4C" to "Realtek",
        "E4:5F:01" to "Motorola", "34:8A:AE" to "Motorola", "C0:EE:FB" to "Motorola",
        "24:DA:9B" to "Motorola", "9C:28:EF" to "Motorola",
        "DC:54:75" to "Huawei", "00:E0:FC" to "Huawei", "64:A6:51" to "Huawei",
        "A8:9C:ED" to "Huawei", "18:8B:45" to "Huawei",
        "00:1E:C2" to "Apple", "3C:07:54" to "Apple", "68:DB:CA" to "Apple"
    )

    fun lookup(mac: String?): String? {
        if (mac.isNullOrBlank()) return null
        val key = mac.uppercase().replace('-', ':').take(8)
        return table[key]
    }
}
