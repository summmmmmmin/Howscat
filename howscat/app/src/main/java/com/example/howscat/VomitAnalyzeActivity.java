package com.example.howscat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;

import com.example.howscat.dto.VomitAnalysisRequest;
import com.example.howscat.dto.VomitAnalysisResponse;
import com.example.howscat.network.ApiService;
import com.example.howscat.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VomitAnalyzeActivity extends AppCompatActivity {

    private ImageView imageVomitPreview;
    private TextView textVomitResult;
    private android.widget.ImageButton btnVomitBack;
    private android.widget.LinearLayout layoutImagePlaceholder;

    private Uri selectedImageUri;
    private Uri cameraImageUri;
    private boolean resultSaved = false;

    private final ActivityResultLauncher<String[]> pickVomitImageLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    try {
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    } catch (Exception ignored) {
                    }
                    onImageSelected(uri);
                }
            });

    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (Boolean.TRUE.equals(success) && cameraImageUri != null) {
                    onImageSelected(cameraImageUri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vomit_analyze);

        imageVomitPreview = findViewById(R.id.imageVomitPreview);
        layoutImagePlaceholder = findViewById(R.id.layoutImagePlaceholder);
        MaterialButton btnPick = findViewById(R.id.btnPickVomitImage);
        MaterialButton btnAnalyze = findViewById(R.id.btnAnalyzeVomit);
        TextView title = findViewById(R.id.textVomitTitle);
        textVomitResult = findViewById(R.id.textVomitResult);
        btnVomitBack = findViewById(R.id.btnVomitBack);
        if (btnVomitBack != null) {
            btnVomitBack.setOnClickListener(v -> finish());
        }

        MaterialButton btnCapture = findViewById(R.id.btnCaptureVomitImage);
        EditText editMemo = findViewById(R.id.editVomitMemo);

        title.setText("토 사진 분석");
        imageVomitPreview.setOnClickListener(v -> openImageInGallery(selectedImageUri));
        btnPick.setOnClickListener(v -> pickVomitImageLauncher.launch(new String[]{"image/*"}));
        btnCapture.setOnClickListener(v -> launchCamera());

        btnAnalyze.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                textVomitResult.setText("먼저 사진을 선택해 주세요.");
                return;
            }
            if (resultSaved) {
                textVomitResult.setText("이미 분석이 완료된 사진입니다. 새 사진을 선택해 주세요.");
                return;
            }

            Long catId = getCurrentCatId();
            if (catId == null || catId <= 0) {
                textVomitResult.setText("고양이를 먼저 선택해 주세요.");
                return;
            }

            String base64 = encodeImageToBase64(selectedImageUri);
            if (base64 == null) {
                textVomitResult.setText("이미지를 읽을 수 없어요. 다시 시도해 주세요.");
                return;
            }

            textVomitResult.setText("AI가 사진을 분석 중이에요...");
            btnAnalyze.setEnabled(false);

            String memo = editMemo.getText() != null ? editMemo.getText().toString().trim() : "";
            VomitAnalysisRequest req = new VomitAnalysisRequest(
                    base64,
                    memo,
                    selectedImageUri.toString()
            );

            ApiService apiService = RetrofitClient.getApiService(this);
            apiService.analyzeVomit(catId, req).enqueue(new Callback<VomitAnalysisResponse>() {
                @Override
                public void onResponse(Call<VomitAnalysisResponse> call, Response<VomitAnalysisResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        btnAnalyze.setEnabled(true);
                        textVomitResult.setText("분석 실패 (HTTP " + response.code() + "). 잠시 후 다시 시도해 주세요.");
                        return;
                    }
                    resultSaved = true;
                    btnAnalyze.setEnabled(false);

                    VomitAnalysisResponse res = response.body();
                    String risk = res.getRiskLevel() != null ? res.getRiskLevel() : "MEDIUM";

                    String riskLabel;
                    switch (risk) {
                        case "HIGH": riskLabel = "🔴 높음"; break;
                        case "LOW":  riskLabel = "🟢 낮음"; break;
                        default:     riskLabel = "🟡 보통"; break;
                    }

                    StringBuilder sb = new StringBuilder();
                    String aiResult = res.getAiResult();
                    if (aiResult != null && !aiResult.isEmpty()) {
                        sb.append("🔍 분석 결과: ").append(aiResult).append("\n");
                    }
                    sb.append("⚠️ 위험도: ").append(riskLabel);
                    String guideText = res.getGuideText();
                    if (guideText != null && !guideText.isEmpty()) {
                        sb.append("\n\n").append(guideText);
                    }
                    String aiGuide = res.getAiGuide();
                    if (aiGuide != null && !aiGuide.isEmpty()) {
                        sb.append("\n\n💬 ").append(aiGuide);
                    }
                    textVomitResult.setText(sb.toString());
                }

                @Override
                public void onFailure(Call<VomitAnalysisResponse> call, Throwable t) {
                    btnAnalyze.setEnabled(true);
                    textVomitResult.setText("분석 실패: 네트워크 상태를 확인해 주세요.");
                }
            });
        });
    }

    private void onImageSelected(Uri uri) {
        selectedImageUri = uri;
        resultSaved = false;
        // 미리보기는 1/4 크기로 축소해 OOM 방지 (전송용 인코딩은 별도 처리)
        try {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;
            Bitmap bmp = BitmapFactory.decodeStream(
                    getContentResolver().openInputStream(uri), null, opts);
            if (bmp != null) imageVomitPreview.setImageBitmap(bmp);
            else imageVomitPreview.setImageURI(uri);
        } catch (Exception e) {
            imageVomitPreview.setImageURI(uri); // fallback
        }
        if (layoutImagePlaceholder != null) {
            layoutImagePlaceholder.setVisibility(View.GONE);
        }
        MaterialButton btnAnalyze = findViewById(R.id.btnAnalyzeVomit);
        if (btnAnalyze != null) btnAnalyze.setEnabled(true);
        textVomitResult.setText("사진이 선택됐어요. '분석하기' 버튼을 눌러주세요.");
    }

    private void launchCamera() {
        try {
            File photoFile = File.createTempFile("vomit_", ".jpg",
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            cameraImageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (Exception e) {
            Toast.makeText(this, "카메라를 열 수 없어요.", Toast.LENGTH_SHORT).show();
        }
    }

    private String encodeImageToBase64(Uri imageUri) {
        InputStream is = null;
        try {
            // 이미지를 1/4 크기로 축소해서 전송 용량 줄이기
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;
            is = getContentResolver().openInputStream(imageUri);
            if (is == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(is, null, opts);
            if (bmp == null) return null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException ignored) {}
            }
        }
    }

    private Long getCurrentCatId() {
        return getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getLong("lastViewedCatId", -1L);
    }

    private void openImageInGallery(Uri imageUri) {
        if (imageUri == null) {
            Toast.makeText(this, "먼저 사진을 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(imageUri, "image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "이미지 뷰어를 열 수 없어요.", Toast.LENGTH_SHORT).show();
        }
    }
}
