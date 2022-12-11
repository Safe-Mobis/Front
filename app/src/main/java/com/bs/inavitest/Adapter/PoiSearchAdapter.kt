package com.bs.inavitest.Adapter

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bs.inavitest.Fragment.MapFragment
import com.bs.inavitest.Fragment.ModeFragment
import com.bs.inavitest.MainActivity
import com.bs.inavitest.R
import com.bs.inavitest.Response.PathResponse
import com.bs.inavitest.Response.Poi
import com.bs.inavitest.databinding.ItemRecyclerBinding
import com.inavi.mapsdk.geometry.LatLng
import com.inavi.mapsdk.geometry.LatLngBounds
import com.inavi.mapsdk.maps.CameraAnimationType
import com.inavi.mapsdk.maps.CameraUpdate
import com.inavi.mapsdk.maps.InaviMap
import com.inavi.mapsdk.style.shapes.*
import kotlinx.coroutines.selects.select
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST
import kotlin.math.roundToInt

class PoiSearchAdapter: RecyclerView.Adapter<PoiSearchAdapter.Holder>() {

    var selectPos = 0
    var poiList: List<Poi>? = null

    lateinit var mainActivity: MainActivity
    lateinit var mapFragment: MapFragment
    lateinit var inaviMap: InaviMap
    lateinit var binding: ItemRecyclerBinding

    val startUTF8UrlEncode = "%EC%B6%9C%EB%B0%9C"
    val endUTF8UrlEncode = "%EB%8F%84%EC%B0%A9"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        binding = ItemRecyclerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    fun selectPoiInit() {
        selectPos = 0
        val poi = poiList?.get(0)
        mapFragment.searchPoiMarkerList[selectPos].iconImage = InvMarkerIcons.BLUE
        onSelectPoi(poi)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val poi = poiList?.get(position)
        holder.setPoi(poi)

        if (selectPos == position) {
            holder.binding.root.setBackgroundColor(Color.parseColor("#FFE0E0E0"))
        } else {
            holder.binding.root.setBackgroundColor(Color.WHITE)
        }

        holder.binding.root.setOnClickListener {
            val beforePos = selectPos
            selectPos = position

            notifyItemChanged(beforePos)
            notifyItemChanged(selectPos)

            mapFragment.searchPoiMarkerList[selectPos].iconImage = InvMarkerIcons.BLUE
            mapFragment.searchPoiMarkerList[beforePos].iconImage = mapFragment.poiMarkerImage

            onSelectPoi(poi)
        }
    }

    fun onSelectPoi(poi: Poi?) {
        val cameraUpdate = CameraUpdate.targetTo(LatLng(poi!!.noorLat.toDouble(), poi.noorLon.toDouble()), 14.0)
        cameraUpdate.animationType = CameraAnimationType.Linear
        cameraUpdate.durationMs = 300
        inaviMap.moveCamera(cameraUpdate)
    }

    override fun getItemCount(): Int {
        return poiList?.size?: 0
    }

