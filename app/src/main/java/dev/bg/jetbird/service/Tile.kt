package dev.bg.jetbird.service

import android.content.Intent
import android.net.VpnService
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import dev.bg.jetbird.R
import dev.bg.jetbird.data.Actions
import dev.bg.jetbird.repository.VpnRepository
import dev.bg.jetbird.util.IoDispatcher
import dev.bg.jetbird.util.ReplaceableJob
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class Tile: TileService() {

    @Inject
    lateinit var vpnRepository: VpnRepository

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private lateinit var scope: CoroutineScope
    private var job = ReplaceableJob()

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(ioDispatcher)
    }

    override fun onStartListening() {
        super.onStartListening()
        startUpdateJob()
    }

    override fun onStopListening() {
        job.cancel()
        super.onStopListening()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        if (VpnService.prepare(this) == null) {
            if (vpnRepository.isVpnConnected()) {
                startService(
                    Intent(this, VPNService::class.java).apply {
                        action = Actions.TILE_STOP_SERVICE_ACTION
                    }
                )
            } else {
                startService(
                    Intent(this, VPNService::class.java).apply {
                        action = Actions.TILE_START_SERVICE_ACTION
                    }
                )
            }
        } else {
            Toast.makeText(this, getString(R.string.requires_vpn_permission), Toast.LENGTH_LONG).show()
        }
    }

    private fun startUpdateJob() {
        job += scope.launch {
            while (isActive) {
                updateTile(vpnRepository.isVpnConnected())
                delay(2000L)
            }
        }
    }

    private fun updateTile(
        isActive: Boolean
    ) {
        val tile = qsTile ?: return
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

}
