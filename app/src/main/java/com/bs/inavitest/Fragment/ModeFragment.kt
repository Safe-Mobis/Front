package com.bs.inavitest.Fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bs.inavitest.LoginService
import com.bs.inavitest.MainActivity
import com.bs.inavitest.R
import com.bs.inavitest.Request.TrafficCodeRequest
import com.bs.inavitest.Request.TrafficModeRequest
import com.bs.inavitest.Request.UserdataRequest
import com.bs.inavitest.Response.BasicResponse
import com.bs.inavitest.Response.TrafficCodeResponse
import com.bs.inavitest.Response.TrafficMode
import com.bs.inavitest.Response.TrafficModeResponse
import com.bs.inavitest.databinding.FragmentModeBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

class ModeFragment : Fragment() {
    companion object {
        var curMode = 0
    }

    val trafficModeList = Array(6, {item->TrafficMode(true,true,true,true,true,true,"")})

    val trafficCodeList = arrayOf("CAR", "PEDESTRIAN", "CHILD", "KICK_BOARD", "BICYCLE", "MOTORCYCLE")

    val trafficCodeListKorean = arrayOf("차량", "보행자", "어린이", "킥보드", "자전거", "오토바이")

    val trafficCodeListImage = arrayOf(
        R.drawable.traffic_code_car_120,
        R.drawable.traffic_code_pedestrian_120,
        R.drawable.traffic_code_child_120,
        R.drawable.traffic_code_kickboard_120,
        R.drawable.traffic_code_bicycle_120,
        R.drawable.traffic_code_motorcycle_120
    )

    lateinit var binding: FragmentModeBinding

    val retrofit = Retrofit.Builder()
        .baseUrl("http://43.201.11.119:8080")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val trafficCodeGetService = retrofit.create(TrafficCodeGetService::class.java)

    val trafficModeGetService = retrofit.create(TrafficModeGetService::class.java)

    val trafficModePatchService = retrofit.create(TrafficModePatchService::class.java)

    val trafficCodePatchService = retrofit.create(TrafficCodePatchService::class.java)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentModeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        trafficCodeGetService.trafficCodeGet(accessToken = MainActivity.accessToken).enqueue(object : Callback<TrafficCodeResponse> {
            override fun onResponse(call: Call<TrafficCodeResponse>, response: Response<TrafficCodeResponse>) {
                val result = response.body()
                if (result?.status == 200) {
                    curMode = trafficCodeList.indexOf(result.data.trafficCode)
                    Log.d("트래픽", "겟 성공")
                } else {
                    Log.d("트래픽", "겟 실패 ${result}")
                }
            }

            override fun onFailure(call: Call<TrafficCodeResponse>, t: Throwable) {
                Log.e("연결 실패", "${t.localizedMessage}")
            }
        })

        trafficModeGetService.trafficModeGet(accessToken = MainActivity.accessToken).enqueue(object : Callback<TrafficModeResponse> {
            override fun onResponse(call: Call<TrafficModeResponse>, response: Response<TrafficModeResponse>) {
                val result = response.body()
                if (result?.status == 200) {
                    for (trafficMode in result.data.trafficModes) {
                        val idx = trafficCodeList.indexOf(trafficMode.trafficCode)
                        trafficModeList[idx].trafficCode = trafficMode.trafficCode
                        trafficModeList[idx].bicycleFlag = trafficMode.bicycleFlag
                        trafficModeList[idx].carFlag = trafficMode.carFlag
                        trafficModeList[idx].childFlag = trafficMode.childFlag
                        trafficModeList[idx].kickboardFlag = trafficMode.kickboardFlag
                        trafficModeList[idx].motorcycleFlag = trafficMode.motorcycleFlag
                        trafficModeList[idx].pedestrianFlag = trafficMode.pedestrianFlag
                    }
                    Log.d("트래픽", "겟 성공")
                    setTrafficCodeMode()
                } else {
                    Log.d("트래픽", "겟 실패")
                }
            }

            override fun onFailure(call: Call<TrafficModeResponse>, t: Throwable) {
                Log.e("연결 실패", "${t.localizedMessage}")
            }
        })

        binding.trafficCodeSetBtn.setOnClickListener {
            saveTrafficCodeMode()
        }

        binding.trafficCodeLeftBtn.setOnClickListener {
            curMode -= 1
            if (curMode == -1)
                curMode = 5
            Log.d("나눗셈", "${curMode}")
            setTrafficCodeMode()
        }

        binding.trafficCodeRightBtn.setOnClickListener {
            curMode = (curMode + 1) % 6
            setTrafficCodeMode()
        }

        binding.imageBtnCar.setOnClickListener {
            if (trafficModeList[curMode].carFlag){
                trafficModeList[curMode].carFlag = false
                binding.imageBtnCar.setBackgroundResource(R.drawable.traffic_code_choice_btn)
            } else {
                trafficModeList[curMode].carFlag = true
                binding.imageBtnCar.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
            }
        }

        binding.imageBtnPedestrian.setOnClickListener {
            if (trafficModeList[curMode].pedestrianFlag){
                trafficModeList[curMode].pedestrianFlag = false
                binding.imageBtnPedestrian.setBackgroundResource(R.drawable.traffic_code_choice_btn)
            } else {
                trafficModeList[curMode].pedestrianFlag = true
                binding.imageBtnPedestrian.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
            }
        }

