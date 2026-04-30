package com.example.howscat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import com.example.howscat.dto.CatResponse;
import com.example.howscat.HealthScheduleAlarmScheduler;
import com.example.howscat.network.ApiService;
import com.example.howscat.network.RetrofitClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private TextView textCatName;
    private ImageView imageTopBarProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        textCatName = findViewById(R.id.textCatName);
        imageTopBarProfile = findViewById(R.id.imageTopBarProfile);
        androidx.core.widget.ImageViewCompat.setImageTintList(
                imageTopBarProfile,
                androidx.core.content.ContextCompat.getColorStateList(this, R.color.white));
        loadProfileImage();

        // 🔔 로그인된 고양이 기준 건강검진/예방접종/투약 알림 예약 동기화
        long catId = getSharedPreferences("auth", MODE_PRIVATE)
                .getLong("lastViewedCatId", -1L);
        if (catId > 0) {
            HealthScheduleAlarmScheduler.syncAlarms(this, catId);
            MedicationAlarmScheduler.syncAlarms(this, catId);
        }

        // SharedPreferences의 최신 고양이 이름 우선, 없으면 intent extra
        String savedCatName = getSharedPreferences("auth", MODE_PRIVATE)
                .getString("lastViewedCatName", null);
        String intentCatName = getIntent().getStringExtra("catName");
        String catName = (savedCatName != null && !savedCatName.isEmpty()) ? savedCatName : intentCatName;
        if (catName != null) {
            textCatName.setText(catName);
        }

        // 고양이 이름 클릭 → 목록 보여주기
        textCatName.setOnClickListener(v -> showCatSelectionDialog());

        // 초기 홈 프래그먼트
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            } else if (id == R.id.nav_hospital) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HospitalFragment())
                        .commit();
            } else if (id == R.id.nav_cat) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CatFragment())
                        .commit();
            } else if (id == R.id.nav_calendar) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new CalendarFragment())
                        .commit();
            } else if (id == R.id.nav_mypage) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new MypageFragment())
                        .commit();
            }

            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileImage();
    }

    public void loadProfileImage() {
        if (imageTopBarProfile == null) return;
        long catId = getSharedPreferences("auth", MODE_PRIVATE)
                .getLong("lastViewedCatId", 0L);
        String uriStr = getSharedPreferences("profile", MODE_PRIVATE)
                .getString("profile_image_uri_cat_" + catId, null);
        if (uriStr == null) {
            // 해당 고양이 프로필 없음 → 기본 아이콘으로 초기화
            imageTopBarProfile.setImageResource(R.drawable.ic_cat);
            int pad = (int) (10 * getResources().getDisplayMetrics().density);
            imageTopBarProfile.setPadding(pad, pad, pad, pad);
            androidx.core.widget.ImageViewCompat.setImageTintList(
                    imageTopBarProfile,
                    androidx.core.content.ContextCompat.getColorStateList(this, R.color.white));
            return;
        }
        try {
            Uri uri = Uri.parse(uriStr);
            // 썸네일용으로 1/4 크기로 축소해 메모리 누수 방지
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;
            Bitmap bitmap = BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(uri), null, opts);
            if (bitmap != null) {
                // 이전 Bitmap 명시 해제
                android.graphics.drawable.Drawable prev = imageTopBarProfile.getDrawable();
                if (prev instanceof android.graphics.drawable.BitmapDrawable) {
                    Bitmap old = ((android.graphics.drawable.BitmapDrawable) prev).getBitmap();
                    if (old != null && !old.isRecycled()) old.recycle();
                }
                imageTopBarProfile.setImageBitmap(bitmap);
                imageTopBarProfile.setPadding(0, 0, 0, 0);
                androidx.core.widget.ImageViewCompat.setImageTintList(imageTopBarProfile, null);
            }
        } catch (Exception ignored) {}
    }

    // 🔥 고양이 선택 다이얼로그
    private void showCatSelectionDialog() {
        ApiService apiService = RetrofitClient.getClient(this).create(ApiService.class);
        apiService.getUserCats().enqueue(new Callback<List<CatResponse>>() {
            @Override
            public void onResponse(Call<List<CatResponse>> call, Response<List<CatResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CatResponse> cats = response.body();
                    if (cats.isEmpty()) {
                        android.widget.Toast.makeText(HomeActivity.this, "등록된 고양이가 없습니다.", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long currentCatId = getSharedPreferences("auth", MODE_PRIVATE)
                            .getLong("lastViewedCatId", -1L);
                    showPrettyCatDialog(cats, currentCatId);

                } else {
                    Log.d("HomeActivity", "고양이 리스트 조회 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<CatResponse>> call, Throwable t) {
                Log.d("HomeActivity", "고양이 리스트 불러오기 실패: " + t.getMessage());
                android.widget.Toast.makeText(
                        HomeActivity.this,
                        "고양이 리스트 불러오기 실패: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()),
                        android.widget.Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void showPrettyCatDialog(List<CatResponse> cats, long currentCatId) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setGravity(android.view.Gravity.BOTTOM);
            dialog.getWindow().getAttributes().windowAnimations = android.R.style.Animation_InputMethod;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(20), dp(20), dp(32));
        root.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_bottom_sheet));

        // 헤더
        TextView title = new TextView(this);
        title.setText("반려묘 선택");
        title.setTextSize(18f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(this, R.color.app_on_surface));
        title.setPadding(dp(4), 0, 0, dp(4));
        root.addView(title);

        // 구분선
        android.view.View divider = new android.view.View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divParams.setMargins(0, dp(8), 0, dp(12));
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.app_divider));
        root.addView(divider);

        // 고양이 카드 목록
        for (CatResponse cat : cats) {
            boolean isSelected = cat.getId() != null && cat.getId() == currentCatId;

            MaterialCardView card = new MaterialCardView(this);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dp(10));
            card.setLayoutParams(cardParams);
            card.setRadius(dp(14));
            card.setCardElevation(0f);
            card.setStrokeWidth(isSelected ? dp(2) : dp(1));
            card.setStrokeColor(ContextCompat.getColor(this,
                    isSelected ? R.color.app_primary : R.color.app_outline));
            card.setCardBackgroundColor(ContextCompat.getColor(this,
                    isSelected ? R.color.app_primary_container : R.color.app_surface));

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(14), dp(16), dp(14));

            // 아이콘
            android.widget.ImageView icon = new android.widget.ImageView(this);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(36), dp(36));
            iconParams.setMarginEnd(dp(12));
            icon.setLayoutParams(iconParams);
            icon.setImageResource(R.drawable.ic_cat);
            icon.setColorFilter(ContextCompat.getColor(this,
                    isSelected ? R.color.app_primary : R.color.app_on_surface_variant));

            // 이름
            TextView nameText = new TextView(this);
            nameText.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            nameText.setText(cat.getName());
            nameText.setTextSize(15f);
            nameText.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            nameText.setTextColor(ContextCompat.getColor(this,
                    isSelected ? R.color.app_primary : R.color.app_on_surface));

            // 선택 표시
            if (isSelected) {
                android.widget.ImageView check = new android.widget.ImageView(this);
                check.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));
                check.setImageResource(R.drawable.ic_check);
                check.setColorFilter(ContextCompat.getColor(this, R.color.app_primary));
                row.addView(icon);
                row.addView(nameText);
                row.addView(check);
            } else {
                row.addView(icon);
                row.addView(nameText);
            }

            card.addView(row);
            card.setOnClickListener(v -> {
                dialog.dismiss();
                switchToCatHome(cat);
            });
            root.addView(card);
        }

        // 취소 버튼
        com.google.android.material.button.MaterialButton btnCancel =
                new com.google.android.material.button.MaterialButton(
                        new android.view.ContextThemeWrapper(this,
                                com.google.android.material.R.style.Widget_Material3_Button_TextButton),
                        null, 0);
        btnCancel.setText("취소");
        btnCancel.setTextColor(ContextCompat.getColor(this, R.color.app_on_surface_variant));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cancelParams.setMargins(0, dp(4), 0, 0);
        btnCancel.setLayoutParams(cancelParams);
        root.addView(btnCancel);

        dialog.setContentView(root);
        dialog.show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    // 🔥 선택한 고양이 홈으로 스위치
    private void switchToCatHome(CatResponse cat) {
        textCatName.setText(cat.getName());

        // 서버에서 last_viewed_cat_id 업데이트
        ApiService api = RetrofitClient.getApiService(this);
        api.selectCat(cat.getId()).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Log.d("HomeActivity", "고양이 선택 서버 업데이트 실패: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.d("HomeActivity", "고양이 선택 서버 업데이트 실패: " + t.getMessage());
            }
        });

        // 캘린더/기록에서 쓸 현재 고양이 id를 저장
        getSharedPreferences("auth", MODE_PRIVATE)
                .edit()
                .putLong("lastViewedCatId", cat.getId() != null ? cat.getId() : -1L)
                .putString("lastViewedCatName", cat.getName() != null ? cat.getName() : "")
                .apply();

        // 고양이 전환 시 해당 고양이 알람 동기화
        if (cat.getId() != null && cat.getId() > 0) {
            HealthScheduleAlarmScheduler.syncAlarms(this, cat.getId());
            MedicationAlarmScheduler.syncAlarms(this, cat.getId());
        }

        // 고양이 전환 시 해당 고양이 프로필 이미지 갱신
        loadProfileImage();

        // 홈 프래그먼트 새로고침
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }
}