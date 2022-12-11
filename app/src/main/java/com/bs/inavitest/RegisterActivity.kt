package com.bs.inavitest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.bs.inavitest.Request.UserdataRequest
import com.bs.inavitest.Response.BasicResponse
import com.bs.inavitest.databinding.ActivityRegisterBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class RegisterActivity : AppCompatActivity() {
    val binding by lazy { ActivityRegisterBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://43.201.11.119:8080")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        binding.buttonRegister.setOnClickListener {
            val username = binding.editTextIdRegister.text.toString()
            val password = binding.editTextPasswordRegister.text.toString()
            val confirmPassword = binding.editTextConfirmPWRegister.text.toString()
            if (username == "" || password == "" || confirmPassword == "") {
                Toast.makeText(this, "아이디와 패스워드 모두 입력해주세요.", Toast.LENGTH_LONG).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "패스워드가 일치하지 않습니다..", Toast.LENGTH_LONG).show()
            } else {
                val registerService = retrofit.create(RegisterService::class.java)
                val userdataRequest = UserdataRequest(password=password, username=username)
                registerService.userRegister(userdataRequest).enqueue(object:
                    Callback<BasicResponse> {
                    override fun onResponse(
                        call: Call<BasicResponse>,
                        response: Response<BasicResponse>
                    ) {
                        val result = response.body()
                        //Log.d("회원가입 성공", "${result}")
                        if (result?.status == 201){
                            Toast.makeText(this@RegisterActivity, "회원가입 되었습니다.", Toast.LENGTH_LONG).show()
                            onBackPressed()
                        } else {
                            Toast.makeText(this@RegisterActivity, "사용할 수 없는 아이디 입니다.", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                        Toast.makeText(this@RegisterActivity, "연결 실패", Toast.LENGTH_LONG).show()
                        Log.e("연결 실패", "${t.localizedMessage}")
                    }
                })
            }
        }
    }
}

interface RegisterService {
    @POST("members")
    fun userRegister(@Body registerRequest: UserdataRequest): Call<BasicResponse>
}