package com.bs.inavitest.Response

data class TrafficCodeResponse(
    val data: DataXXXX,
    val path: String,
    val status: Int,
    val timeStamp: String
)

data class DataXXXX(
    val trafficCode: String
)