        binding.imageBtnChild.setOnClickListener {
            if (trafficModeList[curMode].childFlag){
                trafficModeList[curMode].childFlag = false
                binding.imageBtnChild.setBackgroundResource(R.drawable.traffic_code_choice_btn)
            } else {
                trafficModeList[curMode].childFlag = true
                binding.imageBtnChild.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
            }
        }

        binding.imageBtnKickboard.setOnClickListener {
            if (trafficModeList[curMode].kickboardFlag){
                trafficModeList[curMode].kickboardFlag = false
                binding.imageBtnKickboard.setBackgroundResource(R.drawable.traffic_code_choice_btn)
            } else {
                trafficModeList[curMode].kickboardFlag = true
                binding.imageBtnKickboard.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
            }
        }

        binding.imageBtnBicycle.setOnClickListener {
            if (trafficModeList[curMode].bicycleFlag){
                trafficModeList[curMode].bicycleFlag = false
                binding.imageBtnBicycle.setBackgroundResource(R.drawable.traffic_code_choice_btn)
            } else {
                trafficModeList[curMode].bicycleFlag = true
                binding.imageBtnBicycle.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
            }
        }

        binding.imageBtnMotorcycle.setOnClickListener {
            if (trafficModeList[curMode].motorcycleFlag){
                trafficModeList[curMode].motorcycleFlag = false
                binding.imageBtnMotorcycle.setBackgroundResource(R.drawable.traffic_code_choice_btn)
            } else {
                trafficModeList[curMode].motorcycleFlag = true
                binding.imageBtnMotorcycle.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
            }
        }
    }

    fun setTrafficCodeMode() {
        Log.d("트래픽", "디버그")
        binding.imageViewSettingTrafficCode.setImageResource(trafficCodeListImage[curMode])
        binding.textViewSettingTrafficCode.text = trafficCodeListKorean[curMode]

        if (trafficModeList[curMode].carFlag)
            binding.imageBtnCar.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
        else
            binding.imageBtnCar.setBackgroundResource(R.drawable.traffic_code_choice_btn)

        if (trafficModeList[curMode].pedestrianFlag)
            binding.imageBtnPedestrian.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
        else
            binding.imageBtnPedestrian.setBackgroundResource(R.drawable.traffic_code_choice_btn)

        if (trafficModeList[curMode].childFlag)
            binding.imageBtnChild.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
        else
            binding.imageBtnChild.setBackgroundResource(R.drawable.traffic_code_choice_btn)

        if (trafficModeList[curMode].kickboardFlag)
            binding.imageBtnKickboard.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
        else
            binding.imageBtnKickboard.setBackgroundResource(R.drawable.traffic_code_choice_btn)

        if (trafficModeList[curMode].bicycleFlag)
            binding.imageBtnBicycle.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
        else
            binding.imageBtnBicycle.setBackgroundResource(R.drawable.traffic_code_choice_btn)

        if (trafficModeList[curMode].motorcycleFlag)
            binding.imageBtnMotorcycle.setBackgroundResource(R.drawable.traffic_code_choice_btn_red)
        else
            binding.imageBtnMotorcycle.setBackgroundResource(R.drawable.traffic_code_choice_btn)
    }

    fun saveTrafficCodeMode() {
        val trafficCode = trafficCodeList[curMode]
        val trafficCodeRequest = TrafficCodeRequest(trafficCode)
        trafficCodePatchService.trafficCodePatch(accessToken = MainActivity.accessToken, trafficCodeRequest).enqueue(object: Callback<BasicResponse> {
            override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                val result = response.body()
                if (result?.status == 200) {
                    Log.d("트래픽", "패치 성공")
                } else {
                    Log.d("트래픽", "패치 실패 ${trafficCode}, ${trafficCodeRequest}, ${result}")
                }
            }

            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                Log.e("연결 실패", "${t.localizedMessage}")
            }
        })

        val trafficModeRequest = TrafficModeRequest(trafficModeList.toList())
        trafficModePatchService.trafficModePatch(accessToken = MainActivity.accessToken, trafficModeRequest).enqueue(object: Callback<BasicResponse> {
            override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                val result = response.body()
                if (result?.status == 200) {
                    Log.d("트래픽", "패치 성공")
                } else {
                    Log.d("트래픽", "패치 실패 ${trafficModeRequest}")
                }
            }

            override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                Log.e("연결 실패", "${t.localizedMessage}")
            }
        })
    }

    override fun onPause() {
        super.onPause()
        saveTrafficCodeMode()
    }
}

interface TrafficCodeGetService {
    @GET("members/traffic-code")
    fun trafficCodeGet(@Header("Authorization") accessToken: String): Call<TrafficCodeResponse>
}

interface TrafficModeGetService {
    @GET("members/traffic-mode")
    fun trafficModeGet(@Header("Authorization") accessToken: String): Call<TrafficModeResponse>
}

interface TrafficCodePatchService {
    @PATCH("members/traffic-code")
    fun trafficCodePatch(@Header("Authorization") accessToken: String, @Body trafficCodeRequest: TrafficCodeRequest): Call<BasicResponse>
}

interface TrafficModePatchService {
    @PATCH("members/traffic-mode")
    fun trafficModePatch(@Header("Authorization") accessToken: String, @Body trafficModeRequest: TrafficModeRequest): Call<BasicResponse>
}