package dev.bg.jetbird

import dev.bg.jetbird.util.calculateTunneledRoutes
import org.junit.Test

class SubnetCalculator {

    @Test
    fun testNoOverlaps() {
        assert (
            calculateTunneledRoutes(
                setOf("192.168.1.0/24", "10.0.0.0/16"),
                setOf("192.168.2.0/24", "10.1.0.0/16")
            ).containsAll(setOf("10.0.0.0/16", "192.168.1.0/24"))
        )
    }

    @Test
    fun testFullSubnetExclusion() {
        assert (
            calculateTunneledRoutes(
                setOf("192.168.1.0/24"),
                setOf("192.168.1.0/28")
            ).containsAll(
                setOf("192.168.1.128/25",
                    "192.168.1.64/26",
                    "192.168.1.32/27",
                    "192.168.1.16/28"
                )
            )
        )
    }

    @Test
    fun testPartialOverlap() {
        println(calculateTunneledRoutes(
            setOf("192.168.1.0/24"),
            setOf("192.168.1.0/28", "192.168.1.240/28")
        ))
        assert (
            calculateTunneledRoutes(
                setOf("192.168.1.0/24"),
                setOf("192.168.1.0/28", "192.168.1.240/28")
            ).containsAll(
                setOf(
                    "192.168.1.64/26",
                    "192.168.1.128/26",
                    "192.168.1.32/27",
                    "192.168.1.192/27",
                    "192.168.1.16/28",
                    "192.168.1.224/28"
                )
            )
        )
    }

    @Test
    fun testIpv6() {
        assert (
            calculateTunneledRoutes(
                setOf("2001:db8::/48"),
                setOf("2001:db8:1::/64")
            ).containsAll(setOf("2001:db8::/48"))
        )
    }

    @Test
    fun testIpv4Ipv6() {
        assert (
            calculateTunneledRoutes(
                setOf("192.168.1.0/24", "2001:db8::/32"),
                setOf("2001:db8:aaaa::/48")
            ).containsAll(
                setOf(
                    "192.168.1.0/24",
                    "2001:db8::/33",
                    "2001:db8:c000::/34",
                    "2001:db8:8000::/35",
                    "2001:db8:b000::/36",
                    "2001:db8:a000::/37",
                    "2001:db8:ac00::/38",
                    "2001:db8:a800::/39",
                    "2001:db8:ab00::/40",
                    "2001:db8:aa00::/41",
                    "2001:db8:aac0::/42",
                    "2001:db8:aa80::/43",
                    "2001:db8:aab0::/44",
                    "2001:db8:aaa0::/45",
                    "2001:db8:aaac::/46",
                    "2001:db8:aaa8::/47",
                    "2001:db8:aaab::/48",
                )
            )
        )
    }

    @Test
    fun testLocalBlock() {
        assert (
            calculateTunneledRoutes(
                setOf("0.0.0.0/0"),
                setOf("10.0.1.1/32", "10.0.1.2/32")
            ).containsAll(
                setOf(
                    "128.0.0.0/1",
                    "64.0.0.0/2",
                    "32.0.0.0/3",
                    "16.0.0.0/4",
                    "0.0.0.0/5",
                    "12.0.0.0/6",
                    "8.0.0.0/7",
                    "11.0.0.0/8",
                    "10.128.0.0/9",
                    "10.64.0.0/10",
                    "10.32.0.0/11",
                    "10.16.0.0/12",
                    "10.8.0.0/13",
                    "10.4.0.0/14",
                    "10.2.0.0/15",
                    "10.1.0.0/16",
                    "10.0.128.0/17",
                    "10.0.64.0/18",
                    "10.0.32.0/19",
                    "10.0.16.0/20",
                    "10.0.8.0/21",
                    "10.0.4.0/22",
                    "10.0.2.0/23",
                    "10.0.0.0/24",
                    "10.0.1.128/25",
                    "10.0.1.64/26",
                    "10.0.1.32/27",
                    "10.0.1.16/28",
                    "10.0.1.8/29",
                    "10.0.1.4/30",
                    "10.0.1.0/32",
                    "10.0.1.3/32",
                )
            )
        )
    }

}