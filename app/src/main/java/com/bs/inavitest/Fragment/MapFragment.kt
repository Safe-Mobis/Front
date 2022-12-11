package com.bs.inavitest.Fragment

import android.content.Context
import android.graphics.PointF
import android.location.Location
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bs.inavitest.Adapter.PathService
import com.bs.inavitest.Adapter.PoiSearchAdapter
import com.bs.inavitest.DataClass.IntersectionsInfo
import com.bs.inavitest.MainActivity
import com.bs.inavitest.R
import com.bs.inavitest.Request.Route
import com.bs.inavitest.Request.SavePathRequest
import com.bs.inavitest.Request.WarningPositionRequest
import com.bs.inavitest.Response.*
import com.bs.inavitest.databinding.FragmentMapBinding
import com.inavi.mapsdk.geometry.LatLng
import com.inavi.mapsdk.maps.*
import com.inavi.mapsdk.style.shapes.*
import org.w3c.dom.Text
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.*
import kotlin.concurrent.timer

class MapFragment : Fragment() {

    lateinit var binding: FragmentMapBinding
    lateinit var mainActivity: MainActivity

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 10000
        var calculationTime = 6.0
    }

    private val fusedLocationProvider by lazy {
        FusedLocationProvider(this, LOCATION_PERMISSION_REQUEST_CODE)
    }

    lateinit var inaviMap: InaviMap

    var mapOperationCode = 0

    val adapter = PoiSearchAdapter()

    val appKey = "l7xxd0a7c920839f4260840b5f1eaa55944c"
    var startX: Double? = 0.0
    var startY: Double? = 0.0
    val page = 1
    val count = 30
    var searchPoiMarkerList = mutableListOf<InvMarker>()
    val poiMarkerImage = InvImage(R.drawable.ic_baseline_circle_24)
    val trackingCompassLocationIconImage = InvImage(R.drawable.inv_marker_route_flat_50dp)
    val intersectionMarkerImage = InvImage(R.drawable.intersection_marker_24)

    val mapClickInfo by lazy {
        InvInfoWindow().apply {
            position = LatLng(0.0, 0.0)
            adapter = object : InvInfoWindowTextAdapter(mainActivity) {
                override fun getText(p0: InvInfoWindow): CharSequence {
                    return "목적지\n 설정"
                }
            }
            map = null
        }
    }

    val oppMarkerCarLeft = InvImage(R.drawable.traffic_code_car_blue_left_45)
    val oppMarkerCarRight = InvImage(R.drawable.traffic_code_car_blue_45)
    val oppMarkerPedestrianLeft = InvImage(R.drawable.traffic_code_pedestrian_blue_left_45)
    val oppMarkerPedestrianRight = InvImage(R.drawable.traffic_code_pedestrian_blue_45)


    val oppMarker by lazy {
        InvMarker().apply {
            position = LatLng(0.0, 0.0)
            anchor = PointF(0.5f, 0.5f)
            map = null
        }
    }

    val retrofitTmap = Retrofit.Builder()
        .baseUrl("https://apis.openapi.sk.com")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val searchPoiService = retrofitTmap.create(SearchPoiService::class.java)

    val pathService = retrofitTmap.create(PathService::class.java)

    val retrofitServer = Retrofit.Builder()
        .baseUrl("http://43.201.11.119:8080")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val savePathService = retrofitServer.create(SavePathService::class.java)

    val getIntersectionsService = retrofitServer.create(GetIntersectionsService::class.java)

    val warningPositionService = retrofitServer.create(WarningPositionService::class.java)

    val warningService = retrofitServer.create(WaningService::class.java)

    var prevLocation = Location("prev")
    var prevTime = 0L
    var speed = 0.0
    var navigationStart = false
    var warningCnt = 0
    var collapsed = false
