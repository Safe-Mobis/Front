package com.bs.inavitest.Request

data class SavePathRequest(
    val route: List<Route>
)

data class Route(
    val latitude: Double,
    val longitude: Double
)