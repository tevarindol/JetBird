package dev.bg.jetbird.lib

import io.netbird.android.PlatformFiles as NetBirdPlatformFiles

class PlatformFiles(
    private val configurationFilePath: String,
    private val stateFilePath: String,
    private val cacheFilePath: String
): NetBirdPlatformFiles {

    override fun configurationFilePath(): String = configurationFilePath

    override fun stateFilePath(): String = stateFilePath

    override fun cacheDir(): String = cacheFilePath

}