//    var collapsedCnt = 0

    lateinit var tts: TextToSpeech
    val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    lateinit var ringtone: Ringtone

    private val callback = OnMapReadyCallback { initMap ->
        inaviMap = initMap.apply {
            uiSettings.isCompassVisible = true
            uiSettings.isScaleBarVisible = true
            uiSettings.isZoomControlVisible = true
            uiSettings.isLocationButtonVisible = false

            locationProvider = fusedLocationProvider
            userTrackingMode = UserTrackingMode.Tracking
        }

        inaviMap.locationIcon.imageTrackingCompass = trackingCompassLocationIconImage

        inaviMap.addOnCameraChangeListener { reason ->
            when(reason) {
                CameraUpdate.UPDATE_REASON_GESTURE -> inaviMap.userTrackingMode = UserTrackingMode.NoTracking
            }
        }

        inaviMap.addOnLocationChangedListener { location ->
            startX = location?.longitude
            startY = location?.latitude

            if (mapOperationCode == 3 && location != null) {
                if (navigationStart) {
                    prevTime = SystemClock.elapsedRealtime()
                    prevLocation.latitude = location.latitude
                    prevLocation.longitude = location.longitude
                    navigationStart = false
                } else {
                    val curTime = SystemClock.elapsedRealtime()
                    val dTime = (curTime - prevTime).toDouble() / 1000.0
                    val curLocation = Location("cur")
                    curLocation.latitude = location.latitude
                    curLocation.longitude = location.longitude
                    val dLocation = prevLocation.distanceTo(curLocation).toDouble()
                    //speed = 10.0
                    speed = dLocation / dTime
                    //Log.d("위치 변경", "${speed}")
                    prevTime = curTime
                    prevLocation.latitude = curLocation.latitude
                    prevLocation.longitude = curLocation.longitude

                    warningPositionCheck()
                }
            }
        }

        inaviMap.setOnMapClickListener { pointF, latLng ->
            if (mapOperationCode == 0 || mapOperationCode == 1)
                mapClickInfo.position = latLng
            if (mapClickInfo.map == null)
                mapClickInfo.map = inaviMap
        }

        mapClickInfo.setOnClickListener {
            binding.buttonMyLocation.visibility = View.INVISIBLE
            mapClickInfo.map = null
            val appKey = appKey
            val endX = mapClickInfo.position.longitude
            val endY = mapClickInfo.position.latitude
            if (ModeFragment.curMode == 0) {
                pathService.pathCar(appKey, startX!!, startY!!, endX, endY, adapter.startUTF8UrlEncode, adapter.endUTF8UrlEncode).enqueue(object :Callback<PathResponse> {
                    override fun onResponse(call: Call<PathResponse>, response: Response<PathResponse>) {
                        val path = response.body()
                        adapter.latLngList = mutableListOf<LatLng>()
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
                                    adapter.latLngList.add(LatLng(str4[1].toDouble(), str4[0].toDouble()))
                                }
                            } else if (feature.properties.pointType == "S") {
                                totalDistance = feature.properties.totalDistance
                                totalTime = feature.properties.totalTime
                            }
                        }
                        adapter.drawPath(totalDistance, totalTime, "사용자 지정 목적지")
                    }

                    override fun onFailure(call: Call<PathResponse>, t: Throwable) {
                        Log.e("실패", "${t.localizedMessage}")
                    }
                })
            } else {
                pathService.pathPedestrian(appKey, startX!!, startY!!, endX, endY, adapter.startUTF8UrlEncode, adapter.endUTF8UrlEncode).enqueue(object :Callback<PathResponse> {
                    override fun onResponse(call: Call<PathResponse>, response: Response<PathResponse>) {
                        val path = response.body()
                        adapter.latLngList = mutableListOf<LatLng>()
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
                                    adapter.latLngList.add(LatLng(str4[1].toDouble(), str4[0].toDouble()))
                                }
                            } else if (feature.properties.pointType == "SP") {
                                totalDistance = feature.properties.totalDistance
                                totalTime = feature.properties.totalTime
                            }
                        }
                        adapter.drawPath(totalDistance, totalTime, "사용자 지정 목적지")
                    }

                    override fun onFailure(call: Call<PathResponse>, t: Throwable) {
                        Log.e("실패", "${t.localizedMessage}")
                    }
                })
            }
            true
        }

        adapter.mainActivity = mainActivity
        adapter.mapFragment = this
        adapter.inaviMap = inaviMap
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (fusedLocationProvider.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            return
        }

        //mainActivity.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) mainActivity = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        InaviMapSdk.getInstance(mainActivity).authFailureCallback =
        InaviMapSdk.AuthFailureCallback { errCode: Int, msg: String ->
            Log.d("인증", "인증 실패")
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as InvMapFragment
        mapFragment.getMapAsync(callback)

        tts = TextToSpeech(mainActivity) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.d("티티에스", "언어 선택")
                val result = tts.setLanguage(Locale.KOREAN)

                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.d("티티에스", "지원하지 않는 언어")
                }
            }
        }

        ringtone = RingtoneManager.getRingtone(mainActivity, notification)

        binding.buttonMyLocation.setOnClickListener {
            if (mapOperationCode != 3)
                inaviMap.userTrackingMode = UserTrackingMode.Tracking
            else {
                inaviMap.userTrackingMode = UserTrackingMode.TrackingCompass
            }
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(mainActivity)

        binding.imageViewArrowLeft.setImageResource(R.drawable.collapse_arrow_left_80)
        binding.imageViewArrowRight.setImageResource(R.drawable.collapse_arrow_right_80)
        binding.imageViewArrowLeft.visibility = View.INVISIBLE
        binding.imageViewArrowRight.visibility = View.INVISIBLE

        binding.editTextPOISearch.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchKeyword = textView.text.toString()
                searchPoiService.searchPoi(1, page, count, searchKeyword, appKey).enqueue(object: Callback<PoiSearchResponse> {
                    override fun onResponse(call: Call<PoiSearchResponse>, response: Response<PoiSearchResponse>) {
                        val result = response.body()
                        if (result == null) {
                            Toast.makeText(mainActivity, "검색 결과가 없습니다", Toast.LENGTH_SHORT).show()
                        } else {
                            if (searchPoiMarkerList.isNotEmpty()) {
                                for (marker in searchPoiMarkerList) {
                                    marker.map = null
                                }
                            }
                            searchPoiMarkerList = mutableListOf<InvMarker>()
                            for (poi in result.searchPoiInfo.pois.poi) {
                                val marker = InvMarker().apply {
                                    position = LatLng(poi.noorLat.toDouble(), poi.noorLon.toDouble())
                                    iconImage = poiMarkerImage
                                    map = inaviMap
                                }
                                searchPoiMarkerList.add(marker)
                            }
                            adapter.poiList = result.searchPoiInfo.pois.poi
                            adapter.notifyDataSetChanged()
                            binding.recyclerView.visibility = RecyclerView.VISIBLE
                            binding.buttonMyLocation.visibility = View.INVISIBLE
                            inaviMap.userTrackingMode = UserTrackingMode.NoTracking
                            inaviMap.setPadding(0, 0, 0, 300)
                            adapter.selectPoiInit()
                            mapOperationCode = 1
                        }
                    }

                    override fun onFailure(call: Call<PoiSearchResponse>, t: Throwable) {
                        Log.e("연결 실패", "${t.localizedMessage}")
                    }
                })
                val inputMethodManager = mainActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.editTextPOISearch.windowToken, 0)
            }
            true
        }

        binding.buttonNavigation.setOnClickListener{
            mapOperationCode = 3
            binding.linearLayoutPathResult.visibility = View.INVISIBLE
            binding.buttonMyLocation.visibility = View.VISIBLE

            inaviMap.setPadding(0, 500, 0, 0)
            adapter.startInfoWindow.map = null
            adapter.endInfoWindow.map = null

            val cameraPosition = CameraPosition(
                inaviMap.cameraPosition.target,
                18.0,
                70.0,
                inaviMap.cameraPosition.bearing
            )
            inaviMap.cameraPosition = cameraPosition
            inaviMap.userTrackingMode = UserTrackingMode.TrackingCompass

            val pointList = mutableListOf<Route>()
            for ((i, point) in adapter.latLngList.withIndex()) {
                pointList.add(Route(point.latitude, point.longitude))
            }

            val savePathRequest = SavePathRequest(pointList)
            savePathService.savePath(accessToken = MainActivity.accessToken, savePathRequest).enqueue(object: Callback<BasicResponse> {
                override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                    val result = response.body()
                    if (result?.status == 201) {
                        Log.d("경로 저장", "경로 저장 성공 ${savePathRequest}")
                        navigationStart = true
                    } else {
                        Log.d("경로 저장", "${result}")
                    }
                }

                override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                    Log.e("연결 실패", "${t.localizedMessage}")
                }
            })
            timerGetIntersections()
        }
    }

    var timerTaskIntersection: Timer? = null
    var timerTaskWarning: Timer? = null

    var intersectionInfoList = mutableListOf<IntersectionsInfo>()
    var intersectoinIdList = mutableListOf<Int>()

    fun timerGetIntersections() {
        timerTaskIntersection = timer(period = 10000)
        {
            getIntersectionsService.getIntersections(accessToken = MainActivity.accessToken)
                .enqueue(object : Callback<IntersectionsResponse> {
                    override fun onResponse(
                        call: Call<IntersectionsResponse>,
                        response: Response<IntersectionsResponse>
                    ) {
                        val result = response.body()
                        if (result?.status == 200) {
                            Log.d("교차점", "${result}")
                            drawIntersections(result.data.intersections)
                        } else {
                            Log.d("교차점", "실패")
                        }
                    }

                    override fun onFailure(call: Call<IntersectionsResponse>, t: Throwable) {
                        Log.e("연결 실패", "${t.localizedMessage}")
                    }
                })
        }

        timerTaskWarning = timer(period = 250)
        {
            if (warningCnt > 0) {
                for (intersection in intersectionInfoList) {
                    if (intersection.warningCode == "WARN") {
                        warningService.getWarning(accessToken = MainActivity.accessToken, intersectioinId = intersection.intersection.intersectionId).enqueue(object: Callback<WarningResponse> {
                            override fun onResponse(call: Call<WarningResponse>, response: Response<WarningResponse>){
                                val result = response.body()
                                if (result?.status == 200) {
                                    Log.d("충돌", "${result}")
                                    oppMarker.position = LatLng(result.data.latitude, result.data.longitude)
                                    collapsedNotify(result.data, intersection.intersection)
                                } else {
                                    Log.d("충돌", "${result}")
                                }
                            }

                            override fun onFailure(call: Call<WarningResponse>, t: Throwable) {
                                Log.e("연결 실패", "${t.localizedMessage}")
                            }
                        })
                    }
                }
            } else {
                oppMarker.map = null
                collapsed = false
                ringtone.stop()
                binding.viewWarning.visibility = View.INVISIBLE
                binding.linearLayoutWarningDirection.visibility = View.INVISIBLE
            }
        }
    }

    fun collapsedNotify(data: DataXX, intersection: Intersection) {
        if (!collapsed && data.warningCode == "WARN") {
//            collapsedCnt += 1
            Log.d("충돌", "충돌 경고")
            collapsed = true
            ringtone.play()
            binding.viewWarning.visibility = View.VISIBLE
            val intersectionLocation = Location("intersection")
            intersectionLocation.latitude = intersection.position.latitude
            intersectionLocation.longitude = intersection.position.longitude
            val myAngle = prevLocation.bearingTo(intersectionLocation)
            val opponentLocation = Location("opponent")
            opponentLocation.latitude = data.latitude
            opponentLocation.longitude = data.longitude
            val opponentAngle = opponentLocation.bearingTo(intersectionLocation)
            val dAngle = opponentAngle - myAngle
            var relativeAngle = dAngle
            if (dAngle < 0)
                relativeAngle += 360
            binding.linearLayoutWarningDirection.visibility = View.VISIBLE
            if (relativeAngle > 0 && relativeAngle < 180) {
                binding.imageViewArrowRight.visibility = ImageView.INVISIBLE
                binding.imageViewArrowLeft.visibility = ImageView.VISIBLE
                when (data.trafficCode) {
                    "CAR" -> {
                        binding.imageViewTrafficText.setImageResource(R.drawable.collapse_car_text_200)
                        binding.imageViewTraffic.setImageResource(R.drawable.collapse_car_image_left_120)
                        tts.speak("전방 왼쪽에 자동차가 있습니다", TextToSpeech.QUEUE_FLUSH, null, "")
                        oppMarker.iconImage = oppMarkerCarLeft
                    }
                    "PEDESTRIAN" -> {
                        binding.imageViewTrafficText.setImageResource(R.drawable.collapse_pedestrian_text_200)
                        binding.imageViewTraffic.setImageResource(R.drawable.collapse_pedestrian_image_left_120)
                        tts.speak("전방 왼쪽에 보행자가 있습니다", TextToSpeech.QUEUE_FLUSH, null, "")
                        oppMarker.iconImage = oppMarkerPedestrianLeft
                    }
                }
            } else if (relativeAngle > 180 && relativeAngle < 360) {
                binding.imageViewArrowRight.visibility = ImageView.VISIBLE
                binding.imageViewArrowLeft.visibility = ImageView.INVISIBLE
                when (data.trafficCode) {
                    "CAR" -> {
                        binding.imageViewTrafficText.setImageResource(R.drawable.collapse_car_text_200)
                        binding.imageViewTraffic.setImageResource(R.drawable.collapse_car_image_right_120)
                        tts.speak("전방 오른쪽에 자동차가 있습니다", TextToSpeech.QUEUE_FLUSH, null, "")
                        oppMarker.iconImage = oppMarkerCarRight
                    }
                    "PEDESTRIAN" -> {
                        binding.imageViewTrafficText.setImageResource(R.drawable.collapse_pedestrian_text_200)
                        binding.imageViewTraffic.setImageResource(R.drawable.collapse_pedestrian_image_right_120)
                        tts.speak("전방 오른쪽에 보행차가 있습니다", TextToSpeech.QUEUE_FLUSH, null, "")
                        oppMarker.iconImage = oppMarkerPedestrianRight
                    }
                }
            }
            oppMarker.map = inaviMap
        }
        if (collapsed && data.warningCode == "SAFE") {
//            collapsedCnt -= 1
//            if (collapsedCnt == 0) {
//                collapsed = false
//                binding.viewWarning.visibility = View.INVISIBLE
//            }
            oppMarker.map = null
            collapsed = false
            binding.viewWarning.visibility = View.INVISIBLE
            binding.linearLayoutWarningDirection.visibility = View.INVISIBLE
            Log.d("충돌", "충돌 경고 해제")
            ringtone.stop()
        }
    }

    fun drawIntersections(intersections: List<Intersection>) {
        for (intersection in intersections) {
            if (intersection.intersectionId !in intersectoinIdList) {
                intersectoinIdList.add(intersection.intersectionId)
                val marker = InvMarker().apply {
                    position = LatLng(intersection.position.latitude, intersection.position.longitude)
                    iconImage = intersectionMarkerImage
                    map = inaviMap
                }
                intersectionInfoList.add(IntersectionsInfo(intersection, "SAFE", marker))
            }
        }
    }

    fun warningPositionCheck() {
        if (intersectionInfoList.size > 0) {
            //Log.d("위험", "왜 안돼")
            for (intersectionsInfo in intersectionInfoList){
                val intersectionLocation = Location("intersectionLocation")
                intersectionLocation.latitude = intersectionsInfo.intersection.position.latitude
                intersectionLocation.longitude = intersectionsInfo.intersection.position.longitude
                warningCheck(intersectionLocation, intersectionsInfo.intersection.intersectionId)
            }
        }
    }

    fun warningCheck(intersectionLocation: Location, intersectionId: Int) {
        val intersectionListIndex = intersectoinIdList.indexOf(intersectionId)
        if (prevLocation.distanceTo(intersectionLocation) <= speed * calculationTime) {
            if (intersectionInfoList[intersectionListIndex].warningCode == "SAFE") {
                intersectionInfoList[intersectionListIndex].warningCode = "WARN"
                warningCnt += 1
            }
        } else if (intersectionInfoList[intersectionListIndex].warningCode == "WARN") {
            intersectionInfoList[intersectionListIndex].warningCode = "SAFE"
            warningCnt -= 1
            Log.d("디버그", "${warningCnt}")
//            if (warningCnt == 0) {
//                collapsed = false
////                collapsedCnt = 0
//                ringtone.stop()
//                binding.viewWarning.visibility = View.INVISIBLE
//            }
        } else {
            return
        }
        val intersectionsInfo = intersectionInfoList.get(intersectionListIndex)
        warningNotify(
            intersectionsInfo.warningCode,
            intersectionsInfo.intersection.intersectionId,
            prevLocation.latitude, prevLocation.longitude)
    }

    fun warningNotify(warningCode: String, intersectionId: Int, latitude: Double, longitude: Double) {
        val warningPositionRequest = WarningPositionRequest(intersectionId, latitude, longitude, warningCode)
        warningPositionService.warningPositionNotify(accessToken = MainActivity.accessToken, warningPositionRequest).enqueue(object: Callback<BasicResponse> {
            override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                val result = response.body()
                if (result?.status == 201) {
                    Log.d("위험", "성공")
                } else {
                    Log.d("위험", "${warningPositionRequest}")
                }
            }

            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                Log.e("연결 실패", "${t.localizedMessage}")
            }
        })
    }

    override fun onDestroy() {
        if (tts != null) {
            tts.stop()
            tts.shutdown()
        }
        if (timerTaskIntersection != null)
            timerTaskIntersection?.cancel()
        if (timerTaskWarning != null)
            timerTaskWarning?.cancel()
        super.onDestroy()
    }
}

interface SearchPoiService {
    @GET("tmap/pois")
    fun searchPoi(@Query("version") version: Int,
                  @Query("page") page: Int,
                  @Query("count") count: Int,
                  @Query("searchKeyword") searchKeyword: String,
                  @Query("appKey") appKey: String): Call<PoiSearchResponse>
}

interface SavePathService {
    @POST("members/path")
    fun savePath(@Header("Authorization") accessToken: String, @Body savePathRequest: SavePathRequest): Call<BasicResponse>
}

interface GetIntersectionsService {
    @GET("members/intersections")
    fun getIntersections(@Header("Authorization") accessToken: String): Call<IntersectionsResponse>
}

interface WarningPositionService {
    @POST("members/warning-position")
    fun warningPositionNotify(@Header("Authorization") accessToken: String,
                              @Body warningPositionRequest: WarningPositionRequest): Call<BasicResponse>
}

interface WaningService {
    @GET("members/warning")
    fun getWarning(@Header("Authorization") accessToken: String,
                   @Query("intersectionId") intersectioinId: Int): Call<WarningResponse>
}