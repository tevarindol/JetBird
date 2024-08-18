package dev.bg.jetbird.lib

import io.netbird.android.IFaceDiscover
import java.net.NetworkInterface

class InterfaceDiscovery: IFaceDiscover {

    // e.g. wlan0 26 1500 true true false false true|fe80::74b1:36ff:fe05:ed4c%wlan0/64 10.3.10.199/24
    override fun iFaces(): String {
        return NetworkInterface.getNetworkInterfaces().toList().joinToString(separator = "\n") { networkInterface ->
            "${networkInterface.name} ${networkInterface.index} ${networkInterface.mtu} ${networkInterface.isUp} ${networkInterface.supportsMulticast()} ${networkInterface.isLoopback} ${networkInterface.isPointToPoint} ${networkInterface.supportsMulticast()}|".plus(
                networkInterface.interfaceAddresses.joinToString(" ") { interfaceAddress -> "${interfaceAddress.address.hostAddress}/${interfaceAddress.networkPrefixLength}" }
            )
        }.plus("\n")
    }

}
