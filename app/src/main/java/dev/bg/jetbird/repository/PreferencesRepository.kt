package dev.bg.jetbird.repository

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

interface PreferencesValues {
    // NetBird
    var managementUrl: String?
    var ssoSupported: Boolean
    var hasAuthed: Boolean
    var preSharedKey: String?
    var setupKey: String?
    var ipv6: Boolean
    var rosenpass: Boolean
    var rosenpassPermissive: Boolean
    var engineLogging: Boolean
    var verboseEngineLogging: Boolean
    var liveEngineLogging: Boolean
    var clientRoutes: Boolean
    var serverRoutes: Boolean
    var forceRelay: Boolean
    var dns: Boolean
    var firewall: Boolean
    var ssh: Boolean
    var blockInbound: Boolean

    // JetBird
    var logging: Boolean
    var verboseLogging: Boolean
    var excludedApps: Set<String>
    var invertExclusion: Boolean
    var routeOverridesEnabled: Boolean
    var routeOverrides: Set<String>
    var fallbackDns: String?
    var startOnReboot: Boolean
}

interface PreferencesRepository: PreferencesValues {
    val state: StateFlow<PreferencesState>

    fun reset()
    fun setProfileId(id: String)
    fun removeProfileKeys(id: String)
    suspend fun updateState()
}

class PreferencesRepositoryImpl @Inject constructor(): PreferencesRepository {
    private val mmkv = MMKV.mmkvWithID(PreferenceKeys.ID.key, MMKV.MULTI_PROCESS_MODE)

    private val _profileId: MutableStateFlow<String> = MutableStateFlow(mmkv.decodeString(PreferenceKeys.Profile.key, "default")!!)
    private val _state: MutableStateFlow<PreferencesState> = MutableStateFlow(this.asState())
    override val state: StateFlow<PreferencesState>
        get() = _state.asStateFlow()

    init {
        // No longer used by NetBird
        if (mmkv.containsKey(PreferenceKeys.LazyConnections.key)) {
            mmkv.remove(PreferenceKeys.LazyConnections.key)
        }
    }

    override fun setProfileId(id: String) {
        _profileId.update { id }
        mmkv.encode(PreferenceKeys.Profile.key, id)
    }

    override fun removeProfileKeys(id: String) {
        mmkv.removeValuesForKeys(
            mmkv
                .allKeys()
                ?.filter { it.startsWith(id) }
                ?.toTypedArray()
        )
    }

    override suspend fun updateState() {
        _state.emit(this.asState())
    }

    // NetBird
    override var managementUrl: String?
        get() = mmkv.decodeString(PreferenceKeys.ManagementUrl.key(), null)
        set(value) { mmkv.encode(PreferenceKeys.ManagementUrl.key(), value) }

