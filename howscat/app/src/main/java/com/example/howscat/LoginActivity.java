package com.example.howscat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.howscat.dto.LoginRequest;
import com.example.howscat.dto.LoginResponse;
import com.example.howscat.network.ApiService;
import com.example.howscat.network.RetrofitClient;
import com.example.howscat.BuildConfig;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    EditText editLoginId, editPassword;
    Button btnLogin;
    Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        editLoginId = findViewById(R.id.editLoginId);
        editPassword = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.loginButton);
        btnSignup = findViewById(R.id.signupButton);

        // 🔹 로그인 버튼 클릭
        btnLogin.setOnClickListener(view -> login());

        // 🔹 회원가입 버튼 클릭
        btnSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        // 🔹 뒤로가기 제스처 처리 (AndroidX)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                clearAuth();  // 뒤로가기 시 SharedPreferences 초기화
                setEnabled(false);  // 콜백 비활성화
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    private void login() {
        String loginId = editLoginId.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (loginId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        LoginRequest request = new LoginRequest(loginId, password);

        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);

        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    LoginResponse loginResponse = response.body();

                    String accessToken = loginResponse.getAccessToken();
                    String refreshToken = loginResponse.getRefreshToken();
                    String name = loginResponse.getName();
                    String catName = loginResponse.getLastViewedCatName();
                    Long catId = loginResponse.getLastViewedCatId();

                    // 🔹 SharedPreferences에 저장
                    getSharedPreferences("auth", MODE_PRIVATE)
                            .edit()
                            .putString("accessToken", accessToken)
                            .putString("refreshToken", refreshToken)
                            .putString("name", name)
                            .putString("loginId", loginId)
                            .putLong("lastViewedCatId", catId != null ? catId : -1L)
                            .putString("lastViewedCatName", catName != null ? catName : "")
                            .apply();

                    Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();

                    Intent intent;
                    if (catId != null && catName != null) {
                        // 🔹 마지막 조회한 고양이가 있으면 HomeActivity로 이동
                        intent = new Intent(LoginActivity.this, HomeActivity.class);
                        intent.putExtra("catName", catName);
                    } else {
                        // 🔹 고양이 등록 화면으로 이동
                        intent = new Intent(LoginActivity.this, CatRegisterActivity.class);
                    }

                    startActivity(intent);
                    finish();

                } else {
                    Toast.makeText(LoginActivity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
                    Log.d("LOGIN_CODE", "code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e("login", "연결 실패: " + t.getMessage(), t);
                String hint;
                if (BuildConfig.DEBUG) {
                    hint = "연결 실패. " + BuildConfig.API_BASE_URL + " 에 접속할 수 없습니다.\n"
                            + "PC에서 서버 실행·IP(local.properties)·같은 Wi‑Fi를 확인하세요. "
                            + "에뮬레이터는 10.0.2.2:8080 을 쓰세요.";
                } else {
                    hint = "서버에 연결할 수 없습니다. 네트워크 상태를 확인해 주세요.";
                }
                Toast.makeText(LoginActivity.this, hint, Toast.LENGTH_LONG).show();
            }
        });
    }

    // 🔹 SharedPreferences 초기화
    private void clearAuth() {
        getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }
}