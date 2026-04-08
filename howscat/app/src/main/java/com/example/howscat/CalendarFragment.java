package com.example.howscat;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.example.howscat.dto.HealthScheduleItem;
import com.example.howscat.dto.LitterBoxCreateRequest;
import com.example.howscat.dto.LitterBoxItem;
import com.example.howscat.dto.MedicationCreateRequest;
import com.example.howscat.dto.MedicationItem;
import com.example.howscat.dto.VetVisitCreateRequest;
import com.example.howscat.dto.VetVisitItem;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.example.howscat.dto.CalendarEventItem;
import com.example.howscat.dto.CalendarMemoUpdateRequest;
import com.example.howscat.dto.CalendarMemoCreateRequest;
import com.example.howscat.dto.HealthScheduleUpdateRequest;
import com.example.howscat.HealthScheduleAlarmScheduler;
import com.example.howscat.network.ApiService;
import com.example.howscat.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CalendarFragment extends Fragment {

    private ApiService api;
    private long catId;
    private String currentDateStr;
    private String selectedDateStr;
    private final List<CalendarEventItem> currentItems = new ArrayList<>();
    private final List<CalendarEventItem> filteredItems = new ArrayList<>();
    private LinearLayout listCalendarEventsContainer;
    private TextView textCalendarEmpty;
    private LinearLayout layoutCalendarGrid;
    private TextView textMonthYear;
    private Calendar displayMonth;
    private final Set<String> memoDates = new HashSet<>();
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat monthTitleFmt = new SimpleDateFormat("yyyy년 M월", Locale.KOREA);
    private boolean filterWeight = true;
    private boolean filterVomit = true;
    private boolean filterCheckup = true;
    private boolean filterVaccine = true;
    private boolean filterMemo = true;

    public CalendarFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        listCalendarEventsContainer = view.findViewById(R.id.listCalendarEventsContainer);
        TextView textCalendarEmpty = view.findViewById(R.id.textCalendarEmpty);
        View layoutNextCheckup = view.findViewById(R.id.layoutNextCheckup);
        View layoutNextVaccine = view.findViewById(R.id.layoutNextVaccine);
        TextView textNextCheckup = view.findViewById(R.id.textNextCheckup);
        TextView textNextVaccine = view.findViewById(R.id.textNextVaccine);
        Button btnAddCalendarMemo = view.findViewById(R.id.btnAddCalendarMemo);
        Chip btnFilterWeight = view.findViewById(R.id.btnFilterWeight);
        Chip btnFilterVomit = view.findViewById(R.id.btnFilterVomit);
        Chip btnFilterCheckup = view.findViewById(R.id.btnFilterCheckup);
        Chip btnFilterVaccine = view.findViewById(R.id.btnFilterVaccine);
        Chip btnFilterMemo = view.findViewById(R.id.btnFilterMemo);
        layoutCalendarGrid = view.findViewById(R.id.layoutCalendarGrid);
        textMonthYear = view.findViewById(R.id.textMonthYear);
        View cardMonthCalendar = view.findViewById(R.id.cardMonthCalendar);
        Button btnMonthPrev = view.findViewById(R.id.btnMonthPrev);
        Button btnMonthNext = view.findViewById(R.id.btnMonthNext);

        this.api = RetrofitClient.getApiService(requireContext());
        this.catId = getCurrentCatId();
        this.textCalendarEmpty = textCalendarEmpty;
        this.displayMonth = Calendar.getInstance();

        if (this.catId <= 0) {
            textCalendarEmpty.setText("고양이를 먼저 선택해야 캘린더가 표시됩니다.");
            if (listCalendarEventsContainer != null) {
                listCalendarEventsContainer.removeAllViews();
            }
            btnAddCalendarMemo.setEnabled(false);
            if (cardMonthCalendar != null) {
                cardMonthCalendar.setVisibility(View.GONE);
            }
            return view;
        }

        if (cardMonthCalendar != null) {
            cardMonthCalendar.setVisibility(View.VISIBLE);
        }

        btnAddCalendarMemo.setEnabled(true);
        btnAddCalendarMemo.setOnClickListener(v -> showAddMemoDialog());
        bindFilterUi(btnFilterWeight, btnFilterVomit, btnFilterCheckup, btnFilterVaccine, btnFilterMemo);

        HealthScheduleAlarmScheduler.syncAlarms(requireContext(), catId);

        loadNextHealthSchedules(api, catId, layoutNextCheckup, textNextCheckup, layoutNextVaccine, textNextVaccine);

        String todayStr = dateFmt.format(new Date());
        currentDateStr = todayStr;
        selectedDateStr = todayStr;
        if (textMonthYear != null) {
            textMonthYear.setText(monthTitleFmt.format(displayMonth.getTime()));
        }
        buildCalendarGrid();
        loadMonthMemoMarkers();
        loadEventsForDate(this.api, this.catId, todayStr, textCalendarEmpty, true);

        if (btnMonthPrev != null) {
            btnMonthPrev.setOnClickListener(v -> shiftMonth(-1));
        }
        if (btnMonthNext != null) {
            btnMonthNext.setOnClickListener(v -> shiftMonth(1));
        }

        return view;
    }

    private void bindFilterUi(
            Chip btnWeight,
            Chip btnVomit,
            Chip btnCheckup,
            Chip btnVaccine,
            Chip btnMemo
    ) {
        if (btnWeight != null) {
            btnWeight.setChecked(filterWeight);
            btnWeight.setOnCheckedChangeListener((v, isChecked) -> {
                filterWeight = isChecked;
                rebuildCalendarEventRows();
            });
        }
        if (btnVomit != null) {
            btnVomit.setChecked(filterVomit);
            btnVomit.setOnCheckedChangeListener((v, isChecked) -> {
                filterVomit = isChecked;
                rebuildCalendarEventRows();
            });
        }
        if (btnCheckup != null) {
            btnCheckup.setChecked(filterCheckup);
            btnCheckup.setOnCheckedChangeListener((v, isChecked) -> {
                filterCheckup = isChecked;
                rebuildCalendarEventRows();
            });
        }
        if (btnVaccine != null) {
            btnVaccine.setChecked(filterVaccine);
            btnVaccine.setOnCheckedChangeListener((v, isChecked) -> {
                filterVaccine = isChecked;
                rebuildCalendarEventRows();
            });
        }
        if (btnMemo != null) {
            btnMemo.setChecked(filterMemo);
            btnMemo.setOnCheckedChangeListener((v, isChecked) -> {
                filterMemo = isChecked;
                rebuildCalendarEventRows();
            });
        }
    }

    private void shiftMonth(int delta) {
        if (layoutCalendarGrid == null) return;
        displayMonth.add(Calendar.MONTH, delta);
        int y = displayMonth.get(Calendar.YEAR);
        int m = displayMonth.get(Calendar.MONTH);
        Calendar parsed = Calendar.getInstance();
        try {
            Date d = dateFmt.parse(selectedDateStr);
            if (d != null) {
                parsed.setTime(d);
            }
        } catch (Exception ignored) {
        }
        int desiredDay = 1;
        if (parsed.get(Calendar.YEAR) == y && parsed.get(Calendar.MONTH) == m) {
            desiredDay = parsed.get(Calendar.DAY_OF_MONTH);
        } else {
            Calendar today = Calendar.getInstance();
            if (today.get(Calendar.YEAR) == y && today.get(Calendar.MONTH) == m) {
                desiredDay = today.get(Calendar.DAY_OF_MONTH);
            }
        }
        int max = displayMonth.getActualMaximum(Calendar.DAY_OF_MONTH);
        displayMonth.set(Calendar.DAY_OF_MONTH, Math.min(desiredDay, max));
        selectedDateStr = dateFmt.format(displayMonth.getTime());
        currentDateStr = selectedDateStr;
        if (textMonthYear != null) {
            textMonthYear.setText(monthTitleFmt.format(displayMonth.getTime()));
        }
        buildCalendarGrid();
        loadMonthMemoMarkers();
        loadEventsForDate(api, catId, selectedDateStr, textCalendarEmpty, false);
    }

    private void buildCalendarGrid() {
        if (layoutCalendarGrid == null || getContext() == null) return;
        layoutCalendarGrid.removeAllViews();
        int cellH = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 44, getResources().getDisplayMetrics());
        int dotS = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
        int dotBottom = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());

        LinearLayout weekRow = new LinearLayout(requireContext());
        weekRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] wds = {"일", "월", "화", "수", "목", "금", "토"};
        for (String w : wds) {
            TextView tv = new TextView(requireContext());
            tv.setText(w);
            tv.setGravity(Gravity.CENTER);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tv.setTextColor(getResources().getColor(R.color.app_on_surface_variant));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            weekRow.addView(tv, lp);
        }
        layoutCalendarGrid.addView(weekRow);

        Calendar cal = (Calendar) displayMonth.clone();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int skip = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7;
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        Calendar today = Calendar.getInstance();
        int totalCells = skip + daysInMonth;
        int rows = (totalCells + 6) / 7;

        int dayNum = 1;
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 7; c++) {
                FrameLayout cell = new FrameLayout(requireContext());
                LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(0, cellH, 1f);
                row.addView(cell, cellLp);

                if (skip > 0) {
                    skip--;
                    continue;
                }
                if (dayNum > daysInMonth) {
                    continue;
                }

                String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                        displayMonth.get(Calendar.YEAR),
                        displayMonth.get(Calendar.MONTH) + 1,
                        dayNum);

                TextView tv = new TextView(requireContext());
                tv.setText(String.valueOf(dayNum));
                tv.setGravity(Gravity.CENTER);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);

                boolean isToday = today.get(Calendar.YEAR) == displayMonth.get(Calendar.YEAR)
                        && today.get(Calendar.MONTH) == displayMonth.get(Calendar.MONTH)
                        && today.get(Calendar.DAY_OF_MONTH) == dayNum;
                boolean isSelected = dateStr.equals(selectedDateStr);

                if (isToday) {
                    tv.setTextColor(getResources().getColor(R.color.app_primary_dark));
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    tv.setTextColor(getResources().getColor(R.color.app_on_surface));
                }

                if (isSelected) {
                    cell.setBackgroundResource(R.drawable.bg_calendar_cell_selected);
                } else {
                    cell.setBackground(null);
                }

                FrameLayout.LayoutParams tvLp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                cell.addView(tv, tvLp);

                View dot = new View(requireContext());
                dot.setBackgroundResource(R.drawable.bg_dot_memo);
                FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dotS, dotS);
                dotLp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                dotLp.bottomMargin = dotBottom;
                boolean hasMemo = memoDates.contains(dateStr);
                dot.setVisibility(hasMemo ? View.VISIBLE : View.GONE);
                cell.addView(dot, dotLp);

                final String clickDate = dateStr;
                cell.setOnClickListener(v -> {
                    selectedDateStr = clickDate;
                    currentDateStr = clickDate;
                    Calendar pick = Calendar.getInstance();
                    try {
                        Date d = dateFmt.parse(clickDate);
                        if (d != null) {
                            pick.setTime(d);
                            displayMonth.set(Calendar.YEAR, pick.get(Calendar.YEAR));
                            displayMonth.set(Calendar.MONTH, pick.get(Calendar.MONTH));
                            displayMonth.set(Calendar.DAY_OF_MONTH, pick.get(Calendar.DAY_OF_MONTH));
                        }
                    } catch (Exception ignored) {
                    }
                    buildCalendarGrid();
                    loadEventsForDate(api, catId, clickDate, textCalendarEmpty, false);
                });

                dayNum++;
            }
            layoutCalendarGrid.addView(row);
        }
    }

    private void loadMonthMemoMarkers() {
        if (api == null || catId <= 0 || layoutCalendarGrid == null) return;
        Calendar c = (Calendar) displayMonth.clone();
        c.set(Calendar.DAY_OF_MONTH, 1);
        String from = dateFmt.format(c.getTime());
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        String to = dateFmt.format(c.getTime());

        api.getCalendarEvents(catId, from, to).enqueue(new Callback<List<CalendarEventItem>>() {
            @Override
            public void onResponse(Call<List<CalendarEventItem>> call, Response<List<CalendarEventItem>> response) {
                memoDates.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (CalendarEventItem item : response.body()) {
                        if (item == null || item.getDate() == null) continue;
                        String t = item.getType();
                        if ("MEMO".equals(t) || "VOMIT".equals(t) || "WEIGHT".equals(t)
                                || "HEALTH_CHECKUP".equals(t) || "HEALTH_VACCINE".equals(t)
                                || "MEDICATION".equals(t) || "LITTER".equals(t) || "VET_VISIT".equals(t)) {
                            String ds = item.getDate();
                            if (ds.length() >= 10) {
                                memoDates.add(ds.substring(0, 10));
                            } else {
                                memoDates.add(ds);
                            }
                        }
                    }
                }
                buildCalendarGrid();
            }

            @Override
            public void onFailure(Call<List<CalendarEventItem>> call, Throwable t) {
                buildCalendarGrid();
            }
        });
    }

    private void showAddMemoDialog() {
        if (api == null) return;
        if (catId <= 0) return;

        String memoDate = currentDateStr != null ? currentDateStr : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_calendar_add_memo, null, false);
        TextView tvDate = dialogView.findViewById(R.id.textMemoDate);
        MaterialButtonToggleGroup groupType = dialogView.findViewById(R.id.groupMemoType);
        MaterialButton rbNormal = dialogView.findViewById(R.id.rbMemoTypeNormal);
        MaterialButton rbCheckup = dialogView.findViewById(R.id.rbMemoTypeCheckup);
        MaterialButton rbVaccine = dialogView.findViewById(R.id.rbMemoTypeVaccine);
        EditText editMemo = dialogView.findViewById(R.id.editMemoContent);
        View layoutScheduleAlarm = dialogView.findViewById(R.id.layoutScheduleAlarm);
        com.google.android.material.materialswitch.MaterialSwitch switchScheduleAlarm =
                dialogView.findViewById(R.id.switchScheduleAlarm);
        tvDate.setText("날짜: " + memoDate);
        groupType.check(rbNormal.getId());

        // 건강검진/예방접종 선택 시 알림 설정 영역 표시
        groupType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            boolean isScheduleType = checkedId == rbCheckup.getId() || checkedId == rbVaccine.getId();
            if (layoutScheduleAlarm != null) {
                layoutScheduleAlarm.setVisibility(isScheduleType ? View.VISIBLE : View.GONE);
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("취소", (d, which) -> d.dismiss())
                .setPositiveButton("저장", (d, which) -> {
                    String content = editMemo.getText() != null ? editMemo.getText().toString().trim() : "";

                    int checked = groupType.getCheckedButtonId();
                    boolean isCheckup = checked == rbCheckup.getId();
                    boolean isVaccine = checked == rbVaccine.getId();
                    boolean isScheduleType = isCheckup || isVaccine;

                    // 건강검진/예방접종은 메모 없어도 저장 가능 (일반 메모는 내용 필수)
                    if (!isScheduleType && content.isEmpty()) {
                        Toast.makeText(requireContext(), "메모 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // healthTypeId: 1=건강검진, 2=예방접종 (서버 seed 기준)
                    Long healthTypeId = null;
                    if (isCheckup) healthTypeId = 1L;
                    else if (isVaccine) healthTypeId = 2L;

                    boolean alarmEnabled = switchScheduleAlarm != null && switchScheduleAlarm.isChecked();

                    // 건강검진/예방접종이면 health_schedule 등록 (다음 일정 자동 계산)
                    if (isScheduleType) {
                        final Long finalHealthTypeId = healthTypeId;
                        com.example.howscat.dto.HealthScheduleCreateRequest scheduleReq =
                                new com.example.howscat.dto.HealthScheduleCreateRequest(
                                        finalHealthTypeId, memoDate, alarmEnabled, null);
                        api.createHealthSchedule(catId, scheduleReq).enqueue(new Callback<com.example.howscat.dto.HealthScheduleItem>() {
                            @Override
                            public void onResponse(Call<com.example.howscat.dto.HealthScheduleItem> call,
                                                   Response<com.example.howscat.dto.HealthScheduleItem> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    String nextDate = response.body().getNextDate();
                                    String typeName = isCheckup ? "건강검진" : "예방접종";
                                    Toast.makeText(requireContext(),
                                            typeName + " 등록 완료. 다음 일정: " + nextDate,
                                            Toast.LENGTH_SHORT).show();
                                    HealthScheduleAlarmScheduler.syncAlarms(requireContext(), catId);
                                    // 캘린더에 완료 이벤트 표시 (last_date 기준으로 갱신)
                                    loadMonthMemoMarkers();
                                    loadEventsForDate(api, catId, memoDate, textCalendarEmpty, false);
                                    View root = getView();
                                    if (root != null) {
                                        loadNextHealthSchedules(api, catId,
                                                root.findViewById(R.id.layoutNextCheckup),
                                                root.findViewById(R.id.textNextCheckup),
                                                root.findViewById(R.id.layoutNextVaccine),
                                                root.findViewById(R.id.textNextVaccine));
                                    }
                                }
                            }
                            @Override
                            public void onFailure(Call<com.example.howscat.dto.HealthScheduleItem> call, Throwable t) {
                                Toast.makeText(requireContext(), "일정 등록 실패: " + (t.getMessage() != null ? t.getMessage() : "네트워크 오류"), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    // 건강검진/예방접종은 health_schedule로만 저장 (calendar_memo 중복 생성 방지)
                    // 일반 메모만 calendar_memo에 저장
                    if (!isScheduleType) {
                        String memoContent = content.isEmpty() ? "" : content;
                        CalendarMemoCreateRequest req = new CalendarMemoCreateRequest(memoContent, memoDate, null);
                        api.addCalendarMemo(catId, req).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(requireContext(), "메모 저장 완료", Toast.LENGTH_SHORT).show();
                                    loadMonthMemoMarkers();
                                    loadEventsForDate(api, catId, memoDate, textCalendarEmpty, false);
                                } else {
                                    Toast.makeText(requireContext(), "메모 저장 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(requireContext(), "메모 저장 실패: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .create();
        dialog.show();
        styleDialogWindow(dialog);
    }

    private Long findHealthTypeIdFromCurrentItems(boolean checkup) {
        for (CalendarEventItem item : currentItems) {
            if (item == null || item.getHealthTypeId() == null || item.getType() == null) continue;
            if (checkup && "HEALTH_CHECKUP".equals(item.getType())) return item.getHealthTypeId();
            if (!checkup && "HEALTH_VACCINE".equals(item.getType())) return item.getHealthTypeId();
        }
        return null;
    }

    private long getCurrentCatId() {
        return requireContext()
                .getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
                .getLong("lastViewedCatId", -1L);
    }

    private void loadNextHealthSchedules(
            ApiService api,
            long catId,
            View layoutNextCheckup,
            TextView textNextCheckup,
            View layoutNextVaccine,
            TextView textNextVaccine
    ) {
        if (api == null) return;

        textNextCheckup.setText("건강검진: 불러오는 중...");
        textNextVaccine.setText("예방접종: 불러오는 중...");

        api.getHealthSchedules(catId).enqueue(new Callback<List<HealthScheduleItem>>() {
            @Override
            public void onResponse(Call<List<HealthScheduleItem>> call, Response<List<HealthScheduleItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    textNextCheckup.setText("건강검진: 서버 오류");
                    textNextVaccine.setText("예방접종: 서버 오류");
                    return;
                }

                HealthScheduleItem nextCheckupFuture = null;
                HealthScheduleItem nextVaccineFuture = null;
                HealthScheduleItem nextCheckupAny = null;
                HealthScheduleItem nextVaccineAny = null;

                long nextCheckupFutureMillis = Long.MAX_VALUE;
                long nextVaccineFutureMillis = Long.MAX_VALUE;
                long nextCheckupAnyMillis = Long.MAX_VALUE;
                long nextVaccineAnyMillis = Long.MAX_VALUE;

                Calendar nowCal = Calendar.getInstance();
                nowCal.setTime(new Date());
                nowCal.set(Calendar.HOUR_OF_DAY, 0);
                nowCal.set(Calendar.MINUTE, 0);
                nowCal.set(Calendar.SECOND, 0);
                nowCal.set(Calendar.MILLISECOND, 0);
                long nowMidnightMillis = nowCal.getTimeInMillis();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                sdf.setLenient(false);

                for (HealthScheduleItem item : response.body()) {
                    if (item == null || item.getNextDate() == null) continue;

                    Date d;
                    try {
                        d = sdf.parse(item.getNextDate());
                    } catch (Exception e) {
                        continue;
                    }
                    long dMillis = d.getTime();

                    String typeName = item.getHealthTypeName() != null ? item.getHealthTypeName() : "";
                    boolean isCheckup = typeName.contains("검진");
                    boolean isVaccine = typeName.contains("접종") || typeName.contains("예방");
                    if (!isCheckup && !isVaccine) continue;

                    if (isCheckup) {
                        if (nextCheckupAny == null || dMillis < nextCheckupAnyMillis) {
                            nextCheckupAny = item;
                            nextCheckupAnyMillis = dMillis;
                        }
                        if (dMillis >= nowMidnightMillis) {
                            if (nextCheckupFuture == null || dMillis < nextCheckupFutureMillis) {
                                nextCheckupFuture = item;
                                nextCheckupFutureMillis = dMillis;
                            }
                        }
                    } else {
                        if (nextVaccineAny == null || dMillis < nextVaccineAnyMillis) {
                            nextVaccineAny = item;
                            nextVaccineAnyMillis = dMillis;
                        }
                        if (dMillis >= nowMidnightMillis) {
                            if (nextVaccineFuture == null || dMillis < nextVaccineFutureMillis) {
                                nextVaccineFuture = item;
                                nextVaccineFutureMillis = dMillis;
                            }
                        }
                    }
                }

                HealthScheduleItem checkToShow = nextCheckupFuture != null ? nextCheckupFuture : nextCheckupAny;
                HealthScheduleItem vaccineToShow = nextVaccineFuture != null ? nextVaccineFuture : nextVaccineAny;

                if (checkToShow != null) {
                    textNextCheckup.setText("건강검진: " + checkToShow.getNextDate());
                    layoutNextCheckup.setOnClickListener(v -> showHealthScheduleEditDialog(api, catId, checkToShow));
                } else {
                    textNextCheckup.setText("건강검진: 일정 없음");
                    layoutNextCheckup.setOnClickListener(null);
                }

                if (vaccineToShow != null) {
                    textNextVaccine.setText("예방접종: " + vaccineToShow.getNextDate());
                    layoutNextVaccine.setOnClickListener(v -> showHealthScheduleEditDialog(api, catId, vaccineToShow));
                } else {
                    textNextVaccine.setText("예방접종: 일정 없음");
                    layoutNextVaccine.setOnClickListener(null);
                }
            }

            @Override
            public void onFailure(Call<List<HealthScheduleItem>> call, Throwable t) {
                textNextCheckup.setText("건강검진: 실패");
                textNextVaccine.setText("예방접종: 실패");
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

        Calendar picked = Calendar.getInstance();
        try {
            if (item.getNextDate() != null) {
                Date d = sdf.parse(item.getNextDate());
                picked.setTime(d);
            } else {
                picked.setTime(new Date());
            }
        } catch (Exception e) {
            picked.setTime(new Date());
        }

        Switch switchAlarm = new Switch(requireContext());
        switchAlarm.setText("알림 사용");
        switchAlarm.setChecked(item.getAlarmEnabled() != null && item.getAlarmEnabled());

        TextView labelCycle = new TextView(requireContext());
        labelCycle.setText("커스텀 주기(개월, 비우면 유지)");
        labelCycle.setTextColor(getResources().getColor(R.color.cat_text_secondary));
        labelCycle.setTextSize(12.5f);

        EditText editCycleMonth = new EditText(requireContext());
        editCycleMonth.setHint("예: 6");
        editCycleMonth.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        if (item.getCustomCycleMonth() != null) {
            editCycleMonth.setText(String.valueOf(item.getCustomCycleMonth()));
        }

        TextView labelNextDate = new TextView(requireContext());
        labelNextDate.setText("다음 날짜(yyyy-MM-dd)");
        labelNextDate.setTextColor(getResources().getColor(R.color.cat_text_secondary));
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
                .setNeutralButton("삭제", null)  // null → onShow 에서 override
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
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "저장 완료", Toast.LENGTH_SHORT).show();
                                HealthScheduleAlarmScheduler.syncAlarms(requireContext(), catId);
                                View root = requireView();
                                View layoutCheck = root.findViewById(R.id.layoutNextCheckup);
                                View layoutVaccine = root.findViewById(R.id.layoutNextVaccine);
                                TextView textCheck = root.findViewById(R.id.textNextCheckup);
                                TextView textVaccine = root.findViewById(R.id.textNextVaccine);
                                loadNextHealthSchedules(api, catId, layoutCheck, textCheck, layoutVaccine, textVaccine);
                                loadMonthMemoMarkers();
                                loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false);
                            } else {
                                Toast.makeText(requireContext(), "저장 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(requireContext(), "저장 실패: 네트워크 오류", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .create();

        // 삭제 버튼: dialog 닫히지 않게 override → 확인 후 삭제
        dialog.setOnShowListener(d -> {
            android.widget.Button btnDelete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (btnDelete != null) {
                btnDelete.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("일정 삭제")
                            .setMessage((item.getHealthTypeName() != null ? item.getHealthTypeName() : "이 일정") + "을 삭제할까요?")
                            .setNegativeButton("취소", null)
                            .setPositiveButton("삭제", (confirm, w) -> {
                                dialog.dismiss();
                                api.deleteHealthSchedule(catId, item.getHealthScheduleId())
                                        .enqueue(new Callback<Void>() {
                                    @Override
                                    public void onResponse(Call<Void> call, Response<Void> response) {
                                        if (response.isSuccessful()) {
                                            Toast.makeText(requireContext(), "일정 삭제 완료", Toast.LENGTH_SHORT).show();
                                            HealthScheduleAlarmScheduler.syncAlarms(requireContext(), catId);
                                            View root = requireView();
                                            loadNextHealthSchedules(api, catId,
                                                    root.findViewById(R.id.layoutNextCheckup),
                                                    root.findViewById(R.id.textNextCheckup),
                                                    root.findViewById(R.id.layoutNextVaccine),
                                                    root.findViewById(R.id.textNextVaccine));
                                            loadMonthMemoMarkers();
                                            loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false);
                                        } else {
                                            Toast.makeText(requireContext(), "삭제 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    @Override
                                    public void onFailure(Call<Void> call, Throwable t) {
                                        Toast.makeText(requireContext(), "삭제 실패: 네트워크 오류", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            })
                            .show();
                });
            }
        });

        dialog.show();
        styleDialogWindow(dialog);
    }

    private void loadEventsForDate(
            ApiService api,
            long catId,
            String dateStr,
            TextView textCalendarEmpty,
            boolean initial
    ) {
        clearCalendarEventRows();
        textCalendarEmpty.setText("로딩 중...");

        Call<List<CalendarEventItem>> call = api.getCalendarEvents(
                catId,
                dateStr,
                dateStr
        );

        call.enqueue(new Callback<List<CalendarEventItem>>() {
            @Override
            public void onResponse(Call<List<CalendarEventItem>> call, Response<List<CalendarEventItem>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<CalendarEventItem> items = response.body();
                    currentItems.clear();
                    currentItems.addAll(items);
                    rebuildCalendarEventRows();
                    textCalendarEmpty.setText(filteredItems.isEmpty() ? "필터 조건의 기록이 없습니다" : "");
                } else {
                    textCalendarEmpty.setText("기록을 불러오지 못했습니다 (HTTP " + response.code() + ")");
                }
            }

            @Override
            public void onFailure(Call<List<CalendarEventItem>> call, Throwable t) {
                textCalendarEmpty.setText("기록 불러오기 실패: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()));
            }
        });
    }

    private void clearCalendarEventRows() {
        if (listCalendarEventsContainer != null) {
            listCalendarEventsContainer.removeAllViews();
        }
    }

    private void rebuildCalendarEventRows() {
        if (listCalendarEventsContainer == null || getContext() == null) return;
        listCalendarEventsContainer.removeAllViews();
        filteredItems.clear();
        filteredItems.addAll(getFilteredItems());
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < filteredItems.size(); i++) {
            CalendarEventItem item = filteredItems.get(i);
            if (item == null) continue;
            View row = inflater.inflate(R.layout.item_calendar_list_row, listCalendarEventsContainer, false);
            ImageView icon = row.findViewById(R.id.imageCalendarType);
            View accent = row.findViewById(R.id.viewCalendarAccent);
            FrameLayout iconBg = row.findViewById(R.id.frameCalendarType);
            TextView type = row.findViewById(R.id.textCalendarType);
            TextView primary = row.findViewById(R.id.textCalendarPrimary);
            TextView secondary = row.findViewById(R.id.textCalendarSecondary);

            bindCalendarRowUi(item, icon, accent, iconBg, type, primary, secondary);
            row.setAlpha(0f);
            row.setTranslationY(8f);
            row.animate().alpha(1f).translationY(0f).setDuration(160).start();
            row.setOnTouchListener((v, ev) -> {
                int action = ev.getActionMasked();
                if (action == android.view.MotionEvent.ACTION_DOWN) {
                    v.animate().scaleX(0.985f).scaleY(0.985f).setDuration(85).start();
                } else if (action == android.view.MotionEvent.ACTION_UP
                        || action == android.view.MotionEvent.ACTION_CANCEL) {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(110).start();
                }
                return false;
            });
            final int position = i;
            row.setOnClickListener(v -> {
                if (position >= 0 && position < filteredItems.size()) {
                    showEventDetail(filteredItems.get(position));
                }
            });
            listCalendarEventsContainer.addView(row);
        }
        if (textCalendarEmpty != null) {
            textCalendarEmpty.setText(filteredItems.isEmpty() ? "필터 조건의 기록이 없습니다" : "");
        }
    }

    private List<CalendarEventItem> getFilteredItems() {
        List<CalendarEventItem> out = new ArrayList<>();
        for (CalendarEventItem item : currentItems) {
            if (item == null) continue;
            String t = item.getType() != null ? item.getType() : "MEMO";
            boolean typeEnabled =
                    ("WEIGHT".equals(t) && filterWeight)
                            || ("VOMIT".equals(t) && filterVomit)
                            || ("HEALTH_CHECKUP".equals(t) && filterCheckup)
                            || ("HEALTH_VACCINE".equals(t) && filterVaccine)
                            || (("MEMO".equals(t) || "MEDICATION".equals(t)
                                || "LITTER".equals(t) || "VET_VISIT".equals(t)) && filterMemo);
            if (!typeEnabled) continue;
            out.add(item);
        }
        return out;
    }

    private void bindCalendarRowUi(
            CalendarEventItem item,
            ImageView icon,
            View accent,
            FrameLayout iconBg,
            TextView type,
            TextView primary,
            TextView secondary
    ) {
        String t = item.getType() != null ? item.getType() : "MEMO";
        String typeLabel;
        String main;
        int iconRes;
        int tintColor;
        int accentColor;

        if ("WEIGHT".equals(t)) {
            typeLabel = "몸무게";
            main = trimPreview(item.getSubtitle(), 16);
            iconRes = R.drawable.ic_weight_scale;
            tintColor = R.color.semantic_info;
            accentColor = R.color.semantic_info;
        } else if ("VOMIT".equals(t)) {
            typeLabel = "토 분석";
            String color = vomitColorKorean(item.getVomitColor());
            String risk = riskLevelKorean(item.getRiskLevel());
            main = color + " / 위험도 " + risk;
            iconRes = R.drawable.ic_vomit_drop;
            tintColor = R.color.semantic_warning;
            accentColor = R.color.semantic_warning;
        } else if ("HEALTH_CHECKUP".equals(t)) {
            typeLabel = "건강검진";
            String memo = item.getScheduleMemo();
            main = (memo != null && !memo.isEmpty())
                    ? trimPreview(memo, 22)
                    : trimPreview(item.getSubtitle(), 22);
            iconRes = R.drawable.ic_checkup_clipboard;
            tintColor = R.color.app_primary_dark;
            accentColor = R.color.app_primary_dark;
        } else if ("HEALTH_VACCINE".equals(t)) {
            typeLabel = "예방접종";
            String memo = item.getScheduleMemo();
            main = (memo != null && !memo.isEmpty())
                    ? trimPreview(memo, 22)
                    : trimPreview(item.getSubtitle(), 22);
            iconRes = R.drawable.ic_vaccine_syringe;
            tintColor = R.color.semantic_success;
            accentColor = R.color.semantic_success;
        } else if ("MEDICATION".equals(t)) {
            typeLabel = "투약";
            main = trimPreview(item.getSubtitle(), 26);
            iconRes = R.drawable.ic_vaccine_syringe;
            tintColor = R.color.semantic_success;
            accentColor = R.color.semantic_success;
        } else if ("LITTER".equals(t)) {
            typeLabel = "화장실";
            main = trimPreview(item.getSubtitle(), 26);
            iconRes = R.drawable.ic_vomit_drop;
            tintColor = R.color.app_on_surface_variant;
            accentColor = R.color.app_on_surface_variant;
        } else if ("VET_VISIT".equals(t)) {
            typeLabel = "진료";
            main = trimPreview(item.getSubtitle(), 26);
            iconRes = R.drawable.ic_hospital;
            tintColor = R.color.semantic_warning;
            accentColor = R.color.semantic_warning;
        } else {
            if ("MEMO".equals(t) && item.getSubtitle() != null && item.getSubtitle().startsWith("비만도 |")) {
                typeLabel = "비만도";
                main = trimPreview(item.getSubtitle(), 26);
                iconRes = R.drawable.ic_checkup_clipboard;
                tintColor = R.color.semantic_info;
                accentColor = R.color.semantic_info;
            } else {
                typeLabel = "메모";
                main = trimPreview(item.getSubtitle(), 22);
                iconRes = R.drawable.ic_home;
                tintColor = R.color.app_on_surface_variant;
                accentColor = R.color.app_on_surface_variant;
            }
        }

        type.setText(typeLabel);
        primary.setText(main);
        icon.setImageResource(iconRes);
        int tint = ContextCompat.getColor(requireContext(), tintColor);
        icon.setColorFilter(tint);
        if (accent != null) accent.setBackgroundColor(ContextCompat.getColor(requireContext(), accentColor));
        if (iconBg != null) {
            int bg = ContextCompat.getColor(requireContext(), R.color.app_surface_muted);
            iconBg.setBackgroundTintList(ColorStateList.valueOf(bg));
        }

        String st = item.getTime();
        if (st != null && !st.isEmpty()) {
            secondary.setVisibility(View.VISIBLE);
            secondary.setText(st);
        } else {
            if ("HEALTH_CHECKUP".equals(t) || "HEALTH_VACCINE".equals(t)) {
                String date = item.getDate();
                if (date != null && date.length() >= 10) {
                    secondary.setVisibility(View.VISIBLE);
                    secondary.setText(date.substring(5, 10));
                } else {
                    secondary.setVisibility(View.GONE);
                }
            } else {
                secondary.setVisibility(View.GONE);
            }
        }
    }

    private static String vomitColorKorean(String color) {
        if (color == null) return "미상";
        switch (color.toUpperCase(Locale.ROOT)) {
            case "WHITE":   return "흰색";
            case "YELLOW":  return "노란색";
            case "GREEN":   return "녹색";
            case "RED":     return "붉은색";
            case "BROWN":   return "갈색";
            case "BLACK":   return "검은색";
            default:        return "색불명";
        }
    }

    private static String riskLevelKorean(String risk) {
        if (risk == null) return "미상";
        switch (risk.toUpperCase(Locale.ROOT)) {
            case "HIGH":   return "높음";
            case "LOW":    return "낮음";
            case "MEDIUM": return "보통";
            default:       return risk;
        }
    }

    private String trimPreview(String s, int n) {
        String v = (s != null ? s.trim() : "");
        if (v.isEmpty()) return "내용 없음";
        if (v.length() <= n) return v;
        return v.substring(0, n) + "...";
    }

    private void showEventDetail(CalendarEventItem item) {
        if (item == null) return;
        String type = item.getType();
        if ("MEMO".equals(type)) {
            showMemoDetailDialog(item);
        } else if ("VOMIT".equals(type)) {
            showVomitDetailDialog(item);
        } else if ("WEIGHT".equals(type)) {
            showWeightDetailDialog(item);
        } else if ("HEALTH_CHECKUP".equals(type) || "HEALTH_VACCINE".equals(type)) {
            showHealthEventDetailDialog(item);
        } else if ("MEDICATION".equals(type)) {
            showMedicationDetailDialog(item);
        } else if ("LITTER".equals(type)) {
            showLitterDetailDialog(item);
        } else if ("VET_VISIT".equals(type)) {
            showVetVisitDetailDialog(item);
        } else {
            Toast.makeText(requireContext(), "이 유형은 아직 상세 화면이 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    // ── 투약 기록 ──────────────────────────────────────────────────────────────

    private void showMedicationDetailDialog(CalendarEventItem item) {
        LinearLayout container = buildDetailContainer("투약");
        if (item.getSubtitle() != null) addDetailLine(container, item.getSubtitle(), true);
        if (item.getGuideText() != null && !item.getGuideText().isEmpty()) {
            addDetailTag(container, "메모");
            addDetailLine(container, item.getGuideText(), false);
        }
        showDetailBottomSheet(container,
                "수정", () -> showEditMedicationDialog(item.getId()),
                "삭제", () -> showDeleteMedicationDialog(item.getId()));
    }

    private void showEditMedicationDialog(Long medicationId) {
        if (api == null || catId <= 0 || medicationId == null) return;
        api.getMedications(catId).enqueue(new Callback<List<MedicationItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<MedicationItem>> call, @NonNull Response<List<MedicationItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "기록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                MedicationItem found = null;
                for (MedicationItem m : response.body()) {
                    if (m.getMedicationId() != null && m.getMedicationId().equals(medicationId)) {
                        found = m; break;
                    }
                }
                if (found == null) { Toast.makeText(requireContext(), "기록을 찾지 못했습니다.", Toast.LENGTH_SHORT).show(); return; }
                showMedicationEditForm(medicationId, found);
            }
            @Override
            public void onFailure(@NonNull Call<List<MedicationItem>> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMedicationEditForm(Long medicationId, MedicationItem item) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_medication_add, null, false);
        EditText editName = dialogView.findViewById(R.id.editMedName);
        EditText editDosage = dialogView.findViewById(R.id.editMedDosage);
        com.google.android.material.button.MaterialButtonToggleGroup toggleFreq = dialogView.findViewById(R.id.toggleMedFrequency);
        com.google.android.material.button.MaterialButtonToggleGroup toggleMeridiem = dialogView.findViewById(R.id.toggleMedMeridiem);
        EditText editStartDate = dialogView.findViewById(R.id.editMedStartDate);
        EditText editEndDate = dialogView.findViewById(R.id.editMedEndDate);
        TextView textAlarmTime = dialogView.findViewById(R.id.textMedAlarmTime);
        MaterialSwitch switchAlarm = dialogView.findViewById(R.id.switchMedAlarm);
        EditText editNotes = dialogView.findViewById(R.id.editMedNotes);

        if (editName != null) editName.setText(item.getName());
        if (editDosage != null && item.getDosage() != null) editDosage.setText(item.getDosage());
        if (editStartDate != null && item.getStartDate() != null) editStartDate.setText(item.getStartDate());
        if (editEndDate != null && item.getEndDate() != null) editEndDate.setText(item.getEndDate());
        if (editNotes != null && item.getNotes() != null) editNotes.setText(item.getNotes());
        if (switchAlarm != null) switchAlarm.setChecked(Boolean.TRUE.equals(item.getAlarmEnabled()));
        if (toggleFreq != null) {
            if ("TWICE_DAILY".equals(item.getFrequency())) toggleFreq.check(R.id.btnFreqTwice);
            else if ("AS_NEEDED".equals(item.getFrequency())) toggleFreq.check(R.id.btnFreqAsNeeded);
            else toggleFreq.check(R.id.btnFreqOnce);
        }
        final int[] alarmH = {item.getAlarmHour() != null ? item.getAlarmHour() : 9};
        final int[] alarmM = {item.getAlarmMinute() != null ? item.getAlarmMinute() : 0};
        final boolean[] isAmEdit = {alarmH[0] < 12};

        if (toggleMeridiem != null) {
            toggleMeridiem.check(isAmEdit[0] ? R.id.btnMeridiemAm : R.id.btnMeridiemPm);
            toggleMeridiem.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                isAmEdit[0] = (checkedId == R.id.btnMeridiemAm);
                int h12 = alarmH[0] % 12 == 0 ? 12 : alarmH[0] % 12;
                alarmH[0] = isAmEdit[0] ? (h12 % 12) : (h12 % 12 + 12);
                if (textAlarmTime != null)
                    textAlarmTime.setText(String.format(Locale.getDefault(), "%d:%02d · 탭해서 변경", h12, alarmM[0]));
            });
        }
        if (textAlarmTime != null) {
            int initH12 = alarmH[0] % 12 == 0 ? 12 : alarmH[0] % 12;
            textAlarmTime.setText(String.format(Locale.getDefault(), "%d:%02d · 탭해서 변경", initH12, alarmM[0]));
            textAlarmTime.setOnClickListener(v -> {
                int h12init = alarmH[0] % 12 == 0 ? 12 : alarmH[0] % 12;
                new TimePickerDialog(requireContext(), (tp, h, m) -> {
                    alarmM[0] = m;
                    int h12 = h == 0 ? 12 : h;
                    alarmH[0] = isAmEdit[0] ? (h % 12) : (h % 12 + 12);
                    textAlarmTime.setText(String.format(Locale.getDefault(), "%d:%02d · 탭해서 변경", h12, m));
                }, h12init == 12 ? 0 : h12init, alarmM[0], false).show();
            });
        }
        if (editStartDate != null) editStartDate.setOnClickListener(v -> showEditDatePicker(editStartDate));
        if (editEndDate != null) editEndDate.setOnClickListener(v -> showEditDatePicker(editEndDate));

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.btnMedSave).setOnClickListener(v -> {
            String name = editName != null && editName.getText() != null ? editName.getText().toString().trim() : "";
            if (name.isEmpty()) { Toast.makeText(requireContext(), "약 이름을 입력해 주세요.", Toast.LENGTH_SHORT).show(); return; }
            String dosage = editDosage != null && editDosage.getText() != null ? editDosage.getText().toString().trim() : "";
            String startDate = editStartDate != null && editStartDate.getText() != null ? editStartDate.getText().toString().trim() : "";
            String endDate = editEndDate != null && editEndDate.getText() != null ? editEndDate.getText().toString().trim() : null;
            if (endDate != null && endDate.isEmpty()) endDate = null;
            String freq = "DAILY";
            if (toggleFreq != null) {
                if (toggleFreq.getCheckedButtonId() == R.id.btnFreqTwice) freq = "TWICE_DAILY";
                else if (toggleFreq.getCheckedButtonId() == R.id.btnFreqAsNeeded) freq = "AS_NEEDED";
            }
            boolean alarmEnabled = switchAlarm != null && switchAlarm.isChecked();
            String notes = editNotes != null && editNotes.getText() != null ? editNotes.getText().toString().trim() : "";
            MedicationCreateRequest req = new MedicationCreateRequest(
                    name, dosage.isEmpty() ? null : dosage, freq, startDate, endDate,
                    alarmEnabled, alarmH[0], alarmM[0], notes.isEmpty() ? null : notes);
            api.updateMedication(catId, medicationId, req).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "수정 완료", Toast.LENGTH_SHORT).show();
                        MedicationAlarmScheduler.syncAlarms(requireContext(), catId);
                        dialog.dismiss();
                        loadMonthMemoMarkers();
                        loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false);
                    } else Toast.makeText(requireContext(), "수정 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                    Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show();
                }
            });
        });
        dialogView.findViewById(R.id.btnMedCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDeleteMedicationDialog(Long id) {
        if (id == null || api == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("투약 기록 삭제").setMessage("이 투약 기록을 삭제할까요?")
                .setNegativeButton("취소", (d, w) -> d.dismiss())
                .setPositiveButton("삭제", (d, w) -> api.deleteMedication(catId, id).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        Toast.makeText(requireContext(), response.isSuccessful() ? "삭제 완료" : "삭제 실패", Toast.LENGTH_SHORT).show();
                        if (response.isSuccessful()) {
                            MedicationAlarmScheduler.cancelAlarmForMedication(requireContext(), catId, id);
                            loadMonthMemoMarkers();
                            loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) { Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show(); }
                })).show();
    }

    // ── 화장실 기록 ────────────────────────────────────────────────────────────

    private void showLitterDetailDialog(CalendarEventItem item) {
        LinearLayout container = buildDetailContainer("화장실");
        if (item.getSubtitle() != null) addDetailLine(container, item.getSubtitle(), true);
        if (item.getGuideText() != null && !item.getGuideText().isEmpty()) {
            addDetailTag(container, "메모");
            addDetailLine(container, item.getGuideText(), false);
        }
        showDetailBottomSheet(container,
                "수정", () -> showEditLitterDialog(item.getId()),
                "삭제", () -> showDeleteLitterDialog(item.getId()));
    }

    private void showEditLitterDialog(Long recordId) {
        if (api == null || catId <= 0 || recordId == null) return;
        api.getLitterRecords(catId, 100).enqueue(new Callback<List<LitterBoxItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<LitterBoxItem>> call, @NonNull Response<List<LitterBoxItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "기록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show(); return;
                }
                LitterBoxItem found = null;
                for (LitterBoxItem lb : response.body()) {
                    if (lb.getRecordId() != null && lb.getRecordId().equals(recordId)) { found = lb; break; }
                }
                if (found == null) { Toast.makeText(requireContext(), "기록을 찾지 못했습니다.", Toast.LENGTH_SHORT).show(); return; }
                showLitterEditForm(recordId, found);
            }
            @Override
            public void onFailure(@NonNull Call<List<LitterBoxItem>> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLitterEditForm(Long recordId, LitterBoxItem item) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_litter_box, null, false);
        TextView textCount = dialogView.findViewById(R.id.textLitterCount);
        View btnMinus = dialogView.findViewById(R.id.btnLitterCountMinus);
        View btnPlus = dialogView.findViewById(R.id.btnLitterCountPlus);
        com.google.android.material.button.MaterialButtonToggleGroup toggleColor = dialogView.findViewById(R.id.toggleLitterColor);
        com.google.android.material.button.MaterialButtonToggleGroup toggleShape = dialogView.findViewById(R.id.toggleLitterShape);
        MaterialSwitch switchAbnormal = dialogView.findViewById(R.id.switchLitterAbnormal);
        EditText editNotes = dialogView.findViewById(R.id.editLitterNotes);

        final int[] count = {item.getCount() != null ? item.getCount() : 1};
        if (textCount != null) textCount.setText(String.valueOf(count[0]));
        if (switchAbnormal != null) switchAbnormal.setChecked(Boolean.TRUE.equals(item.getAbnormal()));
        if (editNotes != null && item.getNotes() != null) editNotes.setText(item.getNotes());
        if (toggleColor != null) {
            if ("YELLOW".equals(item.getColor())) toggleColor.check(R.id.btnColorYellow);
            else if ("RED".equals(item.getColor())) toggleColor.check(R.id.btnColorRed);
            else if ("OTHER".equals(item.getColor())) toggleColor.check(R.id.btnColorOther);
            else toggleColor.check(R.id.btnColorNormal);
        }
        if (toggleShape != null) {
            if ("SOFT".equals(item.getShape())) toggleShape.check(R.id.btnShapeSoft);
            else if ("LIQUID".equals(item.getShape())) toggleShape.check(R.id.btnShapeLiquid);
            else if ("NONE".equals(item.getShape())) toggleShape.check(R.id.btnShapeNone);
            else toggleShape.check(R.id.btnShapeNormal);
        }
        if (btnMinus != null) btnMinus.setOnClickListener(v -> { if (count[0] > 1) { count[0]--; if (textCount != null) textCount.setText(String.valueOf(count[0])); } });
        if (btnPlus != null) btnPlus.setOnClickListener(v -> { count[0]++; if (textCount != null) textCount.setText(String.valueOf(count[0])); });

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.btnLitterSave).setOnClickListener(v -> {
            String color = "NORMAL";
            if (toggleColor != null) {
                if (toggleColor.getCheckedButtonId() == R.id.btnColorYellow) color = "YELLOW";
                else if (toggleColor.getCheckedButtonId() == R.id.btnColorRed) color = "RED";
                else if (toggleColor.getCheckedButtonId() == R.id.btnColorOther) color = "OTHER";
            }
            String shape = "NORMAL";
            if (toggleShape != null) {
                if (toggleShape.getCheckedButtonId() == R.id.btnShapeSoft) shape = "SOFT";
                else if (toggleShape.getCheckedButtonId() == R.id.btnShapeLiquid) shape = "LIQUID";
                else if (toggleShape.getCheckedButtonId() == R.id.btnShapeNone) shape = "NONE";
            }
            boolean abnormal = switchAbnormal != null && switchAbnormal.isChecked();
            String notes = editNotes != null && editNotes.getText() != null ? editNotes.getText().toString().trim() : null;
            String date = item.getDate() != null ? item.getDate() : new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
            LitterBoxCreateRequest req = new LitterBoxCreateRequest(date, count[0], color, shape, abnormal, notes != null && notes.isEmpty() ? null : notes);
            api.updateLitterRecord(catId, recordId, req).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "수정 완료", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadMonthMemoMarkers();
                        loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false);
                    } else Toast.makeText(requireContext(), "수정 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) { Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show(); }
            });
        });
        dialogView.findViewById(R.id.btnLitterCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDeleteLitterDialog(Long id) {
        if (id == null || api == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("화장실 기록 삭제").setMessage("이 기록을 삭제할까요?")
                .setNegativeButton("취소", (d, w) -> d.dismiss())
                .setPositiveButton("삭제", (d, w) -> api.deleteLitterRecord(catId, id).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        Toast.makeText(requireContext(), response.isSuccessful() ? "삭제 완료" : "삭제 실패", Toast.LENGTH_SHORT).show();
                        if (response.isSuccessful()) { loadMonthMemoMarkers(); loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false); }
                    }
                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) { Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show(); }
                })).show();
    }

    // ── 진료 기록 ──────────────────────────────────────────────────────────────

    private void showVetVisitDetailDialog(CalendarEventItem item) {
        LinearLayout container = buildDetailContainer("진료");
        if (item.getSubtitle() != null) addDetailLine(container, item.getSubtitle(), true);
        if (item.getGuideText() != null && !item.getGuideText().isEmpty()) {
            addDetailTag(container, "메모");
            addDetailLine(container, item.getGuideText(), false);
        }
        showDetailBottomSheet(container,
                "수정", () -> showEditVetVisitDialog(item.getId()),
                "삭제", () -> showDeleteVetVisitDialog(item.getId()));
    }

    private void showEditVetVisitDialog(Long visitId) {
        if (api == null || catId <= 0 || visitId == null) return;
        api.getVetVisits(catId).enqueue(new Callback<List<VetVisitItem>>() {
            @Override
            public void onResponse(@NonNull Call<List<VetVisitItem>> call, @NonNull Response<List<VetVisitItem>> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "기록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show(); return;
                }
                VetVisitItem found = null;
                for (VetVisitItem vv : response.body()) {
                    if (vv.getVisitId() != null && vv.getVisitId().equals(visitId)) { found = vv; break; }
                }
                if (found == null) { Toast.makeText(requireContext(), "기록을 찾지 못했습니다.", Toast.LENGTH_SHORT).show(); return; }
                showVetVisitEditForm(visitId, found);
            }
            @Override
            public void onFailure(@NonNull Call<List<VetVisitItem>> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(), "불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showVetVisitEditForm(Long visitId, VetVisitItem item) {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_vet_visit, null, false);
        EditText editDate = dialogView.findViewById(R.id.editVetDate);
        EditText editHospital = dialogView.findViewById(R.id.editVetHospital);
        EditText editDiagnosis = dialogView.findViewById(R.id.editVetDiagnosis);
        EditText editPrescription = dialogView.findViewById(R.id.editVetPrescription);
        EditText editNotes = dialogView.findViewById(R.id.editVetNotes);

        if (editDate != null && item.getDate() != null) { editDate.setText(item.getDate()); editDate.setOnClickListener(v -> showEditDatePicker(editDate)); }
        if (editHospital != null && item.getHospitalName() != null) editHospital.setText(item.getHospitalName());
        if (editDiagnosis != null && item.getDiagnosis() != null) editDiagnosis.setText(item.getDiagnosis());
        if (editPrescription != null && item.getPrescription() != null) editPrescription.setText(item.getPrescription());
        if (editNotes != null && item.getNotes() != null) editNotes.setText(item.getNotes());

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogView).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        dialogView.findViewById(R.id.btnVetSave).setOnClickListener(v -> {
            String date = editDate != null && editDate.getText() != null ? editDate.getText().toString().trim() : "";
            String hospital = editHospital != null && editHospital.getText() != null ? editHospital.getText().toString().trim() : "";
            String diagnosis = editDiagnosis != null && editDiagnosis.getText() != null ? editDiagnosis.getText().toString().trim() : "";
            String prescription = editPrescription != null && editPrescription.getText() != null ? editPrescription.getText().toString().trim() : "";
            String notes = editNotes != null && editNotes.getText() != null ? editNotes.getText().toString().trim() : "";
            VetVisitCreateRequest req = new VetVisitCreateRequest(
                    date.isEmpty() ? item.getDate() : date,
                    hospital.isEmpty() ? null : hospital,
                    diagnosis.isEmpty() ? null : diagnosis,
                    prescription.isEmpty() ? null : prescription,
                    notes.isEmpty() ? null : notes);
            api.updateVetVisit(catId, visitId, req).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(requireContext(), "수정 완료", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadMonthMemoMarkers();
                        loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false);
                    } else Toast.makeText(requireContext(), "수정 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) { Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show(); }
            });
        });
        dialogView.findViewById(R.id.btnVetCancel).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showDeleteVetVisitDialog(Long id) {
        if (id == null || api == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("진료 기록 삭제").setMessage("이 진료 기록을 삭제할까요?")
                .setNegativeButton("취소", (d, w) -> d.dismiss())
                .setPositiveButton("삭제", (d, w) -> api.deleteVetVisit(catId, id).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        Toast.makeText(requireContext(), response.isSuccessful() ? "삭제 완료" : "삭제 실패", Toast.LENGTH_SHORT).show();
                        if (response.isSuccessful()) { loadMonthMemoMarkers(); loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false); }
                    }
                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) { Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show(); }
                })).show();
    }

    // ── 날짜 선택 헬퍼 ────────────────────────────────────────────────────────

    private void showEditDatePicker(EditText target) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        String existing = target.getText() != null ? target.getText().toString().trim() : "";
        if (existing.length() == 10) {
            try {
                String[] p = existing.split("-");
                cal.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
            } catch (Exception ignored) {}
        }
        new DatePickerDialog(requireContext(), (view, year, month, day) ->
                target.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)),
                cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showMemoDetailDialog(CalendarEventItem item) {
        if (item.getId() == null) return;

        String content = item.getSubtitle() != null ? item.getSubtitle() : "";
        CalendarEventItem linkedVomit = findLinkedVomitFromMemo(item);

        LinearLayout container = buildDetailContainer("메모 상세");
        addDetailLine(container, content.isEmpty() ? "메모 내용이 비어있습니다." : content, true);

        if (linkedVomit != null) {
            addDetailTag(container, "연결된 토 분석");

            String color = vomitColorKorean(linkedVomit.getVomitColor());
            addDetailLine(container, "색상: " + color, false);
            String risk = riskLevelKorean(linkedVomit.getRiskLevel());
            addDetailLine(container, "위험도: " + risk, false);
            addVomitImageView(container, linkedVomit.getImagePath());
        }

        boolean obesityMemo = isObesityMemo(item);
        showDetailBottomSheet(
                container,
                obesityMemo ? null : "수정",
                obesityMemo ? null : () -> showMemoEditDialog(item),
                "삭제",
                () -> showDeleteMemoDialog(item.getId())
        );
    }

    private CalendarEventItem findLinkedVomitFromMemo(CalendarEventItem memoItem) {
        if (memoItem == null) return null;
        String type = memoItem.getType();
        if (!"MEMO".equals(type)) return null;
        String title = memoItem.getTitle() != null ? memoItem.getTitle() : "";
        String content = memoItem.getSubtitle() != null ? memoItem.getSubtitle() : "";
        boolean looksVomitMemo = title.contains("토") || content.contains("토 기록");
        if (!looksVomitMemo) return null;
        String date = memoItem.getDate();
        if (date == null || date.length() < 10) return null;
        String d = date.substring(0, 10);
        for (CalendarEventItem e : currentItems) {
            if (e == null) continue;
            if (!"VOMIT".equals(e.getType())) continue;
            String ed = e.getDate();
            if (ed == null || ed.length() < 10) continue;
            if (d.equals(ed.substring(0, 10))) {
                return e;
            }
        }
        return null;
    }

    private void addVomitImageView(LinearLayout container, String imagePath) {
        if (container == null || imagePath == null || imagePath.isEmpty()) return;
        ImageView image = new ImageView(requireContext());
        image.setAdjustViewBounds(true);
        image.setMaxHeight(600);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setPadding(24, 6, 24, 10);
        try {
            image.setImageURI(Uri.parse(imagePath));
            image.setOnClickListener(v -> {
                List<String> images = collectSameDayVomitImages();
                int start = images.indexOf(imagePath);
                if (images.isEmpty()) images.add(imagePath);
                showVomitImageGallery(images, start >= 0 ? start : 0);
            });
            container.addView(image);
        } catch (Exception ignored) {
        }
    }

    private List<String> collectSameDayVomitImages() {
        List<String> out = new ArrayList<>();
        for (CalendarEventItem e : currentItems) {
            if (e == null || !"VOMIT".equals(e.getType())) continue;
            String p = e.getImagePath();
            if (p != null && !p.isEmpty()) out.add(p);
        }
        return out;
    }

    private void showVomitImageGallery(List<String> imagePaths, int startIndex) {
        if (imagePaths == null || imagePaths.isEmpty()) return;
        final int[] idx = {Math.max(0, Math.min(startIndex, imagePaths.size() - 1))};

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 10);

        TextView counter = new TextView(requireContext());
        counter.setTextColor(getResources().getColor(R.color.app_on_surface_variant));
        counter.setTextSize(12.5f);
        root.addView(counter);

        ImageView image = new ImageView(requireContext());
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setMaxHeight(900);
        root.addView(image);

        LinearLayout nav = new LinearLayout(requireContext());
        nav.setOrientation(LinearLayout.HORIZONTAL);
        Button prev = new Button(requireContext());
        prev.setText("이전");
        Button next = new Button(requireContext());
        next.setText("다음");
        nav.addView(prev);
        nav.addView(next);
        root.addView(nav);

        Runnable render = () -> {
            String path = imagePaths.get(idx[0]);
            counter.setText((idx[0] + 1) + " / " + imagePaths.size());
            try {
                image.setImageURI(Uri.parse(path));
            } catch (Exception ignored) {
            }
        };
        prev.setOnClickListener(v -> {
            idx[0] = Math.max(0, idx[0] - 1);
            render.run();
        });
        next.setOnClickListener(v -> {
            idx[0] = Math.min(imagePaths.size() - 1, idx[0] + 1);
            render.run();
        });
        image.setOnClickListener(v -> {
            String path = imagePaths.get(idx[0]);
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(path), "image/*");
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(intent);
            } catch (Exception ignored) {
            }
        });
        render.run();

        AlertDialog d = new AlertDialog.Builder(requireContext())
                .setTitle("사진 갤러리")
                .setView(root)
                .setNegativeButton("닫기", (di, w) -> di.dismiss())
                .show();
        styleDialogWindow(d);
    }

    private void showVomitDetailDialog(CalendarEventItem item) {
        if (item.getId() == null) return;

        String color = vomitColorKorean(item.getVomitColor());
        String risk = riskLevelKorean(item.getRiskLevel());
        String guide = item.getGuideText();
        LinearLayout container = buildDetailContainer("토 분석");
        addDetailLine(container, "색상: " + color, false);
        addDetailLine(container, "위험도: " + risk, false);

        if (guide != null && !guide.isEmpty()) {
            addDetailLine(container, guide, false);
        }

        addVomitImageView(container, item.getImagePath());

        showDetailBottomSheet(
                container,
                null,
                null,
                "삭제",
                () -> showDeleteVomitDialog(item.getId())
        );
    }

    private void showWeightDetailDialog(CalendarEventItem item) {
        String subtitle = item.getSubtitle() != null ? item.getSubtitle() : "";
        String time = item.getTime() != null && !item.getTime().isEmpty() ? item.getTime() : "";

        LinearLayout box = buildDetailContainer("몸무게");
        addDetailTag(box, subtitle.isEmpty() ? "-" : subtitle);

        if (!time.isEmpty()) {
            TextView chip = new TextView(requireContext());
            chip.setText(time);
            chip.setTextSize(12f);
            chip.setTextColor(getResources().getColor(R.color.app_on_surface_muted));
            chip.setBackgroundResource(R.drawable.bg_chip_soft);
            chip.setPadding(12, 6, 12, 6);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(24, 0, 0, 8);
            chip.setLayoutParams(lp);
            box.addView(chip);
        }

        showDetailBottomSheet(box, null, null, null, null);
    }

    private void showHealthEventDetailDialog(CalendarEventItem item) {
        if (item.getId() == null || api == null) return;

        api.getHealthSchedules(catId).enqueue(new Callback<List<HealthScheduleItem>>() {
            @Override
            public void onResponse(Call<List<HealthScheduleItem>> call, Response<List<HealthScheduleItem>> response) {
                HealthScheduleItem found = null;
                if (response.isSuccessful() && response.body() != null) {
                    for (HealthScheduleItem h : response.body()) {
                        if (h != null && h.getHealthScheduleId() != null && h.getHealthScheduleId().equals(item.getId())) {
                            found = h;
                            break;
                        }
                    }
                }
                showHealthEventDetailDialogInner(item, found);
            }

            @Override
            public void onFailure(Call<List<HealthScheduleItem>> call, Throwable t) {
                showHealthEventDetailDialogInner(item, null);
            }
        });
    }

    private void showHealthEventDetailDialogInner(CalendarEventItem item, HealthScheduleItem scheduleItem) {
        LinearLayout box = buildDetailContainer("건강 일정");

        StringBuilder sb = new StringBuilder();
        boolean isCompleted = item.getTitle() != null && item.getTitle().endsWith("완료");
        if (isCompleted) {
            sb.append("완료 날짜: ").append(item.getDate() != null ? item.getDate() : "-");
            String sub = item.getSubtitle();
            if (sub != null && sub.contains("다음: ")) {
                sb.append("\n").append(sub.substring(sub.indexOf("다음: ")));
            }
        } else {
            sb.append("예정 날짜: ").append(item.getDate() != null ? item.getDate() : "-");
        }
        if (item.getAlarmEnabled() != null) {
            sb.append("\n알림: ").append(item.getAlarmEnabled() ? "켜짐" : "꺼짐");
        }
        if (!isCompleted) {
            sb.append("\n\n연결 메모:\n");
            String sm = item.getScheduleMemo();
            sb.append(sm != null && !sm.isEmpty() ? sm : "(없음)");
        }
        addDetailLine(box, sb.toString(), true);

        showDetailBottomSheet(
                box,
                "일정 수정",
                () -> {
                    if (scheduleItem != null) {
                        showHealthScheduleEditDialog(api, catId, scheduleItem);
                    } else {
                        Toast.makeText(requireContext(), "일정 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    }
                },
                (item.getLinkedMemoId() != null ? "메모 수정" : "메모 추가"),
                () -> {
                    if (item.getLinkedMemoId() != null) {
                        showLinkedMemoEditDialog(item.getLinkedMemoId(), item.getScheduleMemo(), item.getDate());
                    } else {
                        showAddHealthMemoDialog(item);
                    }
                }
        );
    }

    private LinearLayout buildDetailContainer(String title) {
        LinearLayout container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(10, 10, 10, 10);
        container.setBackgroundResource(R.drawable.bg_card_premium);

        int iconRes = R.drawable.ic_home;
        if ("토 분석".equals(title)) iconRes = R.drawable.ic_vomit_drop;
        if ("몸무게".equals(title)) iconRes = R.drawable.ic_weight_scale;
        if ("건강 일정".equals(title)) iconRes = R.drawable.ic_checkup_clipboard;
        if ("메모 상세".equals(title)) iconRes = R.drawable.ic_home;
        if ("투약".equals(title)) iconRes = R.drawable.ic_vaccine_syringe;
        if ("화장실".equals(title)) iconRes = R.drawable.ic_vomit_drop;
        if ("진료".equals(title)) iconRes = R.drawable.ic_hospital;

        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(6, 6, 6, 8);

        FrameLayout iconWrap = new FrameLayout(requireContext());
        iconWrap.setLayoutParams(new LinearLayout.LayoutParams(40, 40));
        iconWrap.setBackgroundResource(R.drawable.bg_icon_circle_soft);
        iconWrap.setPadding(9, 9, 9, 9);
        ImageView icon = new ImageView(requireContext());
        icon.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        icon.setImageResource(iconRes);
        icon.setColorFilter(getResources().getColor(R.color.app_primary_dark));
        iconWrap.addView(icon);
        header.addView(iconWrap);

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(getResources().getColor(R.color.app_on_surface));
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.leftMargin = 12;
        tvTitle.setLayoutParams(tlp);
        header.addView(tvTitle);
        container.addView(header);
        return container;
    }

    private void addDetailTag(LinearLayout container, String text) {
        TextView tag = new TextView(requireContext());
        tag.setText(text);
        tag.setTextSize(12.5f);
        tag.setTextColor(getResources().getColor(R.color.app_on_surface));
        tag.setBackgroundResource(R.drawable.bg_chip_soft);
        tag.setPadding(14, 8, 14, 8);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(24, 0, 24, 8);
        tag.setLayoutParams(lp);
        container.addView(tag);
    }

    private void addDetailLine(LinearLayout container, String text, boolean primary) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(primary ? 14.5f : 13.2f);
        tv.setLineSpacing(2f, 1.12f);
        tv.setTextColor(getResources().getColor(primary ? R.color.app_on_surface : R.color.app_on_surface_variant));
        tv.setBackgroundResource(R.drawable.bg_chip_soft);
        tv.setPadding(18, 10, 18, 10);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(24, primary ? 10 : 8, 24, 0);
        tv.setLayoutParams(lp);
        container.addView(tv);
    }

    private void styleDialogWindow(AlertDialog dialog) {
        if (dialog == null || dialog.getWindow() == null) return;
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_card_glass);
        dialog.getWindow().setWindowAnimations(android.R.style.Animation_Dialog);
    }

    private void showDetailBottomSheet(
            LinearLayout content,
            String primaryLabel,
            Runnable primaryAction,
            String secondaryLabel,
            Runnable secondaryAction
    ) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_card_glass);
        root.setPadding(12, 8, 12, 12);

        View handle = new View(requireContext());
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(120, 10);
        hlp.gravity = Gravity.CENTER_HORIZONTAL;
        hlp.bottomMargin = 8;
        handle.setLayoutParams(hlp);
        handle.setBackgroundResource(R.drawable.bg_chip_soft);
        root.addView(handle);

        root.addView(content);

        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(18, 14, 18, 6);

        MaterialButton btnClose = createSheetActionButton("닫기", R.color.app_surface_muted, R.color.app_on_surface);
        btnClose.setOnClickListener(v -> sheet.dismiss());
        actions.addView(btnClose);

        if (secondaryLabel != null && secondaryAction != null) {
            MaterialButton btnSecondary = createSheetActionButton(secondaryLabel, R.color.app_primary_container, R.color.app_on_primary_container);
            btnSecondary.setOnClickListener(v -> {
                sheet.dismiss();
                secondaryAction.run();
            });
            actions.addView(btnSecondary);
        }

        if (primaryLabel != null && primaryAction != null) {
            MaterialButton btnPrimary = createSheetActionButton(primaryLabel, R.color.app_primary, R.color.app_on_primary);
            btnPrimary.setOnClickListener(v -> {
                sheet.dismiss();
                primaryAction.run();
            });
            actions.addView(btnPrimary);
        }
        root.addView(actions);

        sheet.setContentView(root);
        sheet.show();
    }

    private MaterialButton createSheetActionButton(String text, int bgColorRes, int textColorRes) {
        MaterialButton btn = new MaterialButton(requireContext());
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setInsetTop(0);
        btn.setInsetBottom(0);
        btn.setCornerRadius(14);
        btn.setTextColor(ContextCompat.getColor(requireContext(), textColorRes));
        btn.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), bgColorRes)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(6, 0, 6, 0);
        btn.setLayoutParams(lp);
        return btn;
    }

    private void showLinkedMemoEditDialog(long memoId, String initialContent, String memoDate) {
        String dateStr = (memoDate != null && memoDate.length() >= 10)
                ? memoDate.substring(0, 10)
                : (currentDateStr != null ? currentDateStr : "-");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_memo_edit, null, false);
        TextView tvDate = dialogView.findViewById(R.id.textMemoEditDate);
        com.google.android.material.textfield.TextInputEditText editContent = dialogView.findViewById(R.id.editMemoEditContent);
        tvDate.setText("날짜: " + dateStr);
        if (initialContent != null) editContent.setText(initialContent);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("취소", (d, which) -> d.dismiss())
                .setNeutralButton("삭제", (d, which) -> showDeleteMemoDialog(memoId))
                .setPositiveButton("저장", (d, which) -> {
                    String newContent = editContent.getText() != null ? editContent.getText().toString().trim() : "";
                    if (newContent.isEmpty()) {
                        Toast.makeText(requireContext(), "내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CalendarMemoUpdateRequest req = new CalendarMemoUpdateRequest(newContent, dateStr);
                    api.updateCalendarMemo(catId, memoId, req).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "메모 수정 완료", Toast.LENGTH_SHORT).show();
                                loadMonthMemoMarkers();
                                loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false);
                            } else {
                                Toast.makeText(requireContext(), "수정 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(requireContext(), "수정 실패: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .create();
        dialog.show();
        styleDialogWindow(dialog);
    }

    private void showAddHealthMemoDialog(CalendarEventItem item) {
        String memoDate = item.getDate() != null && item.getDate().length() >= 10
                ? item.getDate().substring(0, 10)
                : (currentDateStr != null ? currentDateStr : dateFmt.format(new Date()));

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_memo_edit, null, false);
        TextView tvTitle = dialogView.findViewById(R.id.textMemoEditTitle);
        TextView tvDate = dialogView.findViewById(R.id.textMemoEditDate);
        com.google.android.material.textfield.TextInputEditText editMemo = dialogView.findViewById(R.id.editMemoEditContent);
        tvTitle.setText("메모 추가");
        tvDate.setText("날짜: " + memoDate);

        Long htId = item.getHealthTypeId();

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("취소", (d, which) -> d.dismiss())
                .setPositiveButton("저장", (d, which) -> {
                    String content = editMemo.getText() != null ? editMemo.getText().toString().trim() : "";
                    if (content.isEmpty()) {
                        Toast.makeText(requireContext(), "메모 내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CalendarMemoCreateRequest req = new CalendarMemoCreateRequest(content, memoDate, htId);
                    api.addCalendarMemo(catId, req).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "메모 저장 완료", Toast.LENGTH_SHORT).show();
                                loadMonthMemoMarkers();
                                loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false);
                            } else {
                                Toast.makeText(requireContext(), "저장 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(requireContext(), "저장 실패: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .create();
        dialog.show();
        styleDialogWindow(dialog);
    }

    private void showMemoEditDialog(CalendarEventItem item) {
        if (item.getId() == null) return;
        if (isObesityMemo(item)) {
            Toast.makeText(requireContext(), "비만도 기록은 수정할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fixedDateStr = (item.getDate() != null && item.getDate().length() >= 10)
                ? item.getDate().substring(0, 10)
                : (currentDateStr != null ? currentDateStr : "-");

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_memo_edit, null, false);
        TextView tvDate = dialogView.findViewById(R.id.textMemoEditDate);
        com.google.android.material.textfield.TextInputEditText editContent = dialogView.findViewById(R.id.editMemoEditContent);
        tvDate.setText("날짜: " + fixedDateStr);
        editContent.setText(item.getSubtitle() != null ? item.getSubtitle() : "");

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setNegativeButton("취소", (d, which) -> d.dismiss())
                .setNeutralButton("삭제", (d, which) -> showDeleteMemoDialog(item.getId()))
                .setPositiveButton("저장", (d, which) -> {
                    String newContent = editContent.getText() != null ? editContent.getText().toString().trim() : "";
                    if (newContent.isEmpty()) {
                        Toast.makeText(requireContext(), "내용을 입력해 주세요.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    CalendarMemoUpdateRequest req = new CalendarMemoUpdateRequest(newContent);

                    api.updateCalendarMemo(catId, item.getId(), req).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "메모 수정 완료", Toast.LENGTH_SHORT).show();
                                loadMonthMemoMarkers();
                                loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false);
                            } else {
                                Toast.makeText(requireContext(), "수정 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Toast.makeText(requireContext(), "수정 실패: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .create();
        dialog.show();
        styleDialogWindow(dialog);
    }

    private boolean isObesityMemo(CalendarEventItem item) {
        if (item == null) return false;
        if (!"MEMO".equals(item.getType())) return false;
        String subtitle = item.getSubtitle();
        return subtitle != null && subtitle.startsWith("비만도 |");
    }

    private void showDeleteMemoDialog(Long memoId) {
        if (memoId == null || api == null || catId <= 0) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("메모 삭제")
                .setMessage("이 메모를 삭제할까요?")
                .setNegativeButton("취소", (d, w) -> d.dismiss())
                .setPositiveButton("삭제", (d, w) -> api.deleteCalendarMemo(catId, memoId).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "삭제 완료", Toast.LENGTH_SHORT).show();
                            loadMonthMemoMarkers();
                            loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false);
                        } else {
                            Toast.makeText(requireContext(), "삭제 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(requireContext(), "삭제 실패: 네트워크 오류", Toast.LENGTH_SHORT).show();
                    }
                }))
                .show();
    }

    private void showDeleteVomitDialog(Long vomitId) {
        if (vomitId == null || api == null || catId <= 0) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("토 분석 삭제")
                .setMessage("이 토 분석 기록을 삭제할까요?")
                .setNegativeButton("취소", (d, w) -> d.dismiss())
                .setPositiveButton("삭제", (d, w) -> api.deleteVomitRecord(catId, vomitId).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "삭제 완료", Toast.LENGTH_SHORT).show();
                            loadMonthMemoMarkers();
                            loadEventsForDate(api, catId, currentDateStr, textCalendarEmpty, false);
                        } else {
                            Toast.makeText(requireContext(), "삭제 실패 (HTTP " + response.code() + ")", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Toast.makeText(requireContext(), "삭제 실패: 네트워크 오류", Toast.LENGTH_SHORT).show();
                    }
                }))
                .show();
    }
}