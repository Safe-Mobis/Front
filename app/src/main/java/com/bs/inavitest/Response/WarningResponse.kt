package com.bs.inavitest.Response

data class WarningResponse(
    val data: DataXX,
    val path: String,
    val status: Int,
    val timeStamp: String
)

data class DataXX(
    val latitude: Double,
    val longitude: Double,
    val trafficCode: String,
    val warningCode: String
)
