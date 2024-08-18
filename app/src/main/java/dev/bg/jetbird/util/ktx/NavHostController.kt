package dev.bg.jetbird.util.ktx

import androidx.navigation.NavHostController
import dev.bg.jetbird.Route

fun NavHostController.navigate(route: Route) {
    return this.navigate(route.route)
}

fun NavHostController.clearAndNavigate(route: Route) {
    return this.navigate(route.route) {
        popUpTo(0)
    }
}
