package dev.bg.jetbird.lib

import io.netbird.android.PlatformFiles as NetBirdPlatformFiles

class PlatformFiles(
    private val configurationFilePath: String,
    private val stateFilePath: String
): NetBirdPlatformFiles {

    override fun configurationFilePath(): String = configurationFilePath

    override fun stateFilePath(): String = stateFilePath

}
