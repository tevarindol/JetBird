package dev.bg.jetbird.ui.screens.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
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
import dev.bg.jetbird.service.VPNService
import dev.bg.jetbird.ui.components.ConnectionCard
import dev.bg.jetbird.ui.components.OutlinedCopyButton
import dev.bg.jetbird.util.OnLifecycleEvent
import dev.bg.jetbird.util.ktx.hasOtherAlwaysOnVpn
import io.netbird.android.URLOpener
import kotlinx.coroutines.launch

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

    val loginListener = object : URLOpener {
        override fun onLoginSuccess() {}

        override fun open(uri: String?, userCode: String?) {
            uri?.let {
                uriHandler.openUri(it)
            }
        }
    }

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
                    hasVpnPermission -> {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Button(
                                modifier = Modifier.size(256.dp),
                                shape = RoundedCornerShape(16.dp),
                                onClick = {
                                    if (vpnState?.connectionState == ConnectionState.DISCONNECTED) {
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
                                        binder?.stop()
                                    }
                                },
                                enabled = vpnState?.connectionState == ConnectionState.CONNECTED || vpnState?.connectionState == ConnectionState.DISCONNECTED
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_launcher_monochrome),
                                    modifier = Modifier.fillMaxSize(),
                                    contentDescription = if (vpnState?.connectionState == ConnectionState.CONNECTED) {
                                        stringResource(R.string.connected)
                                    } else {
                                        stringResource(R.string.disconnect)
                                    }
                                )
                            }
                            Spacer(Modifier.size(36.dp))
                            AnimatedContent(
                                vpnState?.connectionState,
                                label = "Connection state animation"
                            ) { connectionState ->
                                Text(
                                    text = when (connectionState) {
                                        ConnectionState.CONNECTED -> stringResource(R.string.connected)
                                        ConnectionState.CONNECTING -> stringResource(R.string.connecting)
                                        ConnectionState.DISCONNECTING -> stringResource(R.string.disconnecting)
                                        ConnectionState.DISCONNECTED -> stringResource(R.string.disconnected)
                                        else -> stringResource(R.string.disconnected)
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 24.sp
                                )
                            }
                        }
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

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun PeersList(
    lazyListState: LazyListState
) {
    val ctx = LocalContext.current
    val binder = LocalVPNServiceBinder.current
    val clipboardManager = LocalClipboardManager.current

    val service = binder?.state?.collectAsStateWithLifecycle()?.value
    val connectionState = service?.connectionState
    val peers = service?.peers ?: emptyList()
    val networks = service?.networks ?: emptyList()

    var selectedPeerIp by remember { mutableStateOf<String?>(null) }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.peers_connected,
                    peers.filter { it.connectionStatus == "Connected" }.size,
                    peers.size
                )
            )
            Spacer(Modifier.size(16.dp))
        }
        PrimaryTabRow(
            selectedTabIndex = tab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Tab(
                selected = tab == 0,
                onClick = {
                    tab = 0
                },
                text = {
                    Text(stringResource(R.string.peers))
                }
            )
            Tab(
                selected = tab == 1,
                onClick = {
                    tab = 1
                },
                text = {
                    Text(stringResource(R.string.networks))
                }
            )
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
            when {
                tab == 0 -> {
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
                    } else {
                        item {
                            Spacer(Modifier.height(4.dp))
                        }
                        items(
                            peers,
                            key = { peer -> peer.ip.ifEmpty { peer.hashCode() } }
                        ) { peer ->
                            ConnectionCard(
                                title = peer.fqdn,
                                description = peer.ip,
                                connectionStatus = peer.connectionStatus,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .animateItem()
                                    .clickable {
                                        selectedPeerIp = peer.ip
                                    }
                            )
                        }
                    }
                }
                else -> {
                    if (networks.isEmpty()) {
                        item {
                            Box(
                                Modifier.fillParentMaxSize(0.95f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (connectionState == null || connectionState == ConnectionState.DISCONNECTED) {
                                    Text(stringResource(R.string.connect_to_see_networks))
                                } else {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    } else {
                        item {
                            Spacer(Modifier.height(8.dp))
                        }
                        items(networks) { network ->
                            ConnectionCard(
                                title = network.name,
                                description = network.network,
                                connectionStatus = network.status,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .animateItem()
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
                SelectionContainer {
                    Column {
                        Text(stringResource(R.string.peer_ip, peer?.ip.toString()))
                        Text(stringResource(R.string.peer_fqdn, peer?.fqdn.toString()))
                        Text(stringResource(R.string.peer_connection_status, peer?.connectionStatus.toString()))
                        Text(stringResource(R.string.peer_connection_status_update, peer?.connectionStatusUpdate ?: 0))
                        Text(stringResource(R.string.peer_public_key, peer?.publicKey.toString()))
                        Text(stringResource(R.string.peer_relayed, peer?.relayed ?: false))
                        Text(stringResource(R.string.relay, peer?.relayServerAddress.toString()))
//                    Text(stringResource(R.string.peer_last_wireguard_handshake, peer?.lastWireguardHandshake.orEmpty()))
                        Text(stringResource(R.string.peer_bytes_rx, peer?.bytesRx.toString()))
                        Text(stringResource(R.string.peer_bytes_tx, peer?.bytesTx.toString()))
                        Text(stringResource(R.string.peer_latency, peer?.latency ?: 0))
                        Text(stringResource(R.string.peer_rosenpass, peer?.rosenpassEnabled ?: false))
//                        Text(stringResource(R.string.routes, peer?.routes?.joinToString().orEmpty()))
                    }
                }
            },
            onDismissRequest = { selectedPeerIp = null },
            dismissButton = {
                OutlinedCopyButton {
                    clipboardManager.setText(
                        buildAnnotatedString {
                            append("${ctx.getString(R.string.peer_ip, peer?.ip.toString())}\n")
                            append("${ctx.getString(R.string.peer_fqdn, peer?.fqdn.toString())}\n")
                            append("${ctx.getString(R.string.peer_connection_status, peer?.connectionStatus.toString())}\n")
                            append("${ctx.getString(R.string.peer_connection_status_update, peer?.connectionStatusUpdate ?: 0)}\n")
                            append("${ctx.getString(R.string.peer_public_key, peer?.publicKey.toString())}\n")
                            append("${ctx.getString(R.string.peer_relayed, peer?.relayed ?: false)}\n")
                            append("${ctx.getString(R.string.relay, peer?.relayServerAddress.toString())}\n")
                            append("${ctx.getString(R.string.peer_bytes_rx, peer?.bytesRx.toString())}\n")
                            append("${ctx.getString(R.string.peer_bytes_tx, peer?.bytesTx.toString())}\n")
                            append("${ctx.getString(R.string.peer_latency, peer?.latency ?: 0)}\n")
                            append("${ctx.getString(R.string.peer_rosenpass, peer?.rosenpassEnabled ?: false)}\n")
//                            append(ctx.getString(R.string.routes, peer?.routes?.joinToString().orEmpty()))
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedPeerIp = null
                    }
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
