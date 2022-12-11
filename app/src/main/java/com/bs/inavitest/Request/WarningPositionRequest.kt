package com.bs.inavitest.Request

data class WarningPositionRequest(
    val intersectionId: Int,
    val latitude: Double,
    val longitude: Double,
    val warningCode: String
)
