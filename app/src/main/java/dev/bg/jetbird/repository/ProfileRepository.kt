package dev.bg.jetbird.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bg.jetbird.data.model.Profile
import dev.bg.jetbird.util.DefaultDispatcher
import dev.bg.jetbird.util.ktx.toProfileList
import io.netbird.android.Android
import io.netbird.android.Preferences
import io.netbird.android.ProfileManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

interface ProfileRepository {
    fun listProfiles(): List<Profile>
    fun activeProfileId(): String
    fun addProfile(
        name: String,
        managementUrl: String,
        onSuccess: () -> Unit,
        onError: (error: String) -> Unit
    )
    fun switchProfile(id: String)
    fun renameProfile(id: String, name: String)
    fun removeProfile(id: String)
}

class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val preferencesRepository: PreferencesRepository,
    private val logRepository: LogRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
): ProfileRepository {

    private val profileManager: ProfileManager = Android.newProfileManager(ctx.filesDir.path)
    private val defaultScope = CoroutineScope(defaultDispatcher + SupervisorJob())

    override fun listProfiles(): List<Profile> = profileManager.listProfiles().toProfileList()

    override fun activeProfileId(): String = profileManager.activeProfile.id

    override fun addProfile(
        name: String,
        managementUrl: String,
        onSuccess: () -> Unit,
        onError: (error: String) -> Unit
    ) {
        // Don't allow same name as JetBird uses that to find the ID
        if (listProfiles().any { it.name == name }) {
            logRepository.log("Cannot add a network with the same name as another network")
            onError("Cannot add a network with the same name as another network")
        }
        try {
            profileManager.addProfile(name)
        } catch (e: Exception) {
            logRepository.log("Failed to add profile: $e")
            onError("Failed to add profile: $e")
            return
        }
        // Update preferences
        setProfileIdFromName(name)
        preferencesRepository.managementUrl = managementUrl
        updatePreferencesState()
        // Update NetBird
        profileManager.switchProfile(name)
        val preferences = Preferences(profileManager.activeConfigPath)
        preferences.managementURL = managementUrl
        preferences.commit()
    }

    override fun switchProfile(id: String) {
        // Caller needs to stop tun
        try {
            profileManager.switchProfile(id)
        } catch (e: Exception) {
            logRepository.log("Failed to switch profile: $e")
        }
        preferencesRepository.setProfileId(id)
        updatePreferencesState()
    }

    override fun renameProfile(id: String, name: String) {
        try {
            profileManager.renameProfile(id, name)
        } catch (e: Exception) {
            logRepository.log("Failed to rename profile: $e")
        }
    }

    override fun removeProfile(id: String) {
        preferencesRepository.setProfileId("default")
        profileManager.switchProfile("default")
        try {
            profileManager.removeProfile(id)
        } catch (e: Exception) {
            logRepository.log("Failed to remove profile: $e")
        }
        preferencesRepository.removeProfileKeys(id)
    }

    private fun setProfileIdFromName(name: String) {
        val id = listProfiles().find { it.name == name }?.id
        if (id != null) {
            preferencesRepository.setProfileId(id)
        } else {
            logRepository.log("setProfileIdFromName: failed to find profile with name")
        }
    }

    private fun updatePreferencesState() {
        defaultScope.launch {
            preferencesRepository.updateState()
        }
    }

}
