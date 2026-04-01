package com.example.howscat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.howscat.dto.MedicationItem;
import com.example.howscat.network.ApiService;
import com.example.howscat.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 투약 알람 동기화:
 * - 고양이별(catId) 투약 목록을 API로 조회
 * - alarm_enabled=true + 현재 복용 기간 내 항목에 대해 일일 반복 알람 등록
 * - AS_NEEDED는 알람 제외, TWICE_DAILY는 12시간 간격 2회 등록
 */
public class MedicationAlarmScheduler {

    private static final String PREF_KEY_PREFIX = "medication_alarm_ids_cat_";

    public static void syncAlarms(Context context, Long catId) {
        if (context == null || catId == null || catId <= 0) return;

        String catName = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("lastViewedCatName", "");

        ApiService api = RetrofitClient.getApiService(context);
        api.getMedications(catId).enqueue(new Callback<List<MedicationItem>>() {
            @Override
            public void onResponse(Call<List<MedicationItem>> call, Response<List<MedicationItem>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                List<MedicationItem> medications = response.body();
                cancelAlarmsNotInList(context, catId, medications);
                for (MedicationItem med : medications) {
                    if (med == null || med.getMedicationId() == null) continue;
                    if (!Boolean.TRUE.equals(med.getAlarmEnabled())) {
                        cancelAlarmForMedication(context, catId, med.getMedicationId());
                        continue;
                    }
                    if (!isWithinPeriod(med)) {
                        cancelAlarmForMedication(context, catId, med.getMedicationId());
                        continue;
                    }
                    scheduleAlarm(context, catId, catName, med);
                }
            }

            @Override
            public void onFailure(Call<List<MedicationItem>> call, Throwable t) {
                // 네트워크 실패 시 기존 알람 유지
            }
        });
    }

    private static boolean isWithinPeriod(MedicationItem med) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String today = sdf.format(new java.util.Date());
            if (med.getStartDate() != null && today.compareTo(med.getStartDate()) < 0) return false;
            if (med.getEndDate() != null && !med.getEndDate().isEmpty()
                    && today.compareTo(med.getEndDate()) > 0) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void scheduleAlarm(Context context, Long catId, String catName, MedicationItem med) {
        long medicationId = med.getMedicationId();
        int hour = med.getAlarmHour() != null ? med.getAlarmHour() : 9;
        int minute = med.getAlarmMinute() != null ? med.getAlarmMinute() : 0;
        String freq = med.getFrequency();

        if ("AS_NEEDED".equals(freq)) return; // 필요시 복용은 자동 알람 없음

        scheduleSlot(context, catId, catName, medicationId, hour, minute, 0, med.getName());

        if ("TWICE_DAILY".equals(freq)) {
            scheduleSlot(context, catId, catName, medicationId, (hour + 12) % 24, minute, 1, med.getName());
        }

        saveAlarmId(context, catId, medicationId);
    }

    private static void scheduleSlot(Context context, Long catId, String catName,
                                      long medicationId, int hour, int minute, int slot, String name) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, MedicationAlarmReceiver.class);
        intent.setAction(MedicationAlarmReceiver.ACTION_ALARM);
        intent.putExtra("medicationId", medicationId);
        intent.putExtra("catId", (long) catId);
        intent.putExtra("medName", name != null ? name : "약");
        intent.putExtra("catName", catName != null ? catName : "");

        int requestCode = buildRequestCode(catId, medicationId, slot);
        PendingIntent pi = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                cal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pi);
    }

    public static void cancelAlarmForMedication(Context context, Long catId, Long medicationId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int slot = 0; slot <= 1; slot++) {
            Intent intent = new Intent(context, MedicationAlarmReceiver.class);
            intent.setAction(MedicationAlarmReceiver.ACTION_ALARM);
            int requestCode = buildRequestCode(catId, medicationId, slot);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.cancel(pi);
        }
    }

    private static void cancelAlarmsNotInList(Context context, Long catId, List<MedicationItem> newList) {
        Set<Long> newIds = new HashSet<>();
        if (newList != null) {
            for (MedicationItem m : newList) {
                if (m != null && m.getMedicationId() != null) newIds.add(m.getMedicationId());
            }
        }
        SharedPreferences prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String csv = prefs.getString(PREF_KEY_PREFIX + catId, "");
        for (Long oldId : parseIds(csv)) {
            if (!newIds.contains(oldId)) cancelAlarmForMedication(context, catId, oldId);
        }
        saveAlarmIds(context, catId, newIds);
    }

    private static void saveAlarmId(Context context, Long catId, Long medId) {
        SharedPreferences prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        Set<Long> set = parseIds(prefs.getString(PREF_KEY_PREFIX + catId, ""));
        set.add(medId);
        saveAlarmIds(context, catId, set);
    }

    private static void saveAlarmIds(Context context, Long catId, Set<Long> ids) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Long id : ids) {
            if (!first) sb.append(",");
            sb.append(id);
            first = false;
        }
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                .edit().putString(PREF_KEY_PREFIX + catId, sb.toString()).apply();
    }

    private static Set<Long> parseIds(String csv) {
        Set<Long> set = new HashSet<>();
        if (csv == null || csv.trim().isEmpty()) return set;
        for (String p : csv.split(",")) {
            try { set.add(Long.parseLong(p.trim())); } catch (Exception ignored) {}
        }
        return set;
    }

    private static int buildRequestCode(Long catId, long medicationId, int slot) {
        long seed = catId * 1_000_000L + medicationId * 10L + slot;
        return (int) (Math.abs(seed) % Integer.MAX_VALUE);
    }
}
