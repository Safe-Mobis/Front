package com.bs.inavitest.Fragment

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bs.inavitest.MainActivity
import com.bs.inavitest.R
import com.bs.inavitest.Response.BasicResponse
import com.bs.inavitest.databinding.FragmentProfileBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE
import retrofit2.http.Header
import java.util.*

class ProfileFragment : Fragment() {

    lateinit var binding: FragmentProfileBinding
    lateinit var mainActivity: MainActivity

    val retrofit = Retrofit.Builder()
        .baseUrl("http://43.201.11.119:8080")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val withdrawalService = retrofit.create(WithdrawalService::class.java)

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) mainActivity = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    lateinit var tts: TextToSpeech
    val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    lateinit var ringtone: Ringtone

    var sound = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        binding.buttonWithdrawal.setOnClickListener {
            withdrawalService.withdrawal(accessToken = MainActivity.accessToken).enqueue(object: Callback<BasicResponse> {
                override fun onResponse(call: Call<BasicResponse>, response: Response<BasicResponse>) {
                    val result = response.body()
                    if (result?.status == 200) {
                        Log.d("탈퇴", "탈퇴 성공")
                        mainActivity.finish()
                    } else {
                        Log.d("탈퇴", "탈퇴 실패")
                    }
                }

                override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                    Log.e("연결 실패", "${t.localizedMessage}")
                }
            })
        }

        binding.button2.setOnClickListener {
            if (sound) {
                tts.speak("전방 왼쪽에 자동차가 있습니다", TextToSpeech.QUEUE_FLUSH, null, "")
                ringtone.play()
                binding.button2.text = "끄기"
                sound = false
            } else {
                tts.speak("소리가 꺼집니다 소리가 꺼집니다", TextToSpeech.QUEUE_FLUSH, null, "")
                ringtone.stop()
                binding.button2.text = "소리 테스트"
                sound = true
            }
        }

        binding.textViewCalculationTime.text = MapFragment.calculationTime.toString()

        binding.buttonCalTimeLeft.setOnClickListener {
            MapFragment.calculationTime -= 1.0
            if (MapFragment.calculationTime < 1.0)
                MapFragment.calculationTime = 1.0
            binding.textViewCalculationTime.text = MapFragment.calculationTime.toString()
        }

        binding.buttonCalTimeRight.setOnClickListener {
            MapFragment.calculationTime += 1.0
            binding.textViewCalculationTime.text = MapFragment.calculationTime.toString()
        }
    }
    override fun onDestroy() {
        if (tts != null) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}

interface WithdrawalService {
    @DELETE("members")
    fun withdrawal(@Header("Authorization") accessToken: String): Call<BasicResponse>
}