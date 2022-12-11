package com.bs.inavitest.Response

data class BasicResponse(
    val data: Data,
    val path: String,
    val status: Int,
    val error: String,
    val message: String,
    val timeStamp: String
)

data class Data(
    val accessToken: String,
    val grantType: String,
    val refreshToken: String
)
