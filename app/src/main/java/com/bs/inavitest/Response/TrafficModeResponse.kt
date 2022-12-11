package com.bs.inavitest.Response

data class TrafficModeResponse(
    val data: DataXXX,
    val path: String,
    val status: Int,
    val timeStamp: String
)

data class DataXXX(
    val trafficModes: List<TrafficMode>
)

data class TrafficMode(
    var bicycleFlag: Boolean,
    var carFlag: Boolean,
    var childFlag: Boolean,
    var kickboardFlag: Boolean,
    var motorcycleFlag: Boolean,
    var pedestrianFlag: Boolean,
    var trafficCode: String
)