package dev.bg.jetbird.repository

import com.tencent.mmkv.MMKV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

interface PreferencesValues {
    // NetBird
    var managementUrl: String?
    var ssoSupported: Boolean
    var hasAuthed: Boolean
    var preSharedKey: String?
    var setupKey: String?
    var rosenpass: Boolean
    var rosenpassPermissive: Boolean
    var engineLogging: Boolean
    var verboseEngineLogging: Boolean
    var liveEngineLogging: Boolean

    // JetBird
    var logging: Boolean
    var verboseLogging: Boolean
    var excludedApps: Set<String>
    var routeOverridesEnabled: Boolean
    var routeOverrides: Set<String>
    var fallbackDns: String?
    var reconnectOnNewRoutes: Boolean
}

interface PreferencesRepository: PreferencesValues {
    val state: StateFlow<PreferencesState>

    fun reset()
    suspend fun updateState()
}

class PreferencesRepositoryImpl @Inject constructor(): PreferencesRepository {
    private val mmkv = MMKV.mmkvWithID(PreferenceKeys.ID.key, MMKV.MULTI_PROCESS_MODE)

    private val _state: MutableStateFlow<PreferencesState> = MutableStateFlow(this.asState())
    override val state: StateFlow<PreferencesState>
        get() = _state.asStateFlow()

    override suspend fun updateState() {
        _state.emit(this.asState())
    }

    // NetBird
    override var managementUrl: String?
        get() = mmkv.decodeString(PreferenceKeys.ManagementUrl.key, null)
        set(value) { mmkv.encode(PreferenceKeys.ManagementUrl.key, value) }

    override var ssoSupported: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.SsoSupported.key, false)
        set(value) { mmkv.encode(PreferenceKeys.SsoSupported.key, value) }

    override var hasAuthed: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.HasAuthed.key, false)
        set(value) { mmkv.encode(PreferenceKeys.HasAuthed.key, value) }

    override var preSharedKey: String?
        get() = mmkv.decodeString(PreferenceKeys.PreSharedKey.key, null)
        set(value) { mmkv.encode(PreferenceKeys.PreSharedKey.key, value) }

    override var setupKey: String?
        get() = mmkv.decodeString(PreferenceKeys.SetupKey.key, null)
        set(value) { mmkv.encode(PreferenceKeys.SetupKey.key, value) }

    override var rosenpass: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.Rosenpass.key, false)
        set(value) {mmkv.encode(PreferenceKeys.Rosenpass.key, value) }

    override var rosenpassPermissive: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.RosenpassPermissive.key, false)
        set(value) { mmkv.encode(PreferenceKeys.RosenpassPermissive.key, value) }

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
        get() = mmkv.decodeStringSet(PreferenceKeys.ExcludedApps.key, emptySet<String>())!!.toSet()
        set(value) { mmkv.encode(PreferenceKeys.ExcludedApps.key, value) }

    override var routeOverridesEnabled: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.RouteOverridesEnabled.key, false)
        set(value) { mmkv.encode(PreferenceKeys.RouteOverridesEnabled.key, value) }

    override var routeOverrides: Set<String>
        get() = mmkv.decodeStringSet(PreferenceKeys.RouteOverrides.key, emptySet<String>())!!.toSet()
        set(value) { mmkv.encode(PreferenceKeys.RouteOverrides.key, value) }

    override var fallbackDns: String?
        get() = mmkv.decodeString(PreferenceKeys.FallbackDNS.key, null)
        set(value) { mmkv.encode(PreferenceKeys.FallbackDNS.key, value) }

    override var reconnectOnNewRoutes: Boolean
        get() = mmkv.decodeBool(PreferenceKeys.ReconnectOnNewRoutes.key, false)
        set(value) { mmkv.encode(PreferenceKeys.ReconnectOnNewRoutes.key, value) }

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
            rosenpass = rosenpass,
            rosenpassPermissive = rosenpassPermissive,
            engineLogging = engineLogging,
            verboseEngineLogging = verboseEngineLogging,
            liveEngineLogging = liveEngineLogging,
            logging = logging,
            verboseLogging = verboseLogging,
            excludedApps = excludedApps,
            routeOverridesEnabled = routeOverridesEnabled,
            routeOverrides = routeOverrides,
            fallbackDns = fallbackDns,
            reconnectOnNewRoutes = reconnectOnNewRoutes
        )
    }
}

data class PreferencesState(
    // NetBird
    override var managementUrl: String?,
    override var ssoSupported: Boolean,
    override var hasAuthed: Boolean,
    override var preSharedKey: String?,
    override var setupKey: String?,
    override var rosenpass: Boolean,
    override var rosenpassPermissive: Boolean,
    override var engineLogging: Boolean,
    override var verboseEngineLogging: Boolean,
    override var liveEngineLogging: Boolean,

    // JetBird
    override var logging: Boolean,
    override var verboseLogging: Boolean,
    override var excludedApps: Set<String>,
    override var routeOverridesEnabled: Boolean,
    override var routeOverrides: Set<String>,
    override var fallbackDns: String?,
    override var reconnectOnNewRoutes: Boolean
): PreferencesValues

private enum class PreferenceKeys(
    val key: String
) {
    ID("preferences"),

    // NetBird
    ManagementUrl("management_url"),
    SsoSupported("sso_supported"),
    HasAuthed("has_authed"),
    PreSharedKey("pre_shared_key"),
    SetupKey("setup_key"),
    Rosenpass("rosenpass"),
    RosenpassPermissive("rosenpass_permissive"),
    EngineLogging("engine_logging"),
    VerboseEngineLogging("verbose_engine_logging"),
    LiveEngineLogging("live_engine_logging"),

    // JetBird
    Logging("logging"),
    VerboseLogging("verbose_logging"),
    ExcludedApps("excluded_apps"),
    RouteOverridesEnabled("route_overrides_enabled"),
    RouteOverrides("route_overrides"),
    FallbackDNS("fallback_dns"),
    ReconnectOnNewRoutes("reconnect_new_routes")
}