    val retrofitTmap = Retrofit.Builder()
        .baseUrl("https://apis.openapi.sk.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val pathService = retrofitTmap.create(PathService::class.java)

    var latLngList = mutableListOf<LatLng>()

    inner class Holder(val binding: ItemRecyclerBinding): RecyclerView.ViewHolder(binding.root) {
        fun setPoi(poi: Poi?) {
            poi?.let {
                binding.textPOIName.text = poi.name
                binding.textPOIAddress.text = poi.newAddressList.newAddress[0].fullAddressRoad
                binding.buttonPathSearch.setOnClickListener{
                    val appKey = mapFragment.appKey
                    val startX = mapFragment.startX
                    val startY = mapFragment.startY
                    val endX = poi.noorLon.toDouble()
                    val endY = poi.noorLat.toDouble()
                    if (ModeFragment.curMode == 0) {
                        pathService.pathCar(appKey, startX!!, startY!!, endX, endY, startUTF8UrlEncode, endUTF8UrlEncode).enqueue(object :Callback<PathResponse> {
                            override fun onResponse(call: Call<PathResponse>, response: Response<PathResponse>) {
                                val path = response.body()
                                latLngList = mutableListOf<LatLng>()
                                var totalDistance = 0
                                var totalTime = 0
                                Log.d("성공", "자동차 ${startX}, ${startY}, ${endX}, ${endY}")
                                for ((i, feature) in path!!.features.withIndex()) {
                                    if (feature.geometry.type == "LineString") {
                                        //Log.d("성공", "${feature.geometry.coordinates}")
                                        val str = feature.geometry.coordinates.toString()
                                        val str2 = str.substring(2, str.length - 2)
                                        //Log.d("성공", str2)
                                        val str3 = str2.split("], [")
                                        var start = 0
                                        if (i != 1) start = 1
                                        for (k in start until str3.size) {
                                            val str4 = str3[k].split(", ")
                                            latLngList.add(LatLng(str4[1].toDouble(), str4[0].toDouble()))
                                        }
                                    } else if (feature.properties.pointType == "S") {
                                        totalDistance = feature.properties.totalDistance
                                        totalTime = feature.properties.totalTime
                                    }
                                }
                                drawPath(totalDistance, totalTime, poi.name)
                            }

                            override fun onFailure(call: Call<PathResponse>, t: Throwable) {
                                Log.e("실패", "${t.localizedMessage}")
                            }
                        })
                    } else {
                        pathService.pathPedestrian(appKey, startX!!, startY!!, endX, endY, startUTF8UrlEncode, endUTF8UrlEncode).enqueue(object :Callback<PathResponse> {
                            override fun onResponse(call: Call<PathResponse>, response: Response<PathResponse>) {
                                val path = response.body()
                                latLngList = mutableListOf<LatLng>()
                                var totalDistance = 0
                                var totalTime = 0
                                Log.d("성공", "보행자 ${path}")
                                for ((i, feature) in path!!.features.withIndex()) {
                                    if (feature.geometry.type == "LineString") {
                                        //Log.d("성공", "${feature.geometry.coordinates}")
                                        val str = feature.geometry.coordinates.toString()
                                        val str2 = str.substring(2, str.length - 2)
                                        //Log.d("성공", str2)
                                        val str3 = str2.split("], [")
                                        var start = 0
                                        if (i != 0) start = 1
                                        for (k in start until str3.size) {
                                            val str4 = str3[k].split(", ")
                                            latLngList.add(LatLng(str4[1].toDouble(), str4[0].toDouble()))
                                        }
                                    } else if (feature.properties.pointType == "SP") {
                                        totalDistance = feature.properties.totalDistance
                                        totalTime = feature.properties.totalTime
                                    }
                                }
                                drawPath(totalDistance, totalTime, poi.name)
                            }

                            override fun onFailure(call: Call<PathResponse>, t: Throwable) {
                                Log.e("실패", "${t.localizedMessage}")
                            }
                        })
                    }
                }
            }
        }
    }

    lateinit var route: InvRoute

    val routePatternImage = InvImage(R.drawable.route_arrow_24dp)

    val startInfoWindow by lazy {
        InvInfoWindow().apply {
            position = LatLng(0.0, 0.0)
            adapter = object : InvInfoWindowTextAdapter(mainActivity) {
                override fun getText(p0: InvInfoWindow): CharSequence {
                    return "출발"
                }
            }
            map = null
        }
    }

    val endInfoWindow by lazy {
        InvInfoWindow().apply {
            position = LatLng(0.0, 0.0)
            adapter = object : InvInfoWindowTextAdapter(mainActivity) {
                override fun getText(p0: InvInfoWindow): CharSequence {
                    return "도착"
                }
            }
            map = null
        }
    }

    fun drawPath(totalDistance: Int, totalTime: Int, destination: String) {

        inaviMap.setPadding(0, 0, 0, 150)

        startInfoWindow.position = latLngList.first()
        startInfoWindow.map = inaviMap
        endInfoWindow.position = latLngList.last()
        endInfoWindow.map = inaviMap

        val latLngBounds = LatLngBounds.Builder().includeAll(latLngList).build()

        val cameraUpdate = CameraUpdate.fitBounds(latLngBounds, 150)
        cameraUpdate.animationType = CameraAnimationType.Linear
        cameraUpdate.durationMs = 300
        inaviMap.moveCamera(cameraUpdate)

        for (marker in mapFragment.searchPoiMarkerList) {
            marker.map = null
        }
        mapFragment.mapOperationCode = 2
        mapFragment.binding.recyclerView.visibility = View.INVISIBLE
        mapFragment.binding.textViewDestination.text = destination
        if (totalDistance >= 1000) {
            val distanceKm = totalDistance / 1000.0
            mapFragment.binding.textViewTotalDistance.text = "${distanceKm.roundToInt()}km"
        } else {
            mapFragment.binding.textViewTotalDistance.text = "${totalDistance}m"
        }
        val totalTimeM = totalTime / 60.0
        val totalTimeMRound = totalTimeM.roundToInt()
        if (totalTimeMRound >= 60) {
            val hour = totalTimeMRound / 60
            val minute = totalTimeMRound % 60
            mapFragment.binding.textViewTotalTime.text = "${hour}시간 ${minute}분"
        } else {
            mapFragment.binding.textViewTotalTime.text = "${totalTimeMRound}분"
        }
        mapFragment.binding.linearLayoutPathResult.visibility = View.VISIBLE
        mapFragment.binding.editTextPOISearch.visibility = View.INVISIBLE

        val link1 = InvRoute.InvRouteLink(latLngList)
        link1.lineColor = Color.parseColor("#FFFF3B49")
        link1.strokeColor = Color.WHITE
        val myLinks = listOf(link1)
        route = InvRoute().apply {
            links = myLinks
            map = inaviMap
        }
        route.lineWidth = 15
        route.strokeWidth = 5
        route.patternImage = routePatternImage
        route.patternMargin = 70
        route.patternScale = 0.7f
    }
}

interface PathService {
    @FormUrlEncoded
    @POST("tmap/routes/pedestrian")
    fun pathPedestrian(@Header("appKey") appKey:String,
                       @Field("startX") startX:Double,
                       @Field("startY") startY:Double,
                       @Field("endX") endX:Double,
                       @Field("endY") endY:Double,
                       @Field("startName") startName:String,
                       @Field("endName") endName:String): Call<PathResponse>

    @FormUrlEncoded
    @POST("tmap/routes")
    fun pathCar(@Header("appKey") appKey:String,
                @Field("startX") startX:Double,
                @Field("startY") startY:Double,
                @Field("endX") endX:Double,
                @Field("endY") endY:Double,
                @Field("startName") startName:String,
                @Field("endName") endName:String): Call<PathResponse>
}