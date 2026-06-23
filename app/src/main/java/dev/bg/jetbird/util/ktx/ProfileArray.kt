package dev.bg.jetbird.util.ktx

import dev.bg.jetbird.data.model.Profile
import io.netbird.android.ProfileArray

fun ProfileArray?.toProfileList(): List<Profile> {
    if (this == null) return emptyList()
    val profiles = mutableListOf<Profile>()
    for (i in 0..<length()) {
        val profile = this[i]
        profiles.add(
            Profile(
                id = profile.id,
                name = profile.name,
                isActive = profile.isActive
            )
        )
    }
    return profiles
}
