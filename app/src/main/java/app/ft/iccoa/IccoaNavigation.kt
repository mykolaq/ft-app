package app.ft.iccoa

import app.ft.core.NavigationInfo
import app.ft.core.ProtoWriter

/**
 * FAW RAV4 OEM Carlink.apk ucar.proto ControlIndex.notify_navigation_info = 11.
 *
 * CarLinkAdapter's onNavigationInfoReceived toggles CarLinkSensorMgr on/off
 * based on isNavigating. That SDK path produces GPS through UCarAdapter.
 * Do NOT claim arbitrary app mirroring is navigation; only AA's real nav state.
 */
internal object IccoaNavigation {
    const val CONTROL_METHOD = 11

    fun encode(info: NavigationInfo): ByteArray = ProtoWriter()
        .bool(1, info.active)
        .string(3, info.distanceMeters?.coerceAtLeast(0)?.toString() ?: "")
        .string(4, "m")
        .string(6, info.road)
        .toByteArray()
}
