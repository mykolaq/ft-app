package app.ft.iccoa

import android.content.Context

/** Last user-approved car. Only descriptive BLE metadata is stored; never PINs or keys. */
internal class IccoaRememberedCar(context: Context) {
    private val prefs = context.getSharedPreferences("iccoa-remembered-car", Context.MODE_PRIVATE)

    fun remember(peer: IccoaPeer): Boolean {
        if (!peer.connectable || !peer.hint || peer.address.isBlank()) return false
        return prefs.edit().putString("address", peer.address)
            .putString("name", peer.name.trim()).commit()
    }

    fun select(peers: List<IccoaPeer>, scanComplete: Boolean): IccoaPeer? {
        val address = prefs.getString("address", "").orEmpty()
        if (address.isBlank()) return null
        val direct = peers.filter { it.connectable && it.hint && it.address.equals(address, ignoreCase = true) }
        if (direct.size == 1) return direct.single()
        // XA60 rotates BLE addresses. A uniquely named ICCOA peer is a candidate,
        // not an authenticated car; the normal cryptographic handshake still applies.
        val name = prefs.getString("name", "").orEmpty()
        if (!scanComplete || name.isBlank()) return null
        val matching = peers.filter {
            it.connectable && it.hint && it.name.trim().equals(name, ignoreCase = true)
        }
        return matching.singleOrNull()
    }
}
