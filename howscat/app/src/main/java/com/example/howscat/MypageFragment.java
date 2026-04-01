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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

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
        requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // =========================================================
    // Feeding Alarm
    // =========================================================

    private void updateFeedingAlarmSummary() {
        if (getContext() == null || textFeedingAlarmSummary == null) return;
        boolean morningOn = FeedingAlarmScheduler.isMorningEnabled(requireContext());
        boolean eveningOn = FeedingAlarmScheduler.isEveningEnabled(requireContext());
        if (!morningOn && !eveningOn) {
            textFeedingAlarmSummary.setText("알림이 설정되지 않았어요");
            return;
        }
        StringBuilder sb = new StringBuilder();
        if (morningOn) {
            sb.append("아침 ").append(String.format(Locale.getDefault(), "%02d:%02d",
                    FeedingAlarmScheduler.getMorningHour(requireContext()),
                    FeedingAlarmScheduler.getMorningMinute(requireContext())));
        }
        if (eveningOn) {
            if (sb.length() > 0) sb.append("  ·  ");
            sb.append("저녁 ").append(String.format(Locale.getDefault(), "%02d:%02d",
                    FeedingAlarmScheduler.getEveningHour(requireContext()),
                    FeedingAlarmScheduler.getEveningMinute(requireContext())));
        }
        textFeedingAlarmSummary.setText(sb.toString());
    }

    private void showFeedingAlarmDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_feeding_alarm, null);

        MaterialSwitch switchMorning = dialogView.findViewById(R.id.switchMorningAlarm);
        MaterialSwitch switchEvening = dialogView.findViewById(R.id.switchEveningAlarm);
        TextView textMorningTime = dialogView.findViewById(R.id.textMorningTime);
        TextView textEveningTime = dialogView.findViewById(R.id.textEveningTime);

        final int[] mH = {FeedingAlarmScheduler.getMorningHour(requireContext())};
        final int[] mM = {FeedingAlarmScheduler.getMorningMinute(requireContext())};
        final int[] eH = {FeedingAlarmScheduler.getEveningHour(requireContext())};
        final int[] eM = {FeedingAlarmScheduler.getEveningMinute(requireContext())};

        if (switchMorning != null) switchMorning.setChecked(FeedingAlarmScheduler.isMorningEnabled(requireContext()));
        if (switchEvening != null) switchEvening.setChecked(FeedingAlarmScheduler.isEveningEnabled(requireContext()));
        if (textMorningTime != null)
            textMorningTime.setText(String.format(Locale.getDefault(), "%02d:%02d", mH[0], mM[0]));
        if (textEveningTime != null)
            textEveningTime.setText(String.format(Locale.getDefault(), "%02d:%02d", eH[0], eM[0]));

        if (textMorningTime != null) {
            textMorningTime.setOnClickListener(v ->
                    new TimePickerDialog(requireContext(), (tp, h, m) -> {
                        mH[0] = h; mM[0] = m;
                        textMorningTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
                    }, mH[0], mM[0], true).show());
        }
        if (textEveningTime != null) {
            textEveningTime.setOnClickListener(v ->
                    new TimePickerDialog(requireContext(), (tp, h, m) -> {
                        eH[0] = h; eM[0] = m;
                        textEveningTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, m));
                    }, eH[0], eM[0], true).show());
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView).create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.btnFeedingAlarmSave).setOnClickListener(v -> {
            boolean mOn = switchMorning != null && switchMorning.isChecked();
            boolean eOn = switchEvening != null && switchEvening.isChecked();
            FeedingAlarmScheduler.saveMorning(requireContext(), mOn, mH[0], mM[0]);
            FeedingAlarmScheduler.saveEvening(requireContext(), eOn, eH[0], eM[0]);
            FeedingAlarmScheduler.scheduleAlarms(requireContext());
            updateFeedingAlarmSummary();
            Toast.makeText(requireContext(), "급여 알림이 설정되었습니다.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.btnFeedingAlarmClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
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
                        int vomitCount = 0, highRisk = 0, litterCount = 0, litterAbnormal = 0;
                        for (CalendarEventItem e : response.body()) {
                            if (e == null) continue;
                            if ("VOMIT".equals(e.getType())) {
                                vomitCount++;
                                String risk = e.getRiskLevel() != null ? e.getRiskLevel().toUpperCase(Locale.ROOT) : "";
                                if (risk.contains("HIGH")) highRisk++;
                            } else if ("LITTER".equals(e.getType())) {
                                litterCount++;
                                if (Boolean.TRUE.equals(e.getAlarmEnabled())) litterAbnormal++; // alarmEnabled 필드 재사용 안 되므로 subtitle로 체크
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
