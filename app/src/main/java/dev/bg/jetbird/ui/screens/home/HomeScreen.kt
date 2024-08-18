package dev.bg.jetbird.ui.screens.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bg.jetbird.LocalServiceConnection
import dev.bg.jetbird.LocalSnackbar
import dev.bg.jetbird.LocalVPNServiceBinder
import dev.bg.jetbird.R
import dev.bg.jetbird.data.JetBirdColors
import dev.bg.jetbird.data.model.ConnectionState
import dev.bg.jetbird.data.model.Peer
import dev.bg.jetbird.service.VPNService
import dev.bg.jetbird.util.OnLifecycleEvent
import dev.bg.jetbird.util.ktx.hasOtherAlwaysOnVpn
import io.netbird.android.URLOpener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val binder = LocalVPNServiceBinder.current
    val serviceConnection = LocalServiceConnection.current
    val uriHandler = LocalUriHandler.current
    val snackbar = LocalSnackbar.current

    val scope = rememberCoroutineScope()
    val vpnState = binder?.state?.collectAsStateWithLifecycle()?.value

    var hasOtherAlwaysOnVpn by remember { mutableStateOf(false) }
    var hasVpnPermission by remember { mutableStateOf(false) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                hasVpnPermission = true
            }
            Activity.RESULT_CANCELED -> {
                scope.launch {
                    snackbar.showSnackbar(ctx.getString(R.string.failed_vpn_permission))
                }
            }
        }
    }
    
    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_START,
            Lifecycle.Event.ON_RESUME -> {
                hasOtherAlwaysOnVpn = vpnState?.connectionState != ConnectionState.CONNECTED && ctx.hasOtherAlwaysOnVpn()
                hasVpnPermission = VpnService.prepare(ctx) == null
                if (!hasOtherAlwaysOnVpn && hasVpnPermission && binder == null) {
                    val i = Intent(ctx, VPNService::class.java)
                    ctx.startService(i)
                    ctx.bindService(i, serviceConnection, Context.BIND_AUTO_CREATE)
                }
            }
            else -> Unit
        }
    }

    val loginListener = URLOpener { url -> uriHandler.openUri(url) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val peerListState = rememberLazyListState()
        val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = rememberStandardBottomSheetState(
                confirmValueChange = {
                    if (it == SheetValue.PartiallyExpanded) {
                        scope.launch {
                            peerListState.animateScrollToItem(0)
                        }
                    }
                    true
                }
            )
        )

        BottomSheetScaffold(
            scaffoldState = bottomSheetScaffoldState,
            sheetShape = RoundedCornerShape(
                bottomStart = 0.dp,
                bottomEnd = 0.dp,
                topStart = 12.dp,
                topEnd = 12.dp
            ),
            sheetShadowElevation = 8.dp,
            sheetTonalElevation = 8.dp,
            sheetContent = {
                PeersList(peerListState)
            },
            sheetPeekHeight = 96.dp
        ) { padding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AlwaysOnVpnBanner(
                    isNetbirdRunning = binder != null && vpnState?.connectionState != ConnectionState.DISCONNECTED,
                    hasOtherAlwaysOnVpn = hasOtherAlwaysOnVpn,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                when {
                    hasVpnPermission && (vpnState?.connectionState == ConnectionState.CONNECTED || vpnState?.connectionState == ConnectionState.DISCONNECTED) -> {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                modifier = Modifier.size(256.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !hasOtherAlwaysOnVpn,
                                onClick = {
                                    if (vpnState.connectionState == ConnectionState.DISCONNECTED) {
                                        if (viewModel.hasAuthed()) {
                                            binder.start(null)
                                        } else {
                                            if (viewModel.getSetupKey() == null) {
                                                binder.start(loginListener)
                                            } else {
                                                binder.start(null)
                                            }
                                            viewModel.updateHasAuthed()
                                        }
                                    } else {
                                        binder.stop()
                                    }
                                }
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_launcher_monochrome),
                                    modifier = Modifier.fillMaxSize(),
                                    contentDescription = if (vpnState.connectionState == ConnectionState.CONNECTED) {
                                        stringResource(R.string.connected)
                                    } else {
                                        stringResource(R.string.disconnect)
                                    }
                                )
                            }
                            Spacer(Modifier.size(36.dp))
                            AnimatedContent(
                                vpnState.connectionState,
                                label = "Connection state animation"
                            ) {connectionState ->
                                if (connectionState == ConnectionState.CONNECTED) {
                                    Text(
                                        text = stringResource(R.string.connected),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 24.sp
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.disconnected),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 24.sp
                                    )
                                }
                            }
                        }
                    }
                    hasVpnPermission && (vpnState?.connectionState == ConnectionState.CONNECTING || vpnState?.connectionState == ConnectionState.DISCONNECTING) -> {
                        CircularProgressIndicator()
                    }
                    !hasVpnPermission -> {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                stringResource(R.string.requires_vpn_permission),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                enabled = !hasOtherAlwaysOnVpn,
                                onClick = {
                                    vpnPermissionLauncher.launch(VpnService.prepare(ctx))
                                }
                            ) {
                                Text(stringResource(R.string.grant_permission))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlwaysOnVpnBanner(
    isNetbirdRunning: Boolean,
    hasOtherAlwaysOnVpn: Boolean,
    modifier: Modifier
) {
    if (!isNetbirdRunning && hasOtherAlwaysOnVpn) {
        Surface(
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(12.dp))
                .then(modifier),
            color = JetBirdColors.ErrorRed
        ) {
            Text(
                stringResource(R.string.another_always_on_vpn_active),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PeersList(
    lazyListState: LazyListState
) {
    val binder = LocalVPNServiceBinder.current

    val connectionState = binder?.state?.collectAsStateWithLifecycle()?.value?.connectionState
    var selectedPeerIp by remember { mutableStateOf<String?>(null) }

    var peers by remember { mutableStateOf<List<Peer>>(emptyList()) }

    // temporary whilst listener gets fixed
    LaunchedEffect(Unit, connectionState) {
        while (true) {
            if (connectionState == ConnectionState.CONNECTED) {
                val newPeers = binder.getPeers() ?: emptyList()
                if (peers != newPeers) {
                    peers = newPeers
                }
            }
            if (connectionState == ConnectionState.DISCONNECTED) {
                peers = emptyList()
            }
            delay(5.seconds)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        userScrollEnabled = connectionState != null && connectionState == ConnectionState.CONNECTED
    ) {
        item {
            Text(
                stringResource(R.string.peers_connected,
                    peers.filter { it.connectionStatus == "Connected" }.size,
                    peers.size
                )
            )
            Spacer(Modifier.size(16.dp))
        }
        if (peers.isEmpty()) {
            item {
                Box(
                    Modifier.fillParentMaxSize(0.95f),
                    contentAlignment = Alignment.Center
                ) {
                    if (connectionState == null || connectionState == ConnectionState.DISCONNECTED) {
                        Text(stringResource(R.string.connect_to_see_peers))
                    } else {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        items(
            peers,
            key = { peer -> peer.ip.ifEmpty { peer.hashCode() } }
        ) { peer ->
            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .animateItemPlacement()
                    .clickable {
                        selectedPeerIp = peer.ip
                    }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(peer.fqdn)
                        Text(peer.ip)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = when (peer.connectionStatus) {
                                "Connected" -> stringResource(R.string.connected)
                                "Disconnected" -> stringResource(R.string.disconnected)
                                "Connecting" -> stringResource(R.string.connecting)
                                else -> stringResource(R.string.unknown)
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Canvas(
                            Modifier.size(16.dp)
                        ) {
                            drawCircle(
                                color = when (peer.connectionStatus ) {
                                    "Connected" -> JetBirdColors.IndicatorColors.Connected
                                    "Disconnected" -> JetBirdColors.IndicatorColors.Disconnected
                                    "Connecting" -> JetBirdColors.IndicatorColors.Connecting
                                    else -> JetBirdColors.IndicatorColors.Disconnected
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    if (selectedPeerIp != null) {
        val peer = peers.find { it.ip == selectedPeerIp }
        AlertDialog(
            title = { Text(stringResource(R.string.peer_info)) },
            text = {
                Column {
                    Text(stringResource(R.string.peer_ip, peer?.ip.toString()))
                    Text(stringResource(R.string.peer_fqdn, peer?.fqdn.toString()))
                    Text(stringResource(R.string.peer_connection_status, peer?.connectionStatus.toString()))
                    Text(stringResource(R.string.peer_connection_status_update, peer?.connectionStatusUpdate ?: 0))
                    Text(stringResource(R.string.peer_public_key, peer?.publicKey.toString()))
                    Text(stringResource(R.string.peer_relayed, peer?.relayed ?: false))
                    Text(stringResource(R.string.peer_direct, peer?.direct ?: false))
//                    Text(stringResource(R.string.peer_last_wireguard_handshake, peer?.lastWireguardHandshake.orEmpty()))
                    Text(stringResource(R.string.peer_bytes_rx, peer?.bytesRx.toString()))
                    Text(stringResource(R.string.peer_bytes_tx, peer?.bytesTx.toString()))
                    Text(stringResource(R.string.peer_latency, peer?.latency ?: 0))
                    Text(stringResource(R.string.peer_rosenpass, peer?.rosenpassEnabled ?: false))
                    Text(stringResource(R.string.routes, peer?.routes?.joinToString().orEmpty()))
                }
            },
            onDismissRequest = { selectedPeerIp = null },
            confirmButton = {
                Button(
                    onClick = {
                        selectedPeerIp = null
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}
