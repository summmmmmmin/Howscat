package com.example.howscat;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.example.howscat.dto.ApiResponse;
import com.example.howscat.dto.CareWeightRequest;
import com.example.howscat.dto.CatResponse;
import com.example.howscat.dto.WeightGoalRequest;
import com.example.howscat.dto.CalendarEventItem;
import com.example.howscat.dto.LitterBoxCreateRequest;
import com.example.howscat.dto.MedicationCreateRequest;
import com.example.howscat.dto.MedicationItem;
import com.example.howscat.dto.ObesityCheckRequest;
import com.example.howscat.dto.ObesityCheckResponse;
import com.example.howscat.dto.ObesityHistoryItem;
import com.example.howscat.dto.VetVisitCreateRequest;
import com.example.howscat.dto.VetVisitItem;
import com.example.howscat.dto.WeightHistoryItem;
import com.example.howscat.network.ApiService;
import com.example.howscat.network.RetrofitClient;
import com.example.howscat.widget.SimpleLineChartView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CatFragment extends Fragment {

    // 목표 체중: 서버에서 로드, 로컬 캐시 (앱 재설치 후에도 유지)
    private float cachedWeightGoal = 0f;

    private SimpleLineChartView chartWeightTrend;
    private SimpleLineChartView chartObesityTrend;
    private SimpleLineChartView chartVomitTrend;
    private TextView textVomitTrendSummary;
    private TextView textGoalCurrentWeightDisplay;
    private TextView textGoalTargetWeightDisplay;
    private LinearProgressIndicator progressWeightGoal;

    public CatFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             android.os.Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cat, container, false);

        View btnWaterFood = view.findViewById(R.id.calculateButton);
        View btnObesity = view.findViewById(R.id.obesity_calculateButton);
        View btnVomit = view.findViewById(R.id.vomit_analyzeButton);
        View btnMedication = view.findViewById(R.id.btnMedication);
        View btnLitterBox = view.findViewById(R.id.btnLitterBox);
        View btnVetVisit = view.findViewById(R.id.btnVetVisit);
        View btnWeightGoal = view.findViewById(R.id.btnWeightGoal);

        chartWeightTrend = view.findViewById(R.id.chartWeightTrend);
        chartObesityTrend = view.findViewById(R.id.chartObesityTrend);
        chartVomitTrend = view.findViewById(R.id.chartVomitTrend);
        textVomitTrendSummary = view.findViewById(R.id.textVomitTrendSummary);
        textGoalCurrentWeightDisplay = view.findViewById(R.id.textGoalCurrentWeightDisplay);
        textGoalTargetWeightDisplay = view.findViewById(R.id.textGoalTargetWeightDisplay);
        progressWeightGoal = view.findViewById(R.id.progressWeightGoal);

        btnWaterFood.setOnClickListener(v -> showWaterFoodCalcDialog());
        btnObesity.setOnClickListener(v -> showObesityCheckDialog());
        btnVomit.setOnClickListener(v -> {
            if (getActivity() != null) {
                startActivity(new android.content.Intent(getActivity(), VomitAnalyzeActivity.class));
            }
        });
        btnMedication.setOnClickListener(v -> showMedicationDialog());
        btnLitterBox.setOnClickListener(v -> showLitterBoxDialog());
        btnVetVisit.setOnClickListener(v -> showVetVisitDialog());
        btnWeightGoal.setOnClickListener(v -> showWeightGoalDialog());

        loadWeightAndObesityHistory();
        loadWeightGoalFromServer();
        animateCatEntry(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWeightAndObesityHistory();
    }

    // =========================================================
    // Weight Goal
    // =========================================================

    private void updateWeightGoalDisplay(Float currentWeight) {
        if (getContext() == null) return;
        float goal = getWeightGoal();
        if (textGoalTargetWeightDisplay != null) {
            textGoalTargetWeightDisplay.setText(goal > 0
                    ? String.format(Locale.getDefault(), "%.1f kg", goal)
                    : "미설정");
        }
        if (currentWeight != null && textGoalCurrentWeightDisplay != null) {
            textGoalCurrentWeightDisplay.setText(
                    String.format(Locale.getDefault(), "%.2f kg", currentWeight));
            if (progressWeightGoal != null && goal > 0) {
                int pct = (int) Math.min(100, (currentWeight / goal) * 100);
                progressWeightGoal.setProgress(pct);
            }
        }
    }

    private float getWeightGoal() {
        return cachedWeightGoal;
    }

    private void loadWeightGoalFromServer() {
        if (!isAdded() || getContext() == null) return;
        Long catId = getCurrentCatId();
        if (catId == null || catId <= 0) return;

        RetrofitClient.getApiService(requireContext()).getCat(catId)
                .enqueue(new retrofit2.Callback<CatResponse>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<CatResponse> call,
                                           @NonNull retrofit2.Response<CatResponse> response) {
                        if (!isAdded() || getContext() == null) return;
                        if (response.isSuccessful() && response.body() != null) {
                            Float goal = response.body().getWeightGoal();
                            cachedWeightGoal = goal != null ? goal : 0f;
                            updateWeightGoalDisplay(null);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull retrofit2.Call<CatResponse> call, @NonNull Throwable t) {
                        // 실패 시 cachedWeightGoal 그대로 유지 (기본값 0)
                    }
                });
    }

    private void saveWeightGoal(float goal) {
        if (!isAdded() || getContext() == null) return;
        Long catId = getCurrentCatId();
        if (catId == null || catId <= 0) return;

        float previous = cachedWeightGoal;
        cachedWeightGoal = goal; // 낙관적 업데이트

        RetrofitClient.getApiService(requireContext())
                .updateWeightGoal(catId, new WeightGoalRequest(goal))
                .enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<Void> call,
                                           @NonNull retrofit2.Response<Void> response) {
                        if (!isAdded() || getContext() == null) return;
                        if (!response.isSuccessful()) {
                            cachedWeightGoal = previous; // 실패 시 롤백
                            updateWeightGoalDisplay(null);
                            Toast.makeText(requireContext(),
                                    "목표 체중 저장 실패 (HTTP " + response.code() + ")",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull retrofit2.Call<Void> call, @NonNull Throwable t) {
                        cachedWeightGoal = previous; // 실패 시 롤백
                        if (isAdded() && getContext() != null) {
                            updateWeightGoalDisplay(null);
                            Toast.makeText(requireContext(),
                                    "목표 체중 저장 실패 (네트워크 오류)",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showWeightGoalDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_weight_goal, null);

        TextView textCurrentWeight = dialogView.findViewById(R.id.textGoalCurrentWeight);
        TextView textTargetDisplay = dialogView.findViewById(R.id.textGoalTargetDisplay);
        EditText editGoal = dialogView.findViewById(R.id.editGoalWeight);

        float currentGoal = getWeightGoal();
        if (textTargetDisplay != null && currentGoal > 0) {
            textTargetDisplay.setText(String.format(Locale.getDefault(), "%.1f kg", currentGoal));
        }
        if (editGoal != null && currentGoal > 0) {
            editGoal.setText(String.format(Locale.getDefault(), "%.1f", currentGoal));
        }

        ApiService api = RetrofitClient.getApiService(requireContext());
        Long catId = getCurrentCatId();
        if (catId != null && catId > 0) {
            api.getWeightHistory(catId, 1).enqueue(new retrofit2.Callback<List<WeightHistoryItem>>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<List<WeightHistoryItem>> call,
                                       @NonNull retrofit2.Response<List<WeightHistoryItem>> resp) {
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        Double w = resp.body().get(0).getWeightKg();
                        if (w != null && textCurrentWeight != null) {
                            textCurrentWeight.setText(String.format(Locale.getDefault(), "%.2f kg", w));
                        }
                    }
                }
                @Override
                public void onFailure(@NonNull retrofit2.Call<List<WeightHistoryItem>> call, @NonNull Throwable t) {}
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogView.findViewById(R.id.btnGoalSave).setOnClickListener(v -> {
            String s = editGoal.getText() != null ? editGoal.getText().toString().trim() : "";
            if (s.isEmpty()) {
                Toast.makeText(requireContext(), "목표 체중을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                float goal = Float.parseFloat(s);
                if (goal <= 0 || goal > 30) {
                    Toast.makeText(requireContext(), "올바른 체중을 입력해 주세요 (0.1 ~ 30kg).", Toast.LENGTH_SHORT).show();
                    return;
                }
                saveWeightGoal(goal);
                updateWeightGoalDisplay(null);
                Toast.makeText(requireContext(), "체중 목표가 설정되었습니다!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "숫자를 입력해 주세요.", Toast.LENGTH_SHORT).show();
            }
        });
        dialogView.findViewById(R.id.btnGoalCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // =========================================================
    // Medication
    // =========================================================

    private void showMedicationDialog() {
        if (getContext() == null) return;
        Long catId = getCurrentCatId();
        if (catId == null || catId <= 0) {
            Toast.makeText(requireContext(), "고양이를 먼저 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_medication_add, null);

        EditText editName = dialogView.findViewById(R.id.editMedName);
        EditText editDosage = dialogView.findViewById(R.id.editMedDosage);
        MaterialButtonToggleGroup toggleFreq = dialogView.findViewById(R.id.toggleMedFrequency);
        EditText editStartDate = dialogView.findViewById(R.id.editMedStartDate);
        EditText editEndDate = dialogView.findViewById(R.id.editMedEndDate);
        TextView textAlarmTime = dialogView.findViewById(R.id.textMedAlarmTime);
        MaterialButtonToggleGroup toggleMeridiem = dialogView.findViewById(R.id.toggleMedMeridiem);
        MaterialSwitch switchAlarm = dialogView.findViewById(R.id.switchMedAlarm);
        EditText editNotes = dialogView.findViewById(R.id.editMedNotes);
        View labelTime1 = dialogView.findViewById(R.id.labelMedAlarmTime1);
        View labelTime2 = dialogView.findViewById(R.id.labelMedAlarmTime2);
        View layoutTime2 = dialogView.findViewById(R.id.layoutMedAlarmTime2);
        MaterialButtonToggleGroup toggleMeridiem2 = dialogView.findViewById(R.id.toggleMedMeridiem2);
        TextView textAlarmTime2 = dialogView.findViewById(R.id.textMedAlarmTime2);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (editStartDate != null) editStartDate.setText(today);

        final int[] alarmHour = {9};
        final int[] alarmMinute = {0};
        final boolean[] isAm = {true};
        final int[] alarmHour2 = {21};
        final int[] alarmMinute2 = {0};
        final boolean[] isAm2 = {false};

        if (toggleFreq != null) toggleFreq.check(R.id.btnFreqOnce);
        if (toggleMeridiem != null) toggleMeridiem.check(R.id.btnMeridiemAm);
        if (toggleMeridiem2 != null) toggleMeridiem2.check(R.id.btnMeridiemPm2);

        Runnable updateTwiceVis = () -> {
            boolean isTwice = toggleFreq != null
                    && toggleFreq.getCheckedButtonId() == R.id.btnFreqTwice;
            int vis = isTwice ? View.VISIBLE : View.GONE;
            if (labelTime1 != null) labelTime1.setVisibility(vis);
            if (labelTime2 != null) labelTime2.setVisibility(vis);
            if (layoutTime2 != null) layoutTime2.setVisibility(vis);
        };
        updateTwiceVis.run();
        if (toggleFreq != null) {
            toggleFreq.addOnButtonCheckedListener((g, id, chk) -> { if (chk) updateTwiceVis.run(); });
        }

        if (toggleMeridiem != null) {
            toggleMeridiem.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                isAm[0] = (checkedId == R.id.btnMeridiemAm);
                int h12 = alarmHour[0] % 12 == 0 ? 12 : alarmHour[0] % 12;
                alarmHour[0] = isAm[0] ? (h12 % 12) : (h12 % 12 + 12);
                if (textAlarmTime != null) {
                    textAlarmTime.setText(String.format(Locale.getDefault(), "%d:%02d · 탭해서 변경", h12, alarmMinute[0]));
                }
            });
        }
        if (toggleMeridiem2 != null) {
            toggleMeridiem2.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                isAm2[0] = (checkedId == R.id.btnMeridiemAm2);
                int h12 = alarmHour2[0] % 12 == 0 ? 12 : alarmHour2[0] % 12;
                alarmHour2[0] = isAm2[0] ? (h12 % 12) : (h12 % 12 + 12);
                if (textAlarmTime2 != null) {
                    textAlarmTime2.setText(String.format(Locale.getDefault(), "%d:%02d · 탭해서 변경", h12, alarmMinute2[0]));
                }
            });
        }

        if (editStartDate != null) {
            editStartDate.setOnClickListener(v -> showDatePicker(editStartDate));
        }
        if (editEndDate != null) {
            editEndDate.setOnClickListener(v -> showDatePicker(editEndDate));
        }
        if (textAlarmTime != null) {
            // 24시간제로 열고, 결과를 AM/PM 토글에 동기화
            textAlarmTime.setOnClickListener(v -> new TimePickerDialog(requireContext(), (tp, h, m) -> {
                alarmHour[0]   = h;
                alarmMinute[0] = m;
                isAm[0] = h < 12;
                if (toggleMeridiem != null)
                    toggleMeridiem.check(isAm[0] ? R.id.btnMeridiemAm : R.id.btnMeridiemPm);
                int h12 = (h % 12 == 0) ? 12 : (h % 12);
                textAlarmTime.setText(String.format(Locale.getDefault(), "%d:%02d · 탭해서 변경", h12, m));
            }, alarmHour[0], alarmMinute[0], true).show());
        }
        if (textAlarmTime2 != null) {
            // 초기 표시를 alarmHour2(21시)에 맞게 설정
            int initH12 = (alarmHour2[0] % 12 == 0) ? 12 : (alarmHour2[0] % 12);
            textAlarmTime2.setText(String.format(Locale.getDefault(), "%d:%02d · 탭해서 변경", initH12, alarmMinute2[0]));
            textAlarmTime2.setOnClickListener(v -> new TimePickerDialog(requireContext(), (tp, h, m) -> {
                alarmHour2[0]   = h;
                alarmMinute2[0] = m;
                isAm2[0] = h < 12;
                if (toggleMeridiem2 != null)
                    toggleMeridiem2.check(isAm2[0] ? R.id.btnMeridiemAm2 : R.id.btnMeridiemPm2);
                int h12 = (h % 12 == 0) ? 12 : (h % 12);
                textAlarmTime2.setText(String.format(Locale.getDefault(), "%d:%02d · 탭해서 변경", h12, m));
            }, alarmHour2[0], alarmMinute2[0], true).show());
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnMedSave = dialogView.findViewById(R.id.btnMedSave);
        btnMedSave.setOnClickListener(v -> {
            btnMedSave.setEnabled(false);
            String name = editName.getText() != null ? editName.getText().toString().trim() : "";
            if (name.isEmpty()) {
                btnMedSave.setEnabled(true);
                Toast.makeText(requireContext(), "약 이름을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            String dosage = editDosage.getText() != null ? editDosage.getText().toString().trim() : "";
            String startDate = editStartDate.getText() != null ? editStartDate.getText().toString().trim() : today;
            String endDate = editEndDate != null && editEndDate.getText() != null
                    ? editEndDate.getText().toString().trim() : null;
            if (endDate != null && endDate.isEmpty()) endDate = null;

            String freq = "DAILY";
            if (toggleFreq != null) {
                int chk = toggleFreq.getCheckedButtonId();
                if (chk == R.id.btnFreqTwice) freq = "TWICE_DAILY";
                else if (chk == R.id.btnFreqAsNeeded) freq = "AS_NEEDED";
            }

            boolean alarmEnabled = switchAlarm != null && switchAlarm.isChecked();
            String notes = editNotes.getText() != null ? editNotes.getText().toString().trim() : "";
            boolean isTwice = "TWICE_DAILY".equals(freq);

            MedicationCreateRequest req = new MedicationCreateRequest(
                    name, dosage, freq, startDate, endDate,
                    alarmEnabled, alarmHour[0], alarmMinute[0],
                    isTwice ? alarmHour2[0] : null, isTwice ? alarmMinute2[0] : null,
                    notes.isEmpty() ? null : notes);

            ApiService api = RetrofitClient.getApiService(requireContext());
            api.addMedication(catId, req).enqueue(new retrofit2.Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<ApiResponse> call,
                                       @NonNull retrofit2.Response<ApiResponse> resp) {
                    if (resp.isSuccessful()) {
                        Toast.makeText(requireContext(), "투약 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                        MedicationAlarmScheduler.syncAlarms(requireContext(), catId);
                        dialog.dismiss();
                    } else {
                        btnMedSave.setEnabled(true);
                        Toast.makeText(requireContext(), "저장 실패 (HTTP " + resp.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(@NonNull retrofit2.Call<ApiResponse> call, @NonNull Throwable t) {
                    btnMedSave.setEnabled(true);
                    Toast.makeText(requireContext(), "네트워크 오류: " + t.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialogView.findViewById(R.id.btnMedCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // =========================================================
    // Litter Box
    // =========================================================

    private void showLitterBoxDialog() {
        if (getContext() == null) return;
        Long catId = getCurrentCatId();
        if (catId == null || catId <= 0) {
            Toast.makeText(requireContext(), "고양이를 먼저 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_litter_box, null);

        TextView textCount = dialogView.findViewById(R.id.textLitterCount);
        View btnMinus = dialogView.findViewById(R.id.btnLitterCountMinus);
        View btnPlus = dialogView.findViewById(R.id.btnLitterCountPlus);
        MaterialButtonToggleGroup toggleColor = dialogView.findViewById(R.id.toggleLitterColor);
        MaterialButtonToggleGroup toggleShape = dialogView.findViewById(R.id.toggleLitterShape);
        MaterialSwitch switchAbnormal = dialogView.findViewById(R.id.switchLitterAbnormal);
        EditText editNotes = dialogView.findViewById(R.id.editLitterNotes);

        final int[] count = {1};
        if (toggleColor != null) toggleColor.check(R.id.btnColorNormal);
        if (toggleShape != null) toggleShape.check(R.id.btnShapeNormal);

        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> {
                if (count[0] > 1) {
                    count[0]--;
                    if (textCount != null) textCount.setText(String.valueOf(count[0]));
                }
            });
        }
        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                count[0]++;
                if (textCount != null) textCount.setText(String.valueOf(count[0]));
            });
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnLitterSave = dialogView.findViewById(R.id.btnLitterSave);
        btnLitterSave.setOnClickListener(v -> {
            btnLitterSave.setEnabled(false);
            String color = "NORMAL";
            if (toggleColor != null) {
                int chk = toggleColor.getCheckedButtonId();
                if (chk == R.id.btnColorYellow) color = "YELLOW";
                else if (chk == R.id.btnColorRed) color = "RED";
                else if (chk == R.id.btnColorOther) color = "OTHER";
            }
            String shape = "NORMAL";
            if (toggleShape != null) {
                int chk = toggleShape.getCheckedButtonId();
                if (chk == R.id.btnShapeSoft) shape = "SOFT";
                else if (chk == R.id.btnShapeLiquid) shape = "LIQUID";
                else if (chk == R.id.btnShapeNone) shape = "NONE";
            }
            boolean abnormal = switchAbnormal != null && switchAbnormal.isChecked();
            String notes = editNotes.getText() != null ? editNotes.getText().toString().trim() : null;
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

            LitterBoxCreateRequest req = new LitterBoxCreateRequest(
                    today, count[0], color, shape, abnormal, notes != null && notes.isEmpty() ? null : notes);

            ApiService api = RetrofitClient.getApiService(requireContext());
            api.addLitterRecord(catId, req).enqueue(new retrofit2.Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<ApiResponse> call,
                                       @NonNull retrofit2.Response<ApiResponse> resp) {
                    if (resp.isSuccessful()) {
                        String msg = abnormal ? "화장실 기록 저장 완료. 이상 증상이 있어요!" : "화장실 기록이 저장되었습니다.";
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        btnLitterSave.setEnabled(true);
                        Toast.makeText(requireContext(), "저장 실패 (HTTP " + resp.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(@NonNull retrofit2.Call<ApiResponse> call, @NonNull Throwable t) {
                    btnLitterSave.setEnabled(true);
                    Toast.makeText(requireContext(), "네트워크 오류: " + t.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialogView.findViewById(R.id.btnLitterCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // =========================================================
    // Vet Visit
    // =========================================================

    private void showVetVisitDialog() {
        if (getContext() == null) return;
        Long catId = getCurrentCatId();
        if (catId == null || catId <= 0) {
            Toast.makeText(requireContext(), "고양이를 먼저 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_vet_visit, null);

        EditText editDate = dialogView.findViewById(R.id.editVetDate);
        EditText editHospital = dialogView.findViewById(R.id.editVetHospital);
        EditText editDiagnosis = dialogView.findViewById(R.id.editVetDiagnosis);
        EditText editPrescription = dialogView.findViewById(R.id.editVetPrescription);
        EditText editNotes = dialogView.findViewById(R.id.editVetNotes);

        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if (editDate != null) {
            editDate.setText(today);
            editDate.setOnClickListener(v -> showDatePicker(editDate));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        View btnVetSave = dialogView.findViewById(R.id.btnVetSave);
        btnVetSave.setOnClickListener(v -> {
            btnVetSave.setEnabled(false);
            String date = editDate.getText() != null ? editDate.getText().toString().trim() : today;
            if (date.isEmpty()) date = today;
            String hospital = editHospital.getText() != null ? editHospital.getText().toString().trim() : "";
            String diagnosis = editDiagnosis.getText() != null ? editDiagnosis.getText().toString().trim() : "";
            String prescription = editPrescription.getText() != null ? editPrescription.getText().toString().trim() : "";
            String notes = editNotes.getText() != null ? editNotes.getText().toString().trim() : "";

            VetVisitCreateRequest req = new VetVisitCreateRequest(
                    date,
                    hospital.isEmpty() ? null : hospital,
                    diagnosis.isEmpty() ? null : diagnosis,
                    prescription.isEmpty() ? null : prescription,
                    notes.isEmpty() ? null : notes);

            ApiService api = RetrofitClient.getApiService(requireContext());
            api.addVetVisit(catId, req).enqueue(new retrofit2.Callback<ApiResponse>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<ApiResponse> call,
                                       @NonNull retrofit2.Response<ApiResponse> resp) {
                    if (resp.isSuccessful()) {
                        Toast.makeText(requireContext(), "진료 기록이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        btnVetSave.setEnabled(true);
                        Toast.makeText(requireContext(), "저장 실패 (HTTP " + resp.code() + ")", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(@NonNull retrofit2.Call<ApiResponse> call, @NonNull Throwable t) {
                    btnVetSave.setEnabled(true);
                    Toast.makeText(requireContext(), "네트워크 오류: " + t.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialogView.findViewById(R.id.btnVetCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // =========================================================
    // Water/Food & Obesity (existing logic preserved)
    // =========================================================

    private void loadWeightAndObesityHistory() {
        if (getContext() == null) return;
        Long catId = getCurrentCatId();
        if (catId == null || catId <= 0) {
            applyWeightChart(null);
            applyObesityChart(null);
            applyVomitTrend(null);
            return;
        }
        ApiService apiService = RetrofitClient.getApiService(requireContext());

        apiService.getWeightHistory(catId, 7).enqueue(new retrofit2.Callback<List<WeightHistoryItem>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<List<WeightHistoryItem>> call,
                                   @NonNull retrofit2.Response<List<WeightHistoryItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    applyWeightChart(null);
                    return;
                }
                applyWeightChart(response.body());
                if (!response.body().isEmpty()) {
                    Double w = response.body().get(0).getWeightKg();
                    if (w != null) updateWeightGoalDisplay(w.floatValue());
                }
            }
            @Override
            public void onFailure(@NonNull retrofit2.Call<List<WeightHistoryItem>> call, @NonNull Throwable t) {
                applyWeightChart(null);
            }
        });

        apiService.getObesityHistory(catId, 7).enqueue(new retrofit2.Callback<List<ObesityHistoryItem>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<List<ObesityHistoryItem>> call,
                                   @NonNull retrofit2.Response<List<ObesityHistoryItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    applyObesityChart(null);
                    return;
                }
                applyObesityChart(response.body());
            }
            @Override
            public void onFailure(@NonNull retrofit2.Call<List<ObesityHistoryItem>> call, @NonNull Throwable t) {
                applyObesityChart(null);
            }
        });

        Calendar to = Calendar.getInstance();
        Calendar from = (Calendar) to.clone();
        from.add(Calendar.DAY_OF_MONTH, -29);
        String fromStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(from.getTime());
        String toStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(to.getTime());
        apiService.getCalendarEvents(catId, fromStr, toStr).enqueue(new retrofit2.Callback<List<CalendarEventItem>>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<List<CalendarEventItem>> call,
                                   @NonNull retrofit2.Response<List<CalendarEventItem>> response) {
                applyVomitTrend(response.isSuccessful() ? response.body() : null);
            }
            @Override
            public void onFailure(@NonNull retrofit2.Call<List<CalendarEventItem>> call, @NonNull Throwable t) {
                applyVomitTrend(null);
            }
        });
    }

    private void applyWeightChart(List<WeightHistoryItem> items) {
        if (chartWeightTrend == null) return;
        chartWeightTrend.setUnit("kg");
        if (items == null || items.isEmpty()) {
            chartWeightTrend.setSeries(new float[0], new String[0]);
            return;
        }
        ArrayList<WeightHistoryItem> sorted = new ArrayList<>(items);
        Collections.sort(sorted, Comparator.comparing(WeightHistoryItem::getDate, Comparator.nullsLast(String::compareTo)));
        Map<String, Float> byDate = new LinkedHashMap<>();
        for (WeightHistoryItem it : sorted) {
            if (it == null || it.getDate() == null || it.getDate().length() < 10) continue;
            String key = it.getDate().substring(0, 10);
            Double w = it.getWeightKg();
            byDate.put(key, w != null ? w.floatValue() : 0f);
        }
        float[] arr = new float[byDate.size()];
        String[] labels = new String[byDate.size()];
        int i = 0;
        for (Map.Entry<String, Float> e : byDate.entrySet()) {
            arr[i] = e.getValue();
            labels[i] = formatChartDateLabel(e.getKey());
            i++;
        }
        chartWeightTrend.setSeries(arr, labels);
    }

    private void applyObesityChart(List<ObesityHistoryItem> items) {
        if (chartObesityTrend == null) return;
        chartObesityTrend.setUnit("%");
        if (items == null || items.isEmpty()) {
            chartObesityTrend.setSeries(new float[0], new String[0]);
            return;
        }
        ArrayList<ObesityHistoryItem> sorted = new ArrayList<>(items);
        Collections.sort(sorted, Comparator.comparing(ObesityHistoryItem::getDate, Comparator.nullsLast(String::compareTo)));
        Map<String, Float> byDate = new LinkedHashMap<>();
        for (ObesityHistoryItem it : sorted) {
            if (it == null || it.getDate() == null || it.getDate().length() < 10) continue;
            String key = it.getDate().substring(0, 10);
            Double bf = it.getBodyFatPercent();
            byDate.put(key, bf != null ? bf.floatValue() : 0f);
        }
        float[] arr = new float[byDate.size()];
        String[] labels = new String[byDate.size()];
        int i = 0;
        for (Map.Entry<String, Float> e : byDate.entrySet()) {
            arr[i] = e.getValue();
            labels[i] = formatChartDateLabel(e.getKey());
            i++;
        }
        chartObesityTrend.setSeries(arr, labels);
    }

    private static String formatChartDateLabel(String yyyyMmDd) {
        if (yyyyMmDd == null || yyyyMmDd.length() < 10) return "";
        return yyyyMmDd.substring(5, 7) + "/" + yyyyMmDd.substring(8, 10);
    }

    private void applyVomitTrend(List<CalendarEventItem> items) {
        if (chartVomitTrend == null || textVomitTrendSummary == null) return;
        chartVomitTrend.setUnit("회");
        float[] counts = new float[7];
        String[] labels = new String[7];
        Calendar base = Calendar.getInstance();
        base.add(Calendar.DAY_OF_MONTH, -6);
        SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (int i = 0; i < 7; i++) {
            Calendar d = (Calendar) base.clone();
            d.add(Calendar.DAY_OF_MONTH, i);
            labels[i] = new SimpleDateFormat("MM/dd", Locale.getDefault()).format(d.getTime());
            String key = ymd.format(d.getTime());
            int dayCount = 0;
            if (items != null) {
                for (CalendarEventItem item : items) {
                    if (item == null || !"VOMIT".equals(item.getType())) continue;
                    String ds = item.getDate();
                    if (ds != null && ds.length() >= 10 && key.equals(ds.substring(0, 10))) dayCount++;
                }
            }
            counts[i] = dayCount;
        }
        chartVomitTrend.setSeries(counts, labels);

        int total7 = 0, highRisk7 = 0, total30 = 0;
        if (items != null) {
            for (CalendarEventItem item : items) {
                if (item == null || !"VOMIT".equals(item.getType())) continue;
                total30++;
                String ds = item.getDate();
                boolean isLast7 = false;
                if (ds != null && ds.length() >= 10) {
                    String d = ds.substring(0, 10);
                    for (int i = 0; i < 7; i++) {
                        Calendar c = (Calendar) base.clone();
                        c.add(Calendar.DAY_OF_MONTH, i);
                        if (ymd.format(c.getTime()).equals(d)) { isLast7 = true; break; }
                    }
                }
                if (isLast7) {
                    total7++;
                    String risk = item.getRiskLevel() != null ? item.getRiskLevel().toUpperCase(Locale.getDefault()) : "";
                    if (risk.contains("HIGH") || risk.contains("위험")) highRisk7++;
                }
            }
        }
        textVomitTrendSummary.setText("최근 7일 " + total7 + "회 · 고위험 " + highRisk7 + "회 | 최근 30일 " + total30 + "회");
    }

    private void showWaterFoodCalcDialog() {
        if (getContext() == null) return;
        Long catId = getCurrentCatId();
        if (catId == null || catId <= 0) {
            Toast.makeText(requireContext(), "고양이를 먼저 선택해 주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_water_food_calc, null, false);

        EditText editWeightKg = dialogView.findViewById(R.id.editWeightKg);
        EditText editAgeYears = dialogView.findViewById(R.id.editCatAgeYears);
        TextView textAgeDisplay = dialogView.findViewById(R.id.textCatAgeDisplay);
        EditText editFeedKcalPerG = dialogView.findViewById(R.id.editFeedKcalPerG);
        TextView textResult = dialogView.findViewById(R.id.textWaterFoodResult);
        Button btnCalculate = dialogView.findViewById(R.id.btnWaterFoodCalculate);

        // DB에서 나이 자동 조회
        ApiService apiForAge = RetrofitClient.getApiService(requireContext());
        apiForAge.getCat(catId).enqueue(new retrofit2.Callback<com.example.howscat.dto.CatResponse>() {
            @Override
            public void onResponse(@NonNull retrofit2.Call<com.example.howscat.dto.CatResponse> call,
                                   @NonNull retrofit2.Response<com.example.howscat.dto.CatResponse> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().getAge() != null) {
                    int age = response.body().getAge();
                    editAgeYears.setText(String.valueOf(age));
                    if (textAgeDisplay != null) {
                        textAgeDisplay.setText("나이: " + age + "살 (자동 입력)");
                    }
                } else {
                    if (textAgeDisplay != null) textAgeDisplay.setText("나이를 불러오지 못했어요.");
                }
            }
            @Override
            public void onFailure(@NonNull retrofit2.Call<com.example.howscat.dto.CatResponse> call,
                                  @NonNull Throwable t) {
                if (textAgeDisplay != null) textAgeDisplay.setText("나이를 불러오지 못했어요.");
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        View btnBack = dialogView.findViewById(R.id.btnWaterFoodBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());

        btnCalculate.setOnClickListener(v -> {
            Double weightKg = parseDouble(editWeightKg.getText() != null ? editWeightKg.getText().toString() : null);
            Double ageYears = parseDouble(editAgeYears.getText() != null ? editAgeYears.getText().toString() : null);
            Double feedKcalPerG = parseDouble(editFeedKcalPerG.getText() != null ? editFeedKcalPerG.getText().toString() : null);
            if (weightKg == null || feedKcalPerG == null || feedKcalPerG <= 0) {
                textResult.setText("몸무게/사료 kcal/g를 정확히 입력하세요.");
                return;
            }
            if (ageYears == null || ageYears < 0) {
                textResult.setText("나이를 불러오는 중이에요. 잠시 후 다시 시도해 주세요.");
                return;
            }

            double rer = (weightKg * 30.0) + 70.0;
            double ageMonths = ageYears * 12.0;
            double ageFactor;
            if (ageMonths < 4.0) ageFactor = 3.0;
            else if (ageMonths < 7.0) ageFactor = 2.5;
            else if (ageMonths <= 12.0) ageFactor = 2.0;
            else if (ageYears >= 10.0) ageFactor = 1.0;
            else ageFactor = 1.2;
            double der = rer * ageFactor;
            double recommendedFoodG = der / feedKcalPerG;
            double recommendedWaterMl = weightKg * 50.0;
            textResult.setText(String.format(Locale.getDefault(),
                    "물: %.0f mL\n사료: 약 %.0f g/일", recommendedWaterMl, recommendedFoodG));

            String today2 = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            CareWeightRequest req = new CareWeightRequest(weightKg, recommendedWaterMl, recommendedFoodG, today2);
            ApiService api = RetrofitClient.getApiService(requireContext());
            api.recordCareWeight(catId, req).enqueue(new retrofit2.Callback<Void>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<Void> call, @NonNull retrofit2.Response<Void> response) {
                    if (response.isSuccessful()) {
                        CareResultPrefs.saveWaterFood(requireContext(), catId, recommendedWaterMl, recommendedFoodG, weightKg);
                        loadWeightAndObesityHistory();
                    }
                }
                @Override
                public void onFailure(@NonNull retrofit2.Call<Void> call, @NonNull Throwable t) {}
            });
        });
        dialog.show();
    }

    private void showObesityCheckDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_obesity_check, null, false);

        EditText editWaistCm = dialogView.findViewById(R.id.editObesityBodyFatPercent);
        EditText editHindLegCm = dialogView.findViewById(R.id.editObesityAbdomenCm);
        EditText editWeightKg = dialogView.findViewById(R.id.editObesityWeightKg);
        EditText editFeedKcalPerG = dialogView.findViewById(R.id.editObesityFeedKcalPerG);
        TextView textResult = dialogView.findViewById(R.id.textObesityResult);
        Button btnCalculate = dialogView.findViewById(R.id.btnObesityCalculate);
        LinearLayout layoutWeightRow = dialogView.findViewById(R.id.layoutWeightObesityRow);
        TextView textHint = dialogView.findViewById(R.id.textObesityHint);

        Long catIdBox = getCurrentCatId();
        long catId = catIdBox != null ? catIdBox : -1L;
        boolean useSavedWeight = catId > 0 && CareResultPrefs.hasWeightFromWaterFood(requireContext(), catId);
        if (layoutWeightRow != null) layoutWeightRow.setVisibility(useSavedWeight ? View.GONE : View.VISIBLE);
        if (textHint != null) {
            textHint.setText(useSavedWeight
                    ? "허리 둘레/뒷다리 길이와 사료 kcal을 입력하세요. 몸무게는 물·사료 계산값을 사용합니다."
                    : "허리 둘레/뒷다리 길이와 몸무게(kg)를 입력하세요");
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        View btnBack = dialogView.findViewById(R.id.btnObesityBack);
        if (btnBack != null) btnBack.setOnClickListener(v -> dialog.dismiss());

        btnCalculate.setOnClickListener(v -> {
            Double waistCm = parseDouble(editWaistCm.getText() != null ? editWaistCm.getText().toString() : null);
            Double hindLegCm = parseDouble(editHindLegCm.getText() != null ? editHindLegCm.getText().toString() : null);
            Double feedKcalPerG = parseDouble(editFeedKcalPerG.getText() != null ? editFeedKcalPerG.getText().toString() : null);
            Double bodyFatPercent = estimateBodyFatPercent(waistCm, hindLegCm);
            Double weightKgBox = parseDouble(editWeightKg.getText() != null ? editWeightKg.getText().toString() : null);
            if (useSavedWeight) {
                float w = CareResultPrefs.getLastWeightKgForCat(requireContext(), catId);
                weightKgBox = (double) w;
            }
            if (bodyFatPercent == null || weightKgBox == null || weightKgBox <= 0 || feedKcalPerG == null || feedKcalPerG <= 0) {
                textResult.setText("둘레/몸무게/사료 kcal/g를 정확히 입력하세요.");
                return;
            }
            if (catId <= 0) { textResult.setText("고양이를 먼저 선택해 주세요."); return; }
            final double weightKg = weightKgBox;
            ApiService apiService = RetrofitClient.getApiService(requireContext());
            textResult.setText("비만도 계산 중...");
            ObesityCheckRequest req = new ObesityCheckRequest(bodyFatPercent, weightKg, feedKcalPerG);
            apiService.checkObesity(catId, req).enqueue(new retrofit2.Callback<ObesityCheckResponse>() {
                @Override
                public void onResponse(@NonNull retrofit2.Call<ObesityCheckResponse> call,
                                       @NonNull retrofit2.Response<ObesityCheckResponse> response) {
                    if (!response.isSuccessful() || response.body() == null) {
                        showObesityFallback(textResult, bodyFatPercent, weightKg, feedKcalPerG, catId);
                        return;
                    }
                    ObesityCheckResponse res = response.body();
                    textResult.setText(String.format(Locale.getDefault(),
                            "체지방률 추정: %.1f%%\n레벨: %s\n목표 몸무게: %.2f kg\n물: %.0f mL\n사료: 약 %.0f g/일",
                            bodyFatPercent, CareResultPrefs.obesityLevelLabel(res.getObesityLevel()),
                            res.getRecommendedTargetWeight(), res.getRecommendedWater(), res.getRecommendedFood()));
                    CareResultPrefs.saveObesity(requireContext(), catId,
                            res.getRecommendedWater() != null ? res.getRecommendedWater() : 0,
                            res.getRecommendedFood() != null ? res.getRecommendedFood() : 0,
                            res.getObesityLevel() != null ? res.getObesityLevel() : "");
                    loadWeightAndObesityHistory();
                }
                @Override
                public void onFailure(@NonNull retrofit2.Call<ObesityCheckResponse> call, @NonNull Throwable t) {
                    showObesityFallback(textResult, bodyFatPercent, weightKg, feedKcalPerG, catId);
                }
            });
        });
        dialog.show();
    }

    private Double estimateBodyFatPercent(Double waistCm, Double hindLegCm) {
        if (waistCm == null || hindLegCm == null || waistCm <= 0 || hindLegCm <= 0) return null;
        double pct = ((waistCm + hindLegCm) * 1.5) - 9.0;
        if (pct < 5.0) pct = 5.0;
        if (pct > 60.0) pct = 60.0;
        return pct;
    }

    private Long getCurrentCatId() {
        if (getContext() == null) return null;
        return getContext().getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getLong("lastViewedCatId", -1L);
    }

    private void showObesityFallback(TextView textResult, double bodyFatPercent, double weightKg,
                                      double feedKcalPerG, long catId) {
        String level;
        if (bodyFatPercent < 16) level = "UNDERWEIGHT";
        else if (bodyFatPercent <= 25) level = "NORMAL";
        else if (bodyFatPercent <= 35) level = "SLIGHTLY_OVERWEIGHT";
        else level = "OVERWEIGHT";
        double rer = (weightKg * 30.0) + 70.0;
        double recommendedFoodKcal;
        if ("UNDERWEIGHT".equals(level)) recommendedFoodKcal = rer * 1.4;
        else if ("NORMAL".equals(level)) recommendedFoodKcal = rer * 1.3;
        else if ("SLIGHTLY_OVERWEIGHT".equals(level)) recommendedFoodKcal = rer * 1.0;
        else recommendedFoodKcal = rer * 0.9;
        double recommendedFoodG = recommendedFoodKcal / feedKcalPerG;
        double recommendedWaterMl = weightKg * 50.0;
        double lean = weightKg * (1.0 - bodyFatPercent / 100.0);
        double recommendedTargetWeight = lean / 0.8;
        textResult.setText(String.format(Locale.getDefault(),
                "레벨: %s\n목표 몸무게: %.2f kg\n물: %.0f mL\n사료: 약 %.0f g/일",
                CareResultPrefs.obesityLevelLabel(level), recommendedTargetWeight, recommendedWaterMl, recommendedFoodG));
        CareResultPrefs.saveObesity(requireContext(), catId, recommendedWaterMl, recommendedFoodG, level);
    }

    private Double parseDouble(String s) {
        if (s == null) return null;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return null; }
    }

    private void showDatePicker(EditText target) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
            target.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void animateCatEntry(View root) {
        if (!(root instanceof ViewGroup)) return;
        ViewGroup vg = (ViewGroup) root;
        if (vg.getChildCount() <= 0 || !(vg.getChildAt(0) instanceof ViewGroup)) return;
        ViewGroup content = (ViewGroup) vg.getChildAt(0);
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            child.setAlpha(0f);
            child.setTranslationY(18f);
            child.animate().alpha(1f).translationY(0f)
                    .setStartDelay(i * 30L).setDuration(220).start();
        }
    }
}
