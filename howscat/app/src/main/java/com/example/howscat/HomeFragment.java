package com.example.howscat;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.materialswitch.MaterialSwitch;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.howscat.dto.AiSummaryResponse;
import com.example.howscat.dto.HealthScheduleItem;
import com.example.howscat.dto.HealthScheduleUpdateRequest;
import com.example.howscat.network.ApiService;
import com.example.howscat.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        View btnQuickCare = view.findViewById(R.id.btnQuickCare);
        View btnQuickHospital = view.findViewById(R.id.btnQuickHospital);
        View btnQuickCalendar = view.findViewById(R.id.btnQuickCalendar);
        TextView textHomeTip = view.findViewById(R.id.textHomeTip);

        TextView textNextCheckup = view.findViewById(R.id.textNextHomeCheckup);
        TextView textNextVaccine = view.findViewById(R.id.textNextHomeVaccine);

        btnQuickCare.setOnClickListener(v -> switchFragment(new CatFragment()));
        btnQuickHospital.setOnClickListener(v -> switchFragment(new HospitalFragment()));
        btnQuickCalendar.setOnClickListener(v -> switchFragment(new CalendarFragment()));

        textHomeTip.setText(pickRandomTip());

        refreshCareSummaryCard(view);

        long catId = getCurrentCatId();
        if (catId > 0) {
            ApiService api = RetrofitClient.getApiService(requireContext());
            loadNextHealthSchedules(api, catId, textNextCheckup, textNextVaccine);
            loadAiSummary(api, catId, view.findViewById(R.id.textAiSummary));
            if (!CareResultPrefs.hasSummaryForCat(requireContext(), catId)) {
                restoreCareSummaryFromServer(api, catId, view);
            }
        } else {
            textNextCheckup.setText("건강검진 · 고양이 선택 필요");
            textNextVaccine.setText("예방접종 · 고양이 선택 필요");
            TextView textAiSummary = view.findViewById(R.id.textAiSummary);
            if (textAiSummary != null) textAiSummary.setText("🐾 고양이를 선택하면 AI 분석이 시작돼요.");
        }

        animateHomeEntry(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) {
            refreshCareSummaryCard(v);
        }
    }

    private void refreshCareSummaryCard(@NonNull View root) {
        View card = root.findViewById(R.id.cardCareSummary);
        TextView text = root.findViewById(R.id.textCareSummary);
        if (card == null || text == null) return;
        long catId = getCurrentCatId();
        card.setVisibility(View.VISIBLE);
        if (catId > 0 && CareResultPrefs.hasSummaryForCat(requireContext(), catId)) {
            text.setText(CareResultPrefs.getSummaryText(requireContext(), catId));
            return;
        }
        if (catId <= 0) {
            text.setText("고양이를 선택하면 최신 지급량이 표시돼요.");
        } else {
            text.setText("아직 계산 결과가 없어요.\n케어 탭에서 물·사료 계산을 해주세요.");
        }
    }

    private String pickRandomTip() {
        String[] tips = getResources().getStringArray(R.array.home_tips);
        if (tips == null || tips.length == 0) return "";
        return tips[new Random().nextInt(tips.length)];
    }

    private long getCurrentCatId() {
        return requireContext()
                .getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                .getLong("lastViewedCatId", -1L);
    }

    private void loadAiSummary(ApiService api, long catId, TextView textAiSummary) {
        if (textAiSummary == null) return;
        textAiSummary.setText("분석 중...");
        api.getAiHealthSummary(catId).enqueue(new Callback<AiSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<AiSummaryResponse> call,
                                   @NonNull Response<AiSummaryResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getSummary() != null) {
                    textAiSummary.setText(response.body().getSummary());
                } else if (response.isSuccessful()) {
                    textAiSummary.setText("아직 분석할 기록이 없어요. 케어 탭에서 기록을 추가해 주세요.");
                } else {
                    textAiSummary.setText("🐾 AI 분석을 불러오지 못했습니다. (HTTP " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<AiSummaryResponse> call, @NonNull Throwable t) {
                textAiSummary.setText("🐾 네트워크 오류로 AI 분석을 불러오지 못했습니다.");
            }
        });
    }

    private void loadNextHealthSchedules(
            ApiService api,
            long catId,
            TextView textNextCheckup,
            TextView textNextVaccine
    ) {
        textNextCheckup.setText("건강검진 · 불러오는 중...");
        textNextVaccine.setText("예방접종 · 불러오는 중...");

        api.getHealthSchedules(catId).enqueue(new Callback<List<HealthScheduleItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<HealthScheduleItem>> call,
                                   @NonNull Response<List<HealthScheduleItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    textNextCheckup.setText("건강검진 · 서버 오류");
                    textNextVaccine.setText("예방접종 · 서버 오류");
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                sdf.setLenient(false);

                Calendar nowCal = Calendar.getInstance();
                nowCal.setTime(new Date());
                nowCal.set(Calendar.HOUR_OF_DAY, 0);
                nowCal.set(Calendar.MINUTE, 0);
                nowCal.set(Calendar.SECOND, 0);
                nowCal.set(Calendar.MILLISECOND, 0);
                long nowMidnightMillis = nowCal.getTimeInMillis();

                HealthScheduleItem nextCheckupFuture = null;
                long nextCheckupFutureMillis = Long.MAX_VALUE;
                HealthScheduleItem nextVaccineFuture = null;
                long nextVaccineFutureMillis = Long.MAX_VALUE;

                for (HealthScheduleItem item : response.body()) {
                    if (item == null || item.getNextDate() == null) continue;

                    Date d;
                    try {
                        d = sdf.parse(item.getNextDate());
                    } catch (Exception e) {
                        continue;
                    }
                    long dMillis = d.getTime();
                    if (dMillis < nowMidnightMillis) continue;

                    String typeName = item.getHealthTypeName() != null ? item.getHealthTypeName() : "";
                    boolean isCheckup = typeName.contains("검진");
                    boolean isVaccine = typeName.contains("접종") || typeName.contains("예방");
                    if (!isCheckup && !isVaccine) continue;

                    if (isCheckup) {
                        if (nextCheckupFuture == null || dMillis < nextCheckupFutureMillis) {
                            nextCheckupFuture = item;
                            nextCheckupFutureMillis = dMillis;
                        }
                    } else if (isVaccine) {
                        if (nextVaccineFuture == null || dMillis < nextVaccineFutureMillis) {
                            nextVaccineFuture = item;
                            nextVaccineFutureMillis = dMillis;
                        }
                    }
                }

                final HealthScheduleItem nextCheckupToEdit = nextCheckupFuture;
                final HealthScheduleItem nextVaccineToEdit = nextVaccineFuture;

                if (nextCheckupToEdit != null) {
                    textNextCheckup.setText("건강검진 · " + nextCheckupToEdit.getNextDate());
                } else {
                    textNextCheckup.setText("건강검진 · 일정 없음");
                }

                if (nextVaccineToEdit != null) {
                    textNextVaccine.setText("예방접종 · " + nextVaccineToEdit.getNextDate());
                } else {
                    textNextVaccine.setText("예방접종 · 일정 없음");
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<HealthScheduleItem>> call, @NonNull Throwable t) {
                textNextCheckup.setText("건강검진 · 실패");
                textNextVaccine.setText("예방접종 · 실패");
            }
        });
    }

    private void showHealthScheduleEditDialog(
            ApiService api,
            long catId,
            HealthScheduleItem item
    ) {
        if (api == null || item == null || item.getHealthScheduleId() == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        sdf.setLenient(false);

        Calendar pickedCal = Calendar.getInstance();
        pickedCal.setTime(new Date());
        try {
            if (item.getNextDate() != null) {
                Date d = sdf.parse(item.getNextDate());
                pickedCal.setTime(d);
            }
        } catch (Exception ignored) {
        }

        final Calendar picked = pickedCal;

        MaterialSwitch switchAlarm = new MaterialSwitch(requireContext());
        switchAlarm.setText("알림 사용");
        switchAlarm.setChecked(item.getAlarmEnabled() != null && item.getAlarmEnabled());

        TextView labelCycle = new TextView(requireContext());
        labelCycle.setText("커스텀 주기(개월, 비우면 유지)");
        labelCycle.setTextColor(ContextCompat.getColor(requireContext(), R.color.cat_text_secondary));
        labelCycle.setTextSize(12.5f);

        EditText editCycleMonth = new EditText(requireContext());
        editCycleMonth.setHint("예: 6");
        editCycleMonth.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (item.getCustomCycleMonth() != null) editCycleMonth.setText(String.valueOf(item.getCustomCycleMonth()));

        TextView labelNextDate = new TextView(requireContext());
        labelNextDate.setText("다음 날짜(yyyy-MM-dd)");
        labelNextDate.setTextColor(ContextCompat.getColor(requireContext(), R.color.cat_text_secondary));
        labelNextDate.setTextSize(12.5f);

        EditText editNextDate = new EditText(requireContext());
        editNextDate.setHint("날짜 선택");
        editNextDate.setFocusable(false);
        editNextDate.setText(sdf.format(picked.getTime()));
        editNextDate.setOnClickListener(v -> {
            DatePickerDialog dlg = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        picked.set(Calendar.YEAR, year);
                        picked.set(Calendar.MONTH, month);
                        picked.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        editNextDate.setText(sdf.format(picked.getTime()));
                    },
                    picked.get(Calendar.YEAR),
                    picked.get(Calendar.MONTH),
                    picked.get(Calendar.DAY_OF_MONTH)
            );
            dlg.show();
        });

        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(24, 20, 24, 10);

        container.addView(labelCycle);
        container.addView(editCycleMonth);
        container.addView(labelNextDate);
        container.addView(editNextDate);
        container.addView(switchAlarm);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(item.getHealthTypeName() != null ? item.getHealthTypeName() + " 일정 수정" : "일정 수정")
                .setView(container)
                .setNegativeButton("취소", (d, which) -> d.dismiss())
                .setPositiveButton("저장", (d, which) -> {
                    String cycleStr = editCycleMonth.getText() != null ? editCycleMonth.getText().toString().trim() : "";
                    Integer cycle = null;
                    if (!cycleStr.isEmpty()) {
                        try {
                            cycle = Integer.parseInt(cycleStr);
                        } catch (Exception ignored) {
                        }
                    }

                    String nextDateStr = editNextDate.getText() != null ? editNextDate.getText().toString().trim() : null;
                    HealthScheduleUpdateRequest req = new HealthScheduleUpdateRequest(
                            nextDateStr,
                            cycle,
                            switchAlarm.isChecked()
                    );

                    api.updateHealthSchedule(catId, item.getHealthScheduleId(), req).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "저장 완료", Toast.LENGTH_SHORT).show();
                                HealthScheduleAlarmScheduler.syncAlarms(requireContext(), catId);

                                View root = requireView();
                                TextView tCheck = root.findViewById(R.id.textNextHomeCheckup);
                                TextView tVaccine = root.findViewById(R.id.textNextHomeVaccine);
                                loadNextHealthSchedules(api, catId, tCheck, tVaccine);
                            } else {
                                Toast.makeText(requireContext(), "저장 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                            Toast.makeText(requireContext(), "저장 실패: 네트워크 오류", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .create();

        dialog.show();
    }

    private void restoreCareSummaryFromServer(ApiService api, long catId, View root) {
        api.getWeightHistory(catId, 1).enqueue(new Callback<java.util.List<com.example.howscat.dto.WeightHistoryItem>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.List<com.example.howscat.dto.WeightHistoryItem>> call,
                                   @NonNull Response<java.util.List<com.example.howscat.dto.WeightHistoryItem>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    com.example.howscat.dto.WeightHistoryItem latest = response.body().get(0);
                    Double water  = latest.getRecommendedWaterMl();
                    Double food   = latest.getRecommendedFoodG();
                    Double weight = latest.getWeightKg();
                    if (water != null && water > 0 && food != null && food > 0 && weight != null) {
                        CareResultPrefs.saveWaterFood(requireContext(), catId, water, food, weight);
                        refreshCareSummaryCard(root);
                        return;
                    }
                }
                // 물·사료 계산 기록 없으면 비만도 기록에서 복원 시도
                restoreFromObesityHistory(api, catId, root);
            }

            @Override
            public void onFailure(@NonNull Call<java.util.List<com.example.howscat.dto.WeightHistoryItem>> call,
                                  @NonNull Throwable t) {
                restoreFromObesityHistory(api, catId, root);
            }
        });
    }

    private void restoreFromObesityHistory(ApiService api, long catId, View root) {
        api.getObesityHistory(catId, 1).enqueue(new Callback<java.util.List<com.example.howscat.dto.ObesityHistoryItem>>() {
            @Override
            public void onResponse(@NonNull Call<java.util.List<com.example.howscat.dto.ObesityHistoryItem>> call,
                                   @NonNull Response<java.util.List<com.example.howscat.dto.ObesityHistoryItem>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().isEmpty()) return;
                com.example.howscat.dto.ObesityHistoryItem latest = response.body().get(0);
                Double water = latest.getRecommendedWater();
                Double food  = latest.getRecommendedFood();
                if (water != null && water > 0 && food != null && food > 0) {
                    CareResultPrefs.saveObesity(requireContext(), catId, water, food,
                            latest.getObesityLevel() != null ? latest.getObesityLevel() : "");
                    refreshCareSummaryCard(root);
                }
            }

            @Override
            public void onFailure(@NonNull Call<java.util.List<com.example.howscat.dto.ObesityHistoryItem>> call,
                                  @NonNull Throwable t) {
                // 조용히 무시
            }
        });
    }

    private void switchFragment(@NonNull Fragment fragment) {
        if (getActivity() instanceof AppCompatActivity) {
            ((AppCompatActivity) getActivity()).getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
        }
    }

    private void animateHomeEntry(View root) {
        if (!(root instanceof ViewGroup)) return;
        ViewGroup vg = (ViewGroup) root;
        if (vg.getChildCount() <= 0 || !(vg.getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup content = (ViewGroup) vg.getChildAt(0);
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(20f);
            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(i * 35L)
                    .setDuration(240)
                    .start();
        }
    }
}