package dev.bg.jetbird.util.ktx

fun <K, V> Map<K, V>.assertivelyGet(key: K): V {
    return this[key]!!
}
