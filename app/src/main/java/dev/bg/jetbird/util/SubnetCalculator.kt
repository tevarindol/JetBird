package dev.bg.jetbird.util

import inet.ipaddr.IPAddress
import inet.ipaddr.IPAddressString

fun calculateTunneledRoutes(
    allowedIps: Set<String>,
    disallowedIps: Set<String>
): Set<String> {
    var init = allowedIps.map { IPAddressString(it) }.toSet()
    disallowedIps.map { IPAddressString(it) }.forEach { disallowed ->
        val new = mutableSetOf<IPAddress>()
        init.forEach { allowed ->
            if (allowed.ipVersion == disallowed.ipVersion) {
                if (allowed.contains(disallowed)) {
                    new.addAll(
                        allowed
                            .getAddress()
                            .subtract(disallowed.getAddress())
                            .flatMap { listOf(*it.spanWithPrefixBlocks()) }
                    )
                } else {
                    new.add(allowed.getAddress())
                }
            } else {
                new.add(allowed.getAddress())
            }
        }
        init = new.map { it.toAddressString() }.toSet()
    }
    return init
        .sortedBy { it.networkPrefixLength }
        .map { it.getAddress().toString() }
        .toSet()
}
