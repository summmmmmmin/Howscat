package com.example.howscat;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import com.example.howscat.dto.ApiResponse;
import com.example.howscat.dto.CalendarEventItem;
import com.example.howscat.dto.CatResponse;
import com.example.howscat.dto.HealthScheduleItem;
import com.example.howscat.dto.LitterBoxItem;
import com.example.howscat.dto.MedicationItem;
import com.example.howscat.dto.ObesityHistoryItem;
import com.example.howscat.dto.WeightHistoryItem;

import java.util.Date;
import com.example.howscat.network.ApiService;
import com.example.howscat.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MypageFragment extends Fragment {

    private TextView textFeedingAlarmSummary;
    private ImageView imageProfile;

    private final ActivityResultLauncher<String[]> pickProfileImageLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null || getContext() == null) return;
                try {
                    getContext().getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (Exception ignored) {}
                saveProfileImageUri(uri.toString());
                loadProfileImage(uri.toString());
                if (getActivity() instanceof HomeActivity) {
                    ((HomeActivity) getActivity()).loadProfileImage();
                }
            });

    public MypageFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mypage, container, false);

        Button btnCatRegister = view.findViewById(R.id.registerCatButton);
        Button btnLogout = view.findViewById(R.id.logoutButton);
        LinearLayout containerMultiCatCompare = view.findViewById(R.id.containerMultiCatCompare);
        textFeedingAlarmSummary = view.findViewById(R.id.textFeedingAlarmSummary);

        TextView textUserName = view.findViewById(R.id.textUserName);
        String userName = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("name", "집사님");
        textUserName.setText(userName != null && !userName.isEmpty() ? userName : "집사님");

        imageProfile = view.findViewById(R.id.imageProfile);
        androidx.core.widget.ImageViewCompat.setImageTintList(
                imageProfile,
                androidx.core.content.ContextCompat.getColorStateList(requireContext(), R.color.white));
        String savedUri = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE)
                .getString(profileKey(), null);
        if (savedUri != null) {
            loadProfileImage(savedUri);
        }
        view.findViewById(R.id.cardProfileImage).setOnClickListener(v ->
                pickProfileImageLauncher.launch(new String[]{"image/*"}));

        ApiService api = RetrofitClient.getApiService(requireContext());
        loadMultiCatCompare(api, containerMultiCatCompare);
        updateFeedingAlarmSummary();

        View btnFeedingAlarmOpen = view.findViewById(R.id.btnFeedingAlarmOpen);
        if (btnFeedingAlarmOpen != null) {
            btnFeedingAlarmOpen.setOnClickListener(v -> showFeedingAlarmDialog());
        }

        View btnHealthReport = view.findViewById(R.id.btnHealthReport);
        if (btnHealthReport != null) {
            btnHealthReport.setOnClickListener(v -> showHealthReportDialog(api));
        }

        btnCatRegister.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), CatRegisterActivity.class)));

        btnLogout.setOnClickListener(v -> {
            btnLogout.setEnabled(false);
            api.logout().enqueue(new Callback<ApiResponse>() {
                @Override
                public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) { clearAndGoLogin(); }
                @Override
                public void onFailure(Call<ApiResponse> call, Throwable t) { clearAndGoLogin(); }
            });
        });

        return view;
    }

    private void clearAndGoLogin() {
        Context ctx = requireContext();
        // 1. userId 스코핑된 케어 결과는 auth 삭제 전에 먼저 지워야 올바른 키를 읽을 수 있음
        CareResultPrefs.clear(ctx);

        // 2. 실제 AlarmManager PendingIntent를 취소 (prefs 지우기 전에 ID를 읽어야 하므로 먼저 실행)
        FeedingAlarmScheduler.cancelAll(ctx);
        MedicationAlarmScheduler.cancelAll(ctx);
        HealthScheduleAlarmScheduler.cancelAll(ctx);

        // 3. 모든 SharedPreferences 초기화
        ctx.getSharedPreferences("auth",                 Context.MODE_PRIVATE).edit().clear().apply();
        ctx.getSharedPreferences("profile",              Context.MODE_PRIVATE).edit().clear().apply();
        ctx.getSharedPreferences("medication_alarm",     Context.MODE_PRIVATE).edit().clear().apply();
        ctx.getSharedPreferences("feeding_alarm",        Context.MODE_PRIVATE).edit().clear().apply();
        ctx.getSharedPreferences("health_schedule_alarm",Context.MODE_PRIVATE).edit().clear().apply();
        ctx.getSharedPreferences("alarm_cat_ids",        Context.MODE_PRIVATE).edit().clear().apply();

        // 4. Retrofit 싱글톤 초기화 — 다음 로그인 시 새 토큰으로 클라이언트 재생성
        RetrofitClient.reset();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // =========================================================
    // Feeding Alarm
    // =========================================================

    private void updateFeedingAlarmSummary() {
        if (getContext() == null || textFeedingAlarmSummary == null) return;
        java.util.List<FeedingAlarmScheduler.FeedingAlarm> alarms =
                FeedingAlarmScheduler.getAlarms(requireContext());
        if (alarms.isEmpty()) {
            textFeedingAlarmSummary.setText("알림이 설정되지 않았어요");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < alarms.size(); i++) {
            FeedingAlarmScheduler.FeedingAlarm a = alarms.get(i);
            if (i > 0) sb.append("  ·  ");
            boolean am = a.hour < 12;
            int h12 = a.hour % 12 == 0 ? 12 : a.hour % 12;
            sb.append(a.title != null && !a.title.isEmpty() ? a.title : "급여 알림")
              .append(" ")
              .append(am ? "오전" : "오후")
              .append(" ")
              .append(String.format(Locale.getDefault(), "%d:%02d", h12, a.minute));
        }
        textFeedingAlarmSummary.setText(sb.toString());
    }

    private BottomSheetDialog feedingAlarmListDialog;

    private void showFeedingAlarmDialog() {
        if (getContext() == null) return;
        feedingAlarmListDialog = buildFeedingAlarmListDialog();
        feedingAlarmListDialog.show();
    }

    private BottomSheetDialog buildFeedingAlarmListDialog() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        float density = requireContext().getResources().getDisplayMetrics().density;
        int p16 = (int)(16 * density);
        int p24 = (int)(24 * density);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_bottom_sheet));
        root.setPadding(p24, p24, p24, (int)(40 * density));

        // 제목
        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText("급여 알림 관리");
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_on_surface));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.bottomMargin = p16;
        tvTitle.setLayoutParams(titleLp);
        root.addView(tvTitle);

        // 구분선
        View dividerTop = new View(requireContext());
        LinearLayout.LayoutParams divTopLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divTopLp.bottomMargin = p16;
        dividerTop.setLayoutParams(divTopLp);
        dividerTop.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.app_divider));
        root.addView(dividerTop);

        // 알림 목록 스크롤
        ScrollView scrollView = new ScrollView(requireContext());
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        scrollLp.bottomMargin = p16;
        scrollView.setLayoutParams(scrollLp);

        final LinearLayout listContainer = new LinearLayout(requireContext());
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer);
        root.addView(scrollView);

        rebuildFeedingAlarmRows(listContainer);

        // + 알림 추가 버튼
        MaterialButton btnAdd = new MaterialButton(requireContext());
        btnAdd.setText("+ 알림 추가");
        btnAdd.setAllCaps(false);
        LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addLp.topMargin = (int)(4 * density);
        btnAdd.setLayoutParams(addLp);
        btnAdd.setOnClickListener(v -> showAddEditAlarmDialog(null, () -> {
            rebuildFeedingAlarmRows(listContainer);
            updateFeedingAlarmSummary();
        }));
        root.addView(btnAdd);

        sheet.setContentView(root);
        return sheet;
    }

    private void rebuildFeedingAlarmRows(LinearLayout container) {
        container.removeAllViews();
        java.util.List<FeedingAlarmScheduler.FeedingAlarm> alarms =
                FeedingAlarmScheduler.getAlarms(requireContext());
        float density = requireContext().getResources().getDisplayMetrics().density;
        int p8 = (int)(8 * density);

        if (alarms.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("등록된 알림이 없어요");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_on_surface_variant));
            empty.setTextSize(13.5f);
            empty.setPadding(0, p8, 0, p8);
            container.addView(empty);
            return;
        }

        for (FeedingAlarmScheduler.FeedingAlarm alarm : alarms) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, (int)(6 * density), 0, (int)(6 * density));
            row.setLayoutParams(rowLp);

            LinearLayout infoBlock = new LinearLayout(requireContext());
            infoBlock.setOrientation(LinearLayout.VERTICAL);
            infoBlock.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView tvAlarmTitle = new TextView(requireContext());
            tvAlarmTitle.setText(alarm.title != null && !alarm.title.isEmpty() ? alarm.title : "급여 알림");
            tvAlarmTitle.setTextSize(14.5f);
            tvAlarmTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvAlarmTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_on_surface));
            infoBlock.addView(tvAlarmTitle);

            // 오전/오후 + 시간 표시
            boolean isAm = alarm.hour < 12;
            int h12 = alarm.hour % 12 == 0 ? 12 : alarm.hour % 12;
            String amPm = isAm ? "오전" : "오후";
            String timeStr = String.format(Locale.getDefault(), "%s %d:%02d", amPm, h12, alarm.minute);
            if (alarm.memo != null && !alarm.memo.isEmpty()) {
                timeStr = timeStr + "  ·  " + alarm.memo;
            }
            TextView tvAlarmTime = new TextView(requireContext());
            tvAlarmTime.setText(timeStr);
            tvAlarmTime.setTextSize(13f);
            tvAlarmTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_on_surface_variant));
            infoBlock.addView(tvAlarmTime);
            row.addView(infoBlock);

            MaterialButton btnEdit = new MaterialButton(requireContext());
            btnEdit.setText("수정");
            btnEdit.setAllCaps(false);
            btnEdit.setTextSize(12f);
            btnEdit.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnEdit.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_primary_dark));
            btnEdit.setOnClickListener(v -> showAddEditAlarmDialog(alarm, () -> {
                rebuildFeedingAlarmRows(container);
                updateFeedingAlarmSummary();
            }));
            row.addView(btnEdit);

            MaterialButton btnDelete = new MaterialButton(requireContext());
            btnDelete.setText("삭제");
            btnDelete.setAllCaps(false);
            btnDelete.setTextSize(12f);
            btnDelete.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            btnDelete.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            btnDelete.setOnClickListener(v ->
                    new AlertDialog.Builder(requireContext())
                            .setTitle("알림 삭제")
                            .setMessage("'" + (alarm.title != null ? alarm.title : "급여 알림") + "' 알림을 삭제할까요?")
                            .setNegativeButton("취소", null)
                            .setPositiveButton("삭제", (d, w) -> {
                                FeedingAlarmScheduler.deleteAlarm(requireContext(), alarm.id);
                                rebuildFeedingAlarmRows(container);
                                updateFeedingAlarmSummary();
                            }).show()
            );
            row.addView(btnDelete);
            container.addView(row);

            View divider = new View(requireContext());
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1);
            divLp.setMargins(0, (int)(4 * density), 0, (int)(4 * density));
            divider.setLayoutParams(divLp);
            divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.app_divider));
            container.addView(divider);
        }
    }

    private void showAddEditAlarmDialog(FeedingAlarmScheduler.FeedingAlarm existing, Runnable onSaved) {
        if (getContext() == null) return;

        int initH = existing != null ? existing.hour : 8;
        int initM = existing != null ? existing.minute : 0;
        final int[] selH = {initH};
        final int[] selM = {initM};
        final boolean[] isAm = {initH < 12};

        float density = requireContext().getResources().getDisplayMetrics().density;
        int p8  = (int)(8  * density);
        int p16 = (int)(16 * density);
        int p24 = (int)(24 * density);

        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(p24, p16, p24, p8);

        // 제목
        TextView labelTitle = new TextView(requireContext());
        labelTitle.setText("제목");
        labelTitle.setTextSize(12.5f);
        labelTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_on_surface_variant));
        form.addView(labelTitle);

        EditText editTitle = new EditText(requireContext());
        editTitle.setHint("예: 아침 밥");
        editTitle.setText(existing != null && existing.title != null ? existing.title : "");
        editTitle.setTextSize(14f);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.bottomMargin = p16;
        editTitle.setLayoutParams(etLp);
        form.addView(editTitle);

        // 시간 레이블
        TextView labelTime = new TextView(requireContext());
        labelTime.setText("시간");
        labelTime.setTextSize(12.5f);
        labelTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_on_surface_variant));
        form.addView(labelTime);

        // 오전/오후 + 시간 행
        LinearLayout timeRow = new LinearLayout(requireContext());
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams timeRowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        timeRowLp.bottomMargin = p16;
        timeRow.setLayoutParams(timeRowLp);

        // 오전/오후 토글
        MaterialButtonToggleGroup toggleAmPm = new MaterialButtonToggleGroup(requireContext());
        toggleAmPm.setSingleSelection(true);
        toggleAmPm.setSelectionRequired(true);

        MaterialButton btnAm = new MaterialButton(requireContext(),
                null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnAm.setId(View.generateViewId());
        btnAm.setText("오전");
        btnAm.setAllCaps(false);
        btnAm.setTextSize(13f);
        toggleAmPm.addView(btnAm);

        MaterialButton btnPm = new MaterialButton(requireContext(),
                null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnPm.setId(View.generateViewId());
        btnPm.setText("오후");
        btnPm.setAllCaps(false);
        btnPm.setTextSize(13f);
        toggleAmPm.addView(btnPm);

        toggleAmPm.check(isAm[0] ? btnAm.getId() : btnPm.getId());
        LinearLayout.LayoutParams toggleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        toggleLp.rightMargin = p8;
        toggleAmPm.setLayoutParams(toggleLp);
        timeRow.addView(toggleAmPm);

        // 시간 탭 텍스트
        int h12init = selH[0] % 12 == 0 ? 12 : selH[0] % 12;
        TextView tvTimePick = new TextView(requireContext());
        tvTimePick.setText(String.format(Locale.getDefault(), "%d:%02d  ▾", h12init, selM[0]));
        tvTimePick.setTextSize(16f);
        tvTimePick.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_primary_dark));
        tvTimePick.setClickable(true);
        tvTimePick.setFocusable(true);
        tvTimePick.setBackground(requireContext().obtainStyledAttributes(
                new int[]{android.R.attr.selectableItemBackground}).getDrawable(0));
        tvTimePick.setPadding(p8, p8, p8, p8);
        tvTimePick.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        timeRow.addView(tvTimePick);
        form.addView(timeRow);

        // 오전/오후 토글 리스너 — 현재 시간의 AM/PM만 전환
        toggleAmPm.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean nowAm = (checkedId == btnAm.getId());
            if (isAm[0] == nowAm) return;
            isAm[0] = nowAm;
            int hBase = selH[0] % 12; // 0-11
            selH[0] = isAm[0] ? hBase : hBase + 12;
            int h12 = (selH[0] % 12 == 0) ? 12 : (selH[0] % 12);
            tvTimePick.setText(String.format(Locale.getDefault(), "%d:%02d  ▾", h12, selM[0]));
        });

        // 시간 탭 → TimePickerDialog (24h 모드로 열어 혼동 방지)
        tvTimePick.setOnClickListener(v -> {
            new TimePickerDialog(requireContext(), (tp, h, m) -> {
                // h: 0-23 (24h 포맷)
                selH[0] = h;
                selM[0] = m;
                isAm[0] = h < 12;
                // 커스텀 토글 동기화
                toggleAmPm.check(isAm[0] ? btnAm.getId() : btnPm.getId());
                int h12 = (h % 12 == 0) ? 12 : (h % 12);
                tvTimePick.setText(String.format(Locale.getDefault(), "%d:%02d  ▾", h12, m));
            }, selH[0], selM[0], true).show();
        });

        // 메모
        TextView labelMemo = new TextView(requireContext());
        labelMemo.setText("메모 (선택)");
        labelMemo.setTextSize(12.5f);
        labelMemo.setTextColor(ContextCompat.getColor(requireContext(), R.color.app_on_surface_variant));
        form.addView(labelMemo);

        EditText editMemo = new EditText(requireContext());
        editMemo.setHint("알림에 표시될 메모");
        editMemo.setText(existing != null && existing.memo != null ? existing.memo : "");
        editMemo.setTextSize(14f);
        form.addView(editMemo);

        new AlertDialog.Builder(requireContext())
                .setTitle(existing != null ? "알림 수정" : "알림 추가")
                .setView(form)
                .setNegativeButton("취소", null)
                .setPositiveButton("저장", (d, w) -> {
                    String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
                    if (title.isEmpty()) title = "급여 알림";
                    String memo = editMemo.getText() != null ? editMemo.getText().toString().trim() : "";

                    if (existing != null) {
                        FeedingAlarmScheduler.updateAlarm(requireContext(), existing.id,
                                title, selH[0], selM[0], memo);
                        FeedingAlarmScheduler.FeedingAlarm updated =
                                new FeedingAlarmScheduler.FeedingAlarm(
                                        existing.id, title, selH[0], selM[0], memo);
                        FeedingAlarmScheduler.scheduleAlarm(requireContext(), updated);
                    } else {
                        FeedingAlarmScheduler.FeedingAlarm added =
                                FeedingAlarmScheduler.addAlarm(requireContext(),
                                        title, selH[0], selM[0], memo);
                        FeedingAlarmScheduler.scheduleAlarm(requireContext(), added);
                    }
                    Toast.makeText(requireContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show();
                    if (onSaved != null) onSaved.run();
                })
                .show();
    }

    // =========================================================
    // Health Report
    // =========================================================

    private void showHealthReportDialog(ApiService api) {
        if (getContext() == null) return;
        Long catId = getCurrentCatId();
        if (catId == null || catId <= 0) {
            Toast.makeText(requireContext(), "고양이를 먼저 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_health_report, null);

        TextView textWeightChange = dialogView.findViewById(R.id.textReportWeightChange);
        TextView textVomitCount = dialogView.findViewById(R.id.textReportVomitCount);
        TextView textObesityLevel = dialogView.findViewById(R.id.textReportObesityLevel);
        TextView textInsight = dialogView.findViewById(R.id.textReportInsight);
        TextView textLitterCount = dialogView.findViewById(R.id.textReportLitterCount);
        TextView textLitterAbnormal = dialogView.findViewById(R.id.textReportLitterAbnormal);
        TextView textMedCount = dialogView.findViewById(R.id.textReportMedCount);
        TextView textNextSchedule = dialogView.findViewById(R.id.textReportNextSchedule);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialogView.findViewById(R.id.btnReportClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        api.getWeightHistory(catId, 7).enqueue(new Callback<List<WeightHistoryItem>>() {
            @Override
            public void onResponse(Call<List<WeightHistoryItem>> call, Response<List<WeightHistoryItem>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().size() >= 2) {
                    List<WeightHistoryItem> list = response.body();
                    Double latest = list.get(0).getWeightKg();
                    Double oldest = list.get(list.size() - 1).getWeightKg();
                    if (latest != null && oldest != null && textWeightChange != null) {
                        double diff = latest - oldest;
                        String sign = diff >= 0 ? "+" : "";
                        textWeightChange.setText(String.format(Locale.getDefault(), "%s%.2f kg", sign, diff));
                    }
                } else if (response.isSuccessful() && response.body() != null && response.body().size() == 1) {
                    Double w = response.body().get(0).getWeightKg();
                    if (w != null && textWeightChange != null)
                        textWeightChange.setText(String.format(Locale.getDefault(), "%.2f kg", w));
                }
            }
            @Override
            public void onFailure(Call<List<WeightHistoryItem>> call, Throwable t) {}
        });

        api.getObesityHistory(catId, 1).enqueue(new Callback<List<ObesityHistoryItem>>() {
            @Override
            public void onResponse(Call<List<ObesityHistoryItem>> call, Response<List<ObesityHistoryItem>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    ObesityHistoryItem item = response.body().get(0);
                    if (item != null && textObesityLevel != null) {
                        String levelLabel = CareResultPrefs.obesityLevelLabel(
                                item.getObesityLevel() != null ? item.getObesityLevel() : "");
                        Double bf = item.getBodyFatPercent();
                        textObesityLevel.setText(levelLabel + (bf != null
                                ? String.format(Locale.getDefault(), " (체지방 %.1f%%)", bf) : ""));
                    }
                }
            }
            @Override
            public void onFailure(Call<List<ObesityHistoryItem>> call, Throwable t) {}
        });

        Calendar to = Calendar.getInstance();
        Calendar from7 = (Calendar) to.clone();
        from7.add(Calendar.DAY_OF_MONTH, -6);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fromStr = sdf.format(from7.getTime());
        String toStr = sdf.format(to.getTime());

        // 7일 캘린더 이벤트 — 구토 + 화장실 카운트
        api.getCalendarEvents(catId, fromStr, toStr)
                .enqueue(new Callback<List<CalendarEventItem>>() {
                    @Override
                    public void onResponse(Call<List<CalendarEventItem>> call, Response<List<CalendarEventItem>> response) {
                        if (!response.isSuccessful() || response.body() == null) return;
                        int vomitCount = 0, highRisk = 0, litterCount = 0;
                        for (CalendarEventItem e : response.body()) {
                            if (e == null) continue;
                            if ("VOMIT".equals(e.getType())) {
                                vomitCount++;
                                String risk = e.getRiskLevel() != null ? e.getRiskLevel().toUpperCase(Locale.ROOT) : "";
                                if (risk.contains("HIGH")) highRisk++;
                            } else if ("LITTER".equals(e.getType())) {
                                litterCount++;
                            }
                        }
                        // LITTER 이상 여부는 subtitle에 "이상" 포함 여부로 판단
                        int finalLitterAbnormal = 0;
                        for (CalendarEventItem e : response.body()) {
                            if (e == null || !"LITTER".equals(e.getType())) continue;
                            String sub = e.getSubtitle() != null ? e.getSubtitle() : "";
                            if (sub.contains("이상") || sub.contains("비정상")) finalLitterAbnormal++;
                        }
                        if (textVomitCount != null) textVomitCount.setText(vomitCount + "회");
                        if (textLitterCount != null) textLitterCount.setText(litterCount + "회");
                        if (textLitterAbnormal != null && finalLitterAbnormal > 0)
                            textLitterAbnormal.setText("이상 " + finalLitterAbnormal + "회");

                        if (textInsight != null) {
                            StringBuilder insight = new StringBuilder();
                            if (vomitCount == 0) insight.append("이번 주 구토가 없었어요. 건강한 한 주였네요!");
                            else if (highRisk > 0) insight.append("고위험 구토 ").append(highRisk).append("회 — 수의사 상담을 권장해요.");
                            else if (vomitCount >= 4) insight.append("구토가 잦아요. 공복 시간이나 헤어볼을 확인해 보세요.");
                            else insight.append("구토 ").append(vomitCount).append("회 기록됐어요. 꾸준히 모니터링해 주세요.");
                            if (finalLitterAbnormal > 0)
                                insight.append("\n화장실 이상 ").append(finalLitterAbnormal).append("회 감지 — 소변/대변 상태를 확인해 주세요.");
                            insight.append("\n\n꾸준한 기록이 우리 고양이의 건강 지킴이에요.");
                            textInsight.setText(insight.toString());
                        }
                    }
                    @Override
                    public void onFailure(Call<List<CalendarEventItem>> call, Throwable t) {}
                });

        // 투약 중인 약 개수
        api.getMedications(catId).enqueue(new Callback<List<MedicationItem>>() {
            @Override
            public void onResponse(Call<List<MedicationItem>> call, Response<List<MedicationItem>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                String today = sdf.format(new Date());
                int active = 0;
                for (MedicationItem m : response.body()) {
                    if (m == null) continue;
                    String start = m.getStartDate() != null ? m.getStartDate() : "";
                    String end = m.getEndDate() != null ? m.getEndDate() : "";
                    boolean started = start.isEmpty() || today.compareTo(start) >= 0;
                    boolean notEnded = end.isEmpty() || today.compareTo(end) <= 0;
                    if (started && notEnded) active++;
                }
                if (textMedCount != null) textMedCount.setText(active + "종");
            }
            @Override
            public void onFailure(Call<List<MedicationItem>> call, Throwable t) {}
        });

        // 다음 건강일정 (건강검진/예방접종)
        api.getHealthSchedules(catId).enqueue(new Callback<List<HealthScheduleItem>>() {
            @Override
            public void onResponse(Call<List<HealthScheduleItem>> call, Response<List<HealthScheduleItem>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                String today = sdf.format(new Date());
                String nearest = null;
                String nearestName = null;
                for (HealthScheduleItem s : response.body()) {
                    if (s == null || s.getNextDate() == null) continue;
                    if (s.getNextDate().compareTo(today) >= 0) {
                        if (nearest == null || s.getNextDate().compareTo(nearest) < 0) {
                            nearest = s.getNextDate();
                            nearestName = s.getHealthTypeName();
                        }
                    }
                }
                if (textNextSchedule != null) {
                    if (nearest != null) {
                        String label = nearestName != null ? nearestName.replace("건강검진", "검진").replace("예방접종", "접종") : "";
                        textNextSchedule.setText(label + "\n" + nearest);
                    } else {
                        textNextSchedule.setText("일정없음");
                    }
                }
            }
            @Override
            public void onFailure(Call<List<HealthScheduleItem>> call, Throwable t) {}
        });
    }

    // =========================================================
    // Multi Cat Compare
    // =========================================================

    private void loadMultiCatCompare(ApiService api, LinearLayout container) {
        if (api == null || container == null) return;
        container.removeAllViews();
        TextView loading = new TextView(requireContext());
        loading.setText("불러오는 중...");
        loading.setTextColor(getResources().getColor(R.color.app_on_surface_muted));
        container.addView(loading);

        api.getUserCats().enqueue(new Callback<List<CatResponse>>() {
            @Override
            public void onResponse(Call<List<CatResponse>> call, Response<List<CatResponse>> response) {
                if (!isAdded() || getContext() == null) return;
                container.removeAllViews();
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) {
                    TextView empty = new TextView(requireContext());
                    empty.setText("등록된 고양이가 없습니다.");
                    empty.setTextColor(getResources().getColor(R.color.app_on_surface_muted));
                    container.addView(empty);
                    return;
                }
                for (CatResponse cat : response.body()) {
                    if (cat == null || cat.getId() == null) continue;
                    addCompareRow(container, api, cat);
                }
            }
            @Override
            public void onFailure(Call<List<CatResponse>> call, Throwable t) {
                if (!isAdded() || getContext() == null) return;
                container.removeAllViews();
                TextView fail = new TextView(requireContext());
                fail.setText("비교 데이터를 불러오지 못했습니다.");
                fail.setTextColor(getResources().getColor(R.color.app_on_surface_muted));
                container.addView(fail);
            }
        });
    }

    private void addCompareRow(LinearLayout parent, ApiService api, CatResponse cat) {
        MaterialCardView card = new MaterialCardView(requireContext());
        card.setCardBackgroundColor(getResources().getColor(R.color.app_surface_elevated));
        card.setRadius(18f);
        card.setStrokeColor(getResources().getColor(R.color.app_outline));
        card.setStrokeWidth(1);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 8;
        card.setLayoutParams(lp);

        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(16, 12, 16, 12);
        card.addView(row);

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(R.drawable.ic_cat);
        icon.setColorFilter(getResources().getColor(R.color.app_primary_dark));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(40, 40);
        icon.setLayoutParams(ilp);
        header.addView(icon);

        TextView title = new TextView(requireContext());
        String catDisplayName = cat.getName() != null ? cat.getName() : "고양이";
        if (cat.getAge() != null && cat.getAge() > 0) {
            catDisplayName += " | " + cat.getAge() + "살";
        }
        title.setText(catDisplayName);
        title.setTextSize(15f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(getResources().getColor(R.color.app_on_surface));
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = 8;
        title.setLayoutParams(tlp);
        header.addView(title);
        row.addView(header);

        TextView body = new TextView(requireContext());
        body.setText("몸무게\n-\n\n다음 일정\n-\n\n최근 7일 토 기록\n-");
        body.setTextSize(12.5f);
        body.setTextColor(getResources().getColor(R.color.app_on_surface_variant));
        body.setLineSpacing(3f, 1.2f);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyLp.topMargin = 8;
        body.setLayoutParams(bodyLp);
        row.addView(body);
        parent.addView(card);

        final String[] latestWeight = {"-"};
        final String[] nextHealthDate = {"-"};
        final String[] vomit7 = {"-"};
        final int[] done = {0};

        Runnable render = () -> body.setText(
                "몸무게\n" + latestWeight[0] + "\n\n다음 일정\n" + nextHealthDate[0]
                        + "\n\n최근 7일 토 기록\n" + vomit7[0]);
        Runnable markDone = () -> { done[0]++; render.run(); };

        api.getWeightHistory(cat.getId(), 1).enqueue(new Callback<List<WeightHistoryItem>>() {
            @Override
            public void onResponse(Call<List<WeightHistoryItem>> call, Response<List<WeightHistoryItem>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    WeightHistoryItem it = response.body().get(0);
                    if (it != null && it.getWeightKg() != null)
                        latestWeight[0] = String.format(Locale.getDefault(), "%.2fkg", it.getWeightKg());
                }
                markDone.run();
            }
            @Override
            public void onFailure(Call<List<WeightHistoryItem>> call, Throwable t) { markDone.run(); }
        });

        api.getHealthSchedules(cat.getId()).enqueue(new Callback<List<HealthScheduleItem>>() {
            @Override
            public void onResponse(Call<List<HealthScheduleItem>> call, Response<List<HealthScheduleItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String min = null;
                    for (HealthScheduleItem h : response.body()) {
                        if (h == null || h.getNextDate() == null) continue;
                        if (min == null || h.getNextDate().compareTo(min) < 0) min = h.getNextDate();
                    }
                    if (min != null) nextHealthDate[0] = min;
                }
                markDone.run();
            }
            @Override
            public void onFailure(Call<List<HealthScheduleItem>> call, Throwable t) { markDone.run(); }
        });

        Calendar to = Calendar.getInstance();
        Calendar from = (Calendar) to.clone();
        from.add(Calendar.DAY_OF_MONTH, -6);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        api.getCalendarEvents(cat.getId(), sdf.format(from.getTime()), sdf.format(to.getTime()))
                .enqueue(new Callback<List<CalendarEventItem>>() {
                    @Override
                    public void onResponse(Call<List<CalendarEventItem>> call, Response<List<CalendarEventItem>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            int c = 0;
                            for (CalendarEventItem item : response.body()) {
                                if (item != null && "VOMIT".equals(item.getType())) c++;
                            }
                            vomit7[0] = c + "회";
                        }
                        markDone.run();
                    }
                    @Override
                    public void onFailure(Call<List<CalendarEventItem>> call, Throwable t) { markDone.run(); }
                });
    }

    private Long getCurrentCatId() {
        if (getContext() == null) return null;
        return requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getLong("lastViewedCatId", -1L);
    }

    private Double parseDoubleLocal(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return null; }
    }

    private String profileKey() {
        if (getContext() == null) return "profile_image_uri_cat_0";
        long catId = getContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getLong("lastViewedCatId", 0L);
        return "profile_image_uri_cat_" + catId;
    }

    private void saveProfileImageUri(String uriStr) {
        if (getContext() == null) return;
        getContext().getSharedPreferences("profile", Context.MODE_PRIVATE)
                .edit().putString(profileKey(), uriStr).apply();
    }

    private void loadProfileImage(String uriStr) {
        if (imageProfile == null || getContext() == null) return;
        try {
            Uri uri = Uri.parse(uriStr);
            Bitmap bitmap = BitmapFactory.decodeStream(
                    getContext().getContentResolver().openInputStream(uri));
            if (bitmap != null) {
                imageProfile.setImageBitmap(bitmap);
                imageProfile.setPadding(0, 0, 0, 0);
                androidx.core.widget.ImageViewCompat.setImageTintList(imageProfile, null);
            }
        } catch (Exception ignored) {}
    }
}
