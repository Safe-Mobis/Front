package com.bs.inavitest.Response

data class PoiSearchResponse(
    val searchPoiInfo: SearchPoiInfo
)

data class SearchPoiInfo(
    val count: String,
    val page: String,
    val pois: Pois,
    val totalCount: String
)

data class Pois(
    val poi: List<Poi>
)

data class Poi(
    val bizName: String,
    val collectionType: String,
    val dataKind: String,
    val desc: String,
    val detailAddrName: String,
    val detailBizName: String,
    val detailInfoFlag: String,
    val evChargers: EvChargers,
    val firstBuildNo: String,
    val firstNo: String,
    val frontLat: String,
    val frontLon: String,
    val id: String,
    val lowerAddrName: String,
    val lowerBizName: String,
    val middleAddrName: String,
    val middleBizName: String,
    val mlClass: String,
    val name: String,
    val navSeq: String,
    val newAddressList: NewAddressList,
    val noorLat: String,
    val noorLon: String,
    val parkFlag: String,
    val pkey: String,
    val radius: String,
    val roadName: String,
    val rpFlag: String,
    val secondBuildNo: String,
    val secondNo: String,
    val telNo: String,
    val upperAddrName: String,
    val upperBizName: String,
    val zipCode: String
)

data class NewAddressList(
    val newAddress: List<NewAddres>
)

data class NewAddres(
    val bldNo1: String,
    val bldNo2: String,
    val centerLat: String,
    val centerLon: String,
    val frontLat: String,
    val frontLon: String,
    val fullAddressRoad: String,
    val roadId: String,
    val roadName: String
)

data class EvChargers(
    val evCharger: List<Any>
)
