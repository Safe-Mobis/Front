package com.bs.inavitest.Response

data class IntersectionsResponse(
    val data: DataX,
    val path: String,
    val status: Int,
    val timeStamp: String
)

data class DataX(
    val intersections: List<Intersection>
)

data class Intersection(
    val intersectionId: Int,
    val position: Position
)

data class Position(
    val latitude: Double,
    val longitude: Double
)