    override var ssoSupported: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.SsoSupported.key(), false)
        set(value) { mmkv.encode(PreferenceKeys.SsoSupported.key(), value) }

    override var hasAuthed: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.HasAuthed.key(), false)
        set(value) { mmkv.encode(PreferenceKeys.HasAuthed.key(), value) }

    override var preSharedKey: String?
        get() = mmkv.decodeString(PreferenceKeys.PreSharedKey.key(), null)
        set(value) { mmkv.encode(PreferenceKeys.PreSharedKey.key(), value) }

    override var setupKey: String?
        get() = mmkv.decodeString(PreferenceKeys.SetupKey.key(), null)
        set(value) { mmkv.encode(PreferenceKeys.SetupKey.key(), value) }

    override var ipv6: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.IPv6.key(), true)
        set(value) { mmkv.encode(PreferenceKeys.IPv6.key(), value) }

    override var rosenpass: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.Rosenpass.key(), false)
        set(value) { mmkv.encode(PreferenceKeys.Rosenpass.key(), value) }

    override var rosenpassPermissive: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.RosenpassPermissive.key(), false)
        set(value) { mmkv.encode(PreferenceKeys.RosenpassPermissive.key(), value) }

    override var engineLogging: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.EngineLogging.key, false)
        set(value) {
            mmkv.encode(PreferenceKeys.EngineLogging.key, value)
            if (!value) {
                verboseEngineLogging = false
                liveEngineLogging = false
            }
        }

    override var verboseEngineLogging: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.VerboseEngineLogging.key, false)
        set(value) { mmkv.encode(PreferenceKeys.VerboseEngineLogging.key, value) }

    override var liveEngineLogging: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.LiveEngineLogging.key, false)
        set(value) { mmkv.encode(PreferenceKeys.LiveEngineLogging.key, value) }

    override var clientRoutes: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.ClientRoutes.key(), true)
        set(value) { mmkv.encode(PreferenceKeys.ClientRoutes.key(), value) }

    // NetBird code and default configuration contradict other
    // This should be true on JetBird side 0e5dc9d41229ef3f368aa5feb336934815b69a04#diff-dc3b51d6b4eb84b6bab0fb6f93b1c43e290b8ee615565f77b8af0bb823ac6976R136
    override var serverRoutes: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.ServerRoutes.key(), false)
        set(value) { mmkv.encode(PreferenceKeys.ServerRoutes.key(), value) }

    override var forceRelay: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.ForceRelay.key(), false)
        set(value) { mmkv.encode(PreferenceKeys.ForceRelay.key(), value) }

    override var dns: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.DNS.key(), true)
        set(value) { mmkv.encode(PreferenceKeys.DNS.key(), value) }

    override var firewall: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.Firewall.key(), true)
        set(value) { mmkv.encode(PreferenceKeys.Firewall.key(), value) }

    override var ssh: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.SSH.key(), false)
        set(value) { mmkv.encode(PreferenceKeys.SSH.key(), value) }

    override var blockInbound: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.BlockInbound.key(), false)
        set(value) { mmkv.encode(PreferenceKeys.BlockInbound.key(), value) }

    // JetBird
    override var logging: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.Logging.key, false)
        set(value) {
            mmkv.encode(PreferenceKeys.Logging.key, value)
            if (!value) {
                verboseLogging = false
            }
        }

    override var verboseLogging: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.VerboseLogging.key, false)
        set(value) { mmkv.encode(PreferenceKeys.VerboseLogging.key, value) }

    override var excludedApps: Set<String>
        get() = mmkv.decodeStringSet(PreferenceKeys.ExcludedApps.key(), emptySet<String>())!!.toSet()
        set(value) { mmkv.encode(PreferenceKeys.ExcludedApps.key(), value) }

    override var invertExclusion: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.InvertExclusion.key(), false)
        set(value) { mmkv.encode(PreferenceKeys.InvertExclusion.key(), value) }

    override var routeOverridesEnabled: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.RouteOverridesEnabled.key(), false)
        set(value) { mmkv.encode(PreferenceKeys.RouteOverridesEnabled.key(), value) }

    override var routeOverrides: Set<String>
        get() = mmkv.decodeStringSet(PreferenceKeys.RouteOverrides.key(), emptySet<String>())!!.toSet()
        set(value) { mmkv.encode(PreferenceKeys.RouteOverrides.key(), value) }

    override var fallbackDns: String?
        get() = mmkv.decodeString(PreferenceKeys.FallbackDNS.key(), null)
        set(value) { mmkv.encode(PreferenceKeys.FallbackDNS.key(), value) }

    override var startOnReboot: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.StartOnReboot.key(), false)
        set(value) { mmkv.encode(PreferenceKeys.StartOnReboot.key(), value) }

    override fun reset() {
        mmkv.removeValuesForKeys(mmkv.allKeys())
    }

    private fun asState(): PreferencesState {
        return PreferencesState(
            managementUrl = managementUrl,
            ssoSupported = ssoSupported,
            hasAuthed = hasAuthed,
            preSharedKey = preSharedKey,
            setupKey = setupKey,
            ipv6 = ipv6,
            rosenpass = rosenpass,
            rosenpassPermissive = rosenpassPermissive,
            engineLogging = engineLogging,
            verboseEngineLogging = verboseEngineLogging,
            liveEngineLogging = liveEngineLogging,
            clientRoutes = clientRoutes,
            serverRoutes = serverRoutes,
            forceRelay = forceRelay,
            dns = dns,
            firewall = firewall,
            ssh = ssh,
            blockInbound = blockInbound,
            logging = logging,
            verboseLogging = verboseLogging,
            excludedApps = excludedApps,
            invertExclusion = invertExclusion,
            routeOverridesEnabled = routeOverridesEnabled,
            routeOverrides = routeOverrides,
            fallbackDns = fallbackDns,
            startOnReboot = startOnReboot
        )
    }

    private fun PreferenceKeys.key(): String {
        val profileId = _profileId.value
        val prefix = if (profileId == "default") {
            ""
        } else {
            "${profileId}-"
        }
        return "${prefix}${this.key}"
    }
}

data class PreferencesState(
    // NetBird
    override var managementUrl: String?,
    override var ssoSupported: Boolean,
    override var hasAuthed: Boolean,
    override var preSharedKey: String?,
    override var setupKey: String?,
    override var ipv6: Boolean,
    override var rosenpass: Boolean,
    override var rosenpassPermissive: Boolean,
    override var engineLogging: Boolean,
    override var verboseEngineLogging: Boolean,
    override var liveEngineLogging: Boolean,
    override var clientRoutes: Boolean,
    override var serverRoutes: Boolean,
    override var forceRelay: Boolean,
    override var dns: Boolean,
    override var firewall: Boolean,
    override var ssh: Boolean,
    override var blockInbound: Boolean,

    // JetBird
    override var logging: Boolean,
    override var verboseLogging: Boolean,
    override var excludedApps: Set<String>,
    override var invertExclusion: Boolean,
    override var routeOverridesEnabled: Boolean,
    override var routeOverrides: Set<String>,
    override var fallbackDns: String?,
    override var startOnReboot: Boolean
): PreferencesValues

private enum class PreferenceKeys(
    val key: String
) {
    ID("preferences"),
    Profile("profile"),

    // NetBird
    ManagementUrl("management_url"),
    SsoSupported("sso_supported"),
    HasAuthed("has_authed"),
    PreSharedKey("pre_shared_key"),
    SetupKey("setup_key"),
    LazyConnections("lazy_conn"),
    IPv6("ipv6"),
    Rosenpass("rosenpass"),
    RosenpassPermissive("rosenpass_permissive"),
    EngineLogging("engine_logging"),
    VerboseEngineLogging("verbose_engine_logging"),
    LiveEngineLogging("live_engine_logging"),
    ClientRoutes("client_routes"),
    ServerRoutes("server_routes"),
    ForceRelay("force_relay"),
    DNS("dns"),
    Firewall("firewall"),
    SSH("ssh"),
    BlockInbound("block_inbound"),

    // JetBird
    Logging("logging"),
    VerboseLogging("verbose_logging"),
    ExcludedApps("excluded_apps"),
    InvertExclusion("invert_exclusion"),
    RouteOverridesEnabled("route_overrides_enabled"),
    RouteOverrides("route_overrides"),
    FallbackDNS("fallback_dns"),
    StartOnReboot("start_on_reboot")
}
