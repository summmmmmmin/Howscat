package com.example.howscat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * 기기 재부팅 후 알람을 재등록합니다.
 * AlarmManager 알람은 기기 재부팅 시 사라지므로 BOOT_COMPLETED 시 다시 설정해야 합니다.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String PREF_ALARM_CATS = "alarm_cat_ids";
    private static final String KEY_CAT_IDS = "all_alarm_cat_ids";

    /** 알람을 등록할 때 catId를 영속 저장소에 추가합니다. */
    public static void registerCatId(Context context, long catId) {
        if (catId <= 0) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_ALARM_CATS, Context.MODE_PRIVATE);
        Set<String> ids = new HashSet<>(prefs.getStringSet(KEY_CAT_IDS, new HashSet<>()));
        ids.add(String.valueOf(catId));
        prefs.edit().putStringSet(KEY_CAT_IDS, ids).apply();
    }

    /** 알람을 모두 취소할 때 catId를 제거합니다. */
    public static void unregisterCatId(Context context, long catId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_ALARM_CATS, Context.MODE_PRIVATE);
        Set<String> ids = new HashSet<>(prefs.getStringSet(KEY_CAT_IDS, new HashSet<>()));
        ids.remove(String.valueOf(catId));
        prefs.edit().putStringSet(KEY_CAT_IDS, ids).apply();
    }

    private static Set<Long> getAllCatIds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_ALARM_CATS, Context.MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet(KEY_CAT_IDS, new HashSet<>());
        Set<Long> result = new HashSet<>();
        for (String s : raw) {
            try { result.add(Long.parseLong(s)); } catch (Exception ignored) {}
        }
        return result;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
                !"android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) return;
        if (context == null) return;

        // 급여 알람 재등록 (고양이 무관)
        FeedingAlarmScheduler.scheduleAllAlarms(context);

        // 알람이 등록된 모든 고양이에 대해 투약·건강검진 알람 복구
        Set<Long> catIds = getAllCatIds(context);

        // 마지막 조회 고양이도 포함 (알람 등록 이력이 없어도)
        long lastCatId = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getLong("lastViewedCatId", -1L);
        if (lastCatId > 0) catIds.add(lastCatId);

        for (long catId : catIds) {
            MedicationAlarmScheduler.syncAlarms(context, catId);
            HealthScheduleAlarmScheduler.syncAlarms(context, catId);
        }
    }
}
