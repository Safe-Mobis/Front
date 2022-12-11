package com.bs.inavitest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.bs.inavitest.Request.UserdataRequest
import com.bs.inavitest.Response.BasicResponse
import com.bs.inavitest.databinding.ActivityLoginBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class LoginActivity : AppCompatActivity() {
    val binding by lazy { ActivityLoginBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val mainIntent = Intent(this, MainActivity::class.java)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://43.201.11.119:8080")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        binding.buttonLogin.setOnClickListener {
            val username = binding.editTextIdLogin.text.toString()
            val password = binding.editTextPasswordLogin.text.toString()
            if (username == "" || password == "") {
                Toast.makeText(this, "아이디와 패스워드 모두 입력해주세요.", Toast.LENGTH_LONG).show()
            } else {
                val loginService = retrofit.create(LoginService::class.java)
                val userdataRequest = UserdataRequest(password=password, username=username)
                loginService.userLogin(userdataRequest).enqueue(object: Callback<BasicResponse> {
                    override fun onResponse(
                        call: Call<BasicResponse>,
                        response: Response<BasicResponse>
                    ) {
                        val result = response.body()
                        //Log.d("로그인 성공", "${result}")
                        if (result?.status == 200){
                            mainIntent.putExtra("username", username)
                            mainIntent.putExtra("password", password)
                            mainIntent.putExtra("accessToken", result.data.accessToken)
                            startActivity(mainIntent)
                        } else{
                            Toast.makeText(this@LoginActivity, "아이디 또는 비밀번호를 확인해주세요", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<BasicResponse>, t: Throwable) {
                        Toast.makeText(this@LoginActivity, "연결 실패", Toast.LENGTH_LONG).show()
                        Log.e("연결 실패", "${t.localizedMessage}")
                    }
                })
            }
        }

        val registerIntent = Intent(this, RegisterActivity::class.java)

        binding.buttonToRegister.setOnClickListener {
            startActivity(registerIntent)
        }
    }
}

interface LoginService {
    @POST("members/login")
    fun userLogin(@Body userdataRequest: UserdataRequest): Call<BasicResponse>
}