package com.example.howscat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.howscat.dto.CatResponse;
import com.example.howscat.network.ApiService;
import com.example.howscat.dto.CatRequest;
import com.example.howscat.network.RetrofitClient;

import java.util.List;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CatRegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cat_register);

        Button registerButton = findViewById(R.id.registerCatButton);
        registerButton.setOnClickListener(v -> registerCat());

        EditText birthEdit = findViewById(R.id.editCatBirth);
        TextInputLayout birthLayout = findViewById(R.id.layoutCatBirth);

        View.OnClickListener openDatePicker = v -> {
            CalendarConstraints constraints = new CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointBackward.now())
                    .build();
            MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("생년월일 선택")
                    .setCalendarConstraints(constraints)
                    .build();
            picker.addOnPositiveButtonClickListener(selection -> {
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.setTimeInMillis(selection);
                birthEdit.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d",
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH)));
            });
            picker.show(getSupportFragmentManager(), "birth_date_picker");
        };

        birthEdit.setOnClickListener(openDatePicker);
        if (birthLayout != null) {
            birthLayout.setEndIconOnClickListener(openDatePicker);
        }

        TextView welcomeText = findViewById(R.id.textWelcome);

        String name = getSharedPreferences("auth", MODE_PRIVATE)
                .getString("name", "");

        welcomeText.setText("환영합니다 " + name + " 집사님!");
    }

    private void registerCat() {

        EditText nameEdit = findViewById(R.id.editCatName);
        MaterialButtonToggleGroup toggleGender = findViewById(R.id.toggleCatGender);
        EditText birthEdit = findViewById(R.id.editCatBirth);

        String name = nameEdit.getText().toString().trim();
        String birthDate = birthEdit.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "고양이 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (toggleGender.getCheckedButtonId() == View.NO_ID) {
            Toast.makeText(this, "성별을 선택해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (birthDate.isEmpty()) {
            Toast.makeText(this, "생일을 입력해주세요. (예: 2022-03-15)", Toast.LENGTH_SHORT).show();
            return;
        }

        String gender = (toggleGender.getCheckedButtonId() == R.id.btnGenderFemale) ? "F" : "M";

        String accessToken = getSharedPreferences("auth", MODE_PRIVATE)
                .getString("accessToken", null);

        if (accessToken == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        CatRequest request = new CatRequest(name, gender, birthDate);
        ApiService api = RetrofitClient.getApiService(this);

        api.registerCat(request).enqueue(new Callback<com.example.howscat.dto.ApiResponse>() {
            @Override
            public void onResponse(Call<com.example.howscat.dto.ApiResponse> call, Response<com.example.howscat.dto.ApiResponse> response) {
                Log.d("API_TEST", "응답 코드: " + response.code());
                if (response.isSuccessful()) {
                    Log.d("cat", "등록 성공");
                    Toast.makeText(CatRegisterActivity.this, name + " 등록 완료!", Toast.LENGTH_SHORT).show();
                    // 서버 응답에서 catId를 직접 파싱해 SharedPreferences에 저장
                    long newCatId = -1L;
                    if (response.body() != null && response.body().getCatId() != null) {
                        newCatId = response.body().getCatId();
                    }
                    if (newCatId > 0) {
                        getSharedPreferences("auth", MODE_PRIVATE).edit()
                                .putLong("lastViewedCatId", newCatId)
                                .putString("lastViewedCatName", name)
                                .apply();
                    }
                    CareResultPrefs.clear(CatRegisterActivity.this);
                    Intent intent = new Intent(CatRegisterActivity.this, HomeActivity.class);
                    intent.putExtra("catName", name);
                    startActivity(intent);
                    finish();
                } else {
                    Log.d("cat", "등록 실패: " + response.code());
                    Toast.makeText(CatRegisterActivity.this, "등록에 실패했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<com.example.howscat.dto.ApiResponse> call, Throwable t) {
                Log.d("cat", "에러: " + t.getMessage());
                Toast.makeText(CatRegisterActivity.this, "서버에 연결할 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}