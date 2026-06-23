package dev.bg.jetbird.ui.screens.profile

import dev.bg.jetbird.data.model.Profile

class ProfileContract {

    data class State(
        val profiles: List<Profile>?
    )

}
