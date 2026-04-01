package com.example.howscat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.howscat.dto.ApiResponse;
import com.example.howscat.dto.SignupRequest;
import com.example.howscat.network.ApiService;
import com.example.howscat.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 이미 JWT가 있으면 로그인 화면/회원가입 화면으로 매번 안 들어가게 처리
        String accessToken = getSharedPreferences("auth", MODE_PRIVATE)
                .getString("accessToken", null);
        if (accessToken != null && !accessToken.isEmpty()) {
            String catName = getSharedPreferences("auth", MODE_PRIVATE)
                    .getString("lastViewedCatName", "");
            Intent intent = new Intent(SignupActivity.this, HomeActivity.class);
            if (catName != null && !catName.isEmpty()) {
                intent.putExtra("catName", catName);
            }
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.signup);

        Button signupbutton = findViewById(R.id.signupButton);
        Button loginbutton = findViewById(R.id.loginButton);


        signupbutton.setOnClickListener(v -> signup());
        loginbutton.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void signup() {

        EditText loginIdEdit = findViewById(R.id.editLoginId);
        EditText passwordEdit = findViewById(R.id.editPassword);
        EditText nameEdit = findViewById(R.id.editName);

        String loginId = loginIdEdit.getText().toString().trim();
        String password = passwordEdit.getText().toString().trim();
        String name = nameEdit.getText().toString().trim();

        if (loginId.isEmpty()) {
            Toast.makeText(this, "아이디를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (loginId.length() < 4) {
            Toast.makeText(this, "아이디는 4자 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "비밀번호는 6자 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty()) {
            Toast.makeText(this, "이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        SignupRequest request = new SignupRequest(loginId, password, name);

        ApiService api = RetrofitClient.getApiService(this);
        api.signup(request).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("signup", "회원가입 성공");
                    Toast.makeText(SignupActivity.this, "회원가입 완료! 로그인해 주세요.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    startActivity(intent);
                } else {
                    Log.d("signup", "실패: " + response.code());
                    if (response.code() == 409) {
                        Toast.makeText(SignupActivity.this, "이미 사용 중인 아이디입니다.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(SignupActivity.this, "회원가입에 실패했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Log.d("signup", "에러: " + t.getMessage());
                Toast.makeText(SignupActivity.this, "서버에 연결할 수 없습니다. 네트워크를 확인해 주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
