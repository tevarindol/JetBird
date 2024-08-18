package dev.bg.jetbird.data.model

data class CIDR(
    private val route: String
) {
    private val destructured = route.split("/")
    val address = destructured[0]
    val mask = destructured[1].toInt()
}
