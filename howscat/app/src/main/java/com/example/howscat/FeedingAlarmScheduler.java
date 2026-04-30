package com.example.howscat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FeedingAlarmScheduler {

    private static final String PREF_NAME = "feeding_alarm";
    private static final String KEY_ALARMS_JSON = "alarms_json";
    private static final String KEY_NEXT_ID = "next_id";

    public static class FeedingAlarm {
        public int id;
        public String title;
        public int hour;
        public int minute;
        public String memo;

        public FeedingAlarm(int id, String title, int hour, int minute, String memo) {
            this.id = id;
            this.title = title;
            this.hour = hour;
            this.minute = minute;
            this.memo = memo;
        }
    }

    public static List<FeedingAlarm> getAlarms(Context ctx) {
        List<FeedingAlarm> list = new ArrayList<>();
        String json = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ALARMS_JSON, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new FeedingAlarm(
                        obj.getInt("id"),
                        obj.optString("title", "급여 알림"),
                        obj.getInt("hour"),
                        obj.getInt("minute"),
                        obj.optString("memo", "")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private static void saveAlarms(Context ctx, List<FeedingAlarm> alarms) {
        try {
            JSONArray arr = new JSONArray();
            for (FeedingAlarm a : alarms) {
                JSONObject obj = new JSONObject();
                obj.put("id", a.id);
                obj.put("title", a.title != null ? a.title : "급여 알림");
                obj.put("hour", a.hour);
                obj.put("minute", a.minute);
                obj.put("memo", a.memo != null ? a.memo : "");
                arr.put(obj);
            }
            ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit().putString(KEY_ALARMS_JSON, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static FeedingAlarm addAlarm(Context ctx, String title, int hour, int minute, String memo) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        int nextId = prefs.getInt(KEY_NEXT_ID, 1);
        prefs.edit().putInt(KEY_NEXT_ID, nextId + 1).apply();

        FeedingAlarm alarm = new FeedingAlarm(nextId, title, hour, minute, memo);
        List<FeedingAlarm> alarms = getAlarms(ctx);
        alarms.add(alarm);
        saveAlarms(ctx, alarms);
        return alarm;
    }

    public static void updateAlarm(Context ctx, int id, String title, int hour, int minute, String memo) {
        List<FeedingAlarm> alarms = getAlarms(ctx);
        for (FeedingAlarm a : alarms) {
            if (a.id == id) {
                a.title = title;
                a.hour = hour;
                a.minute = minute;
                a.memo = memo;
                break;
            }
        }
        saveAlarms(ctx, alarms);
    }

    public static void deleteAlarm(Context ctx, int id) {
        List<FeedingAlarm> alarms = getAlarms(ctx);
        alarms.removeIf(a -> a.id == id);
        saveAlarms(ctx, alarms);
        cancelAlarm(ctx, id);
    }

    public static void scheduleAllAlarms(Context ctx) {
        List<FeedingAlarm> alarms = getAlarms(ctx);
        for (FeedingAlarm alarm : alarms) {
            scheduleAlarm(ctx, alarm);
        }
    }

    public static void scheduleAlarm(Context ctx, FeedingAlarm alarm) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(ctx, FeedingAlarmReceiver.class);
        intent.setAction(FeedingAlarmReceiver.ACTION_FEEDING);
        intent.putExtra("alarmId", alarm.id);
        intent.putExtra("alarmHour", alarm.hour);
        intent.putExtra("alarmMinute", alarm.minute);
        intent.putExtra("title", alarm.title != null ? alarm.title : "급여 알림");
        intent.putExtra("memo", alarm.memo != null ? alarm.memo : "");

        int requestCode = 3000 + alarm.id;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.hour);
        cal.set(Calendar.MINUTE, alarm.minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        // 정확한 시각 1회 예약 — FeedingAlarmReceiver에서 다음 날치를 재예약
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }

    public static void cancelAlarm(Context ctx, int id) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(ctx, FeedingAlarmReceiver.class);
        intent.setAction(FeedingAlarmReceiver.ACTION_FEEDING);
        int requestCode = 3000 + id;
        PendingIntent pi = PendingIntent.getBroadcast(ctx, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }

    public static void cancelAll(Context ctx) {
        for (FeedingAlarm a : getAlarms(ctx)) {
            cancelAlarm(ctx, a.id);
        }
    }
}
