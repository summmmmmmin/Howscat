package com.example.howscat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.example.howscat.dto.HealthScheduleItem;
import com.example.howscat.network.ApiService;
import com.example.howscat.network.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HealthScheduleAlarmScheduler {

    private static final String DATE_PATTERN = "yyyy-MM-dd";
    private static final int[] OFFSETS_DAYS = new int[]{7, 1, 0};

    private static String prefKey(long catId) {
        return "health_schedule_alarm_schedule_ids_cat_" + catId;
    }

    public static void syncAlarms(Context context, Long catId) {
        if (context == null || catId == null || catId <= 0) return;

        ApiService api = RetrofitClient.getApiService(context);
        api.getHealthSchedules(catId).enqueue(new Callback<List<HealthScheduleItem>>() {
            @Override
            public void onResponse(Call<List<HealthScheduleItem>> call, Response<List<HealthScheduleItem>> response) {
                if (!response.isSuccessful() || response.body() == null) return;

                List<HealthScheduleItem> schedules = response.body();

                // 이 고양이의 기존 예약 중 서버에 없는 것만 취소
                cancelAlarmsNotInNewList(context, catId, schedules);

                for (HealthScheduleItem item : schedules) {
                    if (item == null || item.getHealthScheduleId() == null) continue;

                    boolean enabled = item.getAlarmEnabled() != null && item.getAlarmEnabled();
                    long scheduleId = item.getHealthScheduleId();

                    if (!enabled) {
                        cancelAlarm(context, scheduleId);
                        continue;
                    }

                    scheduleAlarm(context, catId, item);
                }
            }

            @Override
            public void onFailure(Call<List<HealthScheduleItem>> call, Throwable t) {
                // 네트워크 실패면 알람 동기화는 스킵
            }
        });
    }

    private static void scheduleAlarm(Context context, long catId, HealthScheduleItem item) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Long scheduleIdObj = item.getHealthScheduleId();
        if (scheduleIdObj == null) return;
        long scheduleId = scheduleIdObj;

        String nextDateStr = item.getNextDate();
        if (nextDateStr == null) return;

        Date parsedDate;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
            sdf.setLenient(false);
            parsedDate = sdf.parse(nextDateStr);
        } catch (Exception e) {
            return;
        }

        int effectiveCycleMonths = item.getEffectiveCycleMonth() != null ? item.getEffectiveCycleMonth() : 12;

        for (int offsetDay : OFFSETS_DAYS) {
            Calendar triggerCal = Calendar.getInstance();
            triggerCal.setTime(parsedDate);
            triggerCal.set(Calendar.HOUR_OF_DAY, 9);
            triggerCal.set(Calendar.MINUTE, 0);
            triggerCal.set(Calendar.SECOND, 0);
            triggerCal.set(Calendar.MILLISECOND, 0);
            triggerCal.add(Calendar.DAY_OF_MONTH, -offsetDay);

            long nowMillis = System.currentTimeMillis();
            int guard = 0;
            while (triggerCal.getTimeInMillis() <= nowMillis && guard < 3 && effectiveCycleMonths > 0) {
                triggerCal.add(Calendar.MONTH, effectiveCycleMonths);
                guard++;
            }
            if (triggerCal.getTimeInMillis() <= nowMillis) continue;

            Intent intent = new Intent(context, HealthScheduleAlarmReceiver.class);
            intent.setAction(HealthScheduleAlarmReceiver.ACTION_ALARM);
            intent.putExtra("scheduleId", scheduleId);
            intent.putExtra("dayOffset", offsetDay);
            intent.putExtra("title", buildTitle(item));
            intent.putExtra("content", buildContent(nextDateStr, offsetDay));

            int requestCode = buildRequestCode(scheduleId, offsetDay);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerCal.getTimeInMillis(), pi);
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerCal.getTimeInMillis(), pi);
            }
        }

        saveLastScheduleId(context, catId, scheduleId);
    }

    private static String buildContent(String nextDateStr, int offsetDay) {
        if (offsetDay == 7) return "D-7 알림 · 일정일: " + nextDateStr;
        if (offsetDay == 1) return "D-1 알림 · 일정일: " + nextDateStr;
        return "오늘 일정입니다 · " + nextDateStr;
    }

    private static int buildRequestCode(long scheduleId, int offsetDay) {
        long seed = (scheduleId * 10L) + offsetDay;
        return (int) (Math.abs(seed) % Integer.MAX_VALUE);
    }

    private static String buildTitle(HealthScheduleItem item) {
        String catName = item.getCatName() != null ? item.getCatName() : "고양이";
        String typeName = item.getHealthTypeName() != null ? item.getHealthTypeName() : "건강 일정";
        return catName + " " + typeName;
    }

    private static void cancelAlarm(Context context, long scheduleId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, HealthScheduleAlarmReceiver.class);
        intent.setAction(HealthScheduleAlarmReceiver.ACTION_ALARM);
        intent.putExtra("scheduleId", scheduleId);

        for (int offsetDay : OFFSETS_DAYS) {
            intent.putExtra("dayOffset", offsetDay);
            int requestCode = buildRequestCode(scheduleId, offsetDay);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            am.cancel(pi);
        }
    }

    private static void cancelAlarmsNotInNewList(Context context, long catId,
                                                   List<HealthScheduleItem> newSchedules) {
        Set<Long> newIds = new HashSet<>();
        if (newSchedules != null) {
            for (HealthScheduleItem s : newSchedules) {
                if (s != null && s.getHealthScheduleId() != null) {
                    newIds.add(s.getHealthScheduleId());
                }
            }
        }

        SharedPreferences prefs = context.getSharedPreferences("health_schedule_alarm", Context.MODE_PRIVATE);
        String csv = prefs.getString(prefKey(catId), "");
        if (csv == null || csv.trim().isEmpty()) {
            saveIds(context, catId, newIds);
            return;
        }

        String[] parts = csv.split(",");
        for (String p : parts) {
            try {
                long oldId = Long.parseLong(p.trim());
                if (!newIds.contains(oldId)) {
                    cancelAlarm(context, oldId);
                }
            } catch (Exception ignored) {
            }
        }

        saveIds(context, catId, newIds);
    }

    private static void saveLastScheduleId(Context context, long catId, long scheduleId) {
        SharedPreferences prefs = context.getSharedPreferences("health_schedule_alarm", Context.MODE_PRIVATE);
        String csv = prefs.getString(prefKey(catId), "");
        Set<Long> set = new HashSet<>();
        if (csv != null && !csv.trim().isEmpty()) {
            for (String p : csv.split(",")) {
                try { set.add(Long.parseLong(p.trim())); } catch (Exception ignored) {}
            }
        }
        set.add(scheduleId);
        saveIds(context, catId, set);
    }

    private static void saveIds(Context context, long catId, Set<Long> ids) {
        if (ids == null) ids = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Long id : ids) {
            if (!first) sb.append(",");
            sb.append(id);
            first = false;
        }
        context.getSharedPreferences("health_schedule_alarm", Context.MODE_PRIVATE)
                .edit().putString(prefKey(catId), sb.toString()).apply();
    }
}

