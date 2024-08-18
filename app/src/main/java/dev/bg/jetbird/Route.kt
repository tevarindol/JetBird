package dev.bg.jetbird

import androidx.compose.runtime.Immutable

enum class ScaffoldVisibility {
    Gone,
    Top,
    NavOnly,
    ActionsOnly
}

@Immutable
sealed class Route(
    val route: String
) {
    data object Home: Route("home")
    data object Setup: Route("setup")
    data object Log: Route("log")
    data object Settings: Route("settings")
    data object RouteSelector: Route("route_selector")
    data object Credits: Route("credits")

    companion object {
        fun getScaffoldVisibility(route: String?): ScaffoldVisibility = when (route) {
            Home.route -> ScaffoldVisibility.ActionsOnly
            Setup.route -> ScaffoldVisibility.Gone
            Log.route -> ScaffoldVisibility.NavOnly
            Settings.route -> ScaffoldVisibility.NavOnly
            RouteSelector.route -> ScaffoldVisibility.NavOnly
            Credits.route -> ScaffoldVisibility.NavOnly
            else -> ScaffoldVisibility.Gone
        }
    }
}
