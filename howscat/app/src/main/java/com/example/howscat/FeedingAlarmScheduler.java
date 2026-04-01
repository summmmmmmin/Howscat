package com.example.howscat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Calendar;

public class FeedingAlarmScheduler {

    private static final String PREF_NAME = "feeding_alarm";
    private static final String KEY_MORNING_ENABLED = "morning_enabled";
    private static final String KEY_MORNING_HOUR = "morning_hour";
    private static final String KEY_MORNING_MINUTE = "morning_minute";
    private static final String KEY_EVENING_ENABLED = "evening_enabled";
    private static final String KEY_EVENING_HOUR = "evening_hour";
    private static final String KEY_EVENING_MINUTE = "evening_minute";

    public static void saveMorning(Context ctx, boolean enabled, int hour, int minute) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_MORNING_ENABLED, enabled)
                .putInt(KEY_MORNING_HOUR, hour)
                .putInt(KEY_MORNING_MINUTE, minute)
                .apply();
    }

    public static void saveEvening(Context ctx, boolean enabled, int hour, int minute) {
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_EVENING_ENABLED, enabled)
                .putInt(KEY_EVENING_HOUR, hour)
                .putInt(KEY_EVENING_MINUTE, minute)
                .apply();
    }

    public static boolean isMorningEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_MORNING_ENABLED, false);
    }
    public static int getMorningHour(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_MORNING_HOUR, 8);
    }
    public static int getMorningMinute(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_MORNING_MINUTE, 0);
    }
    public static boolean isEveningEnabled(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_EVENING_ENABLED, false);
    }
    public static int getEveningHour(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_EVENING_HOUR, 18);
    }
    public static int getEveningMinute(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_EVENING_MINUTE, 0);
    }

    public static void scheduleAlarms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        scheduleOrCancel(context,
                FeedingAlarmReceiver.ACTION_MORNING, 2001,
                prefs.getBoolean(KEY_MORNING_ENABLED, false),
                prefs.getInt(KEY_MORNING_HOUR, 8),
                prefs.getInt(KEY_MORNING_MINUTE, 0));
        scheduleOrCancel(context,
                FeedingAlarmReceiver.ACTION_EVENING, 2002,
                prefs.getBoolean(KEY_EVENING_ENABLED, false),
                prefs.getInt(KEY_EVENING_HOUR, 18),
                prefs.getInt(KEY_EVENING_MINUTE, 0));
    }

    private static void scheduleOrCancel(Context context, String action, int requestCode,
                                          boolean enabled, int hour, int minute) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent intent = new Intent(context, FeedingAlarmReceiver.class);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        if (!enabled) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pi);
    }

    public static void cancelAll(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        cancelOne(context, am, FeedingAlarmReceiver.ACTION_MORNING, 2001);
        cancelOne(context, am, FeedingAlarmReceiver.ACTION_EVENING, 2002);
    }

    private static void cancelOne(Context context, AlarmManager am, String action, int requestCode) {
        Intent intent = new Intent(context, FeedingAlarmReceiver.class);
        intent.setAction(action);
        PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
    }
}
