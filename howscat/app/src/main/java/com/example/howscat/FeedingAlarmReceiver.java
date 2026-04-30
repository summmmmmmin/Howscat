package com.example.howscat;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public class FeedingAlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "feeding_alarm_channel";
    public static final String ACTION_FEEDING = "com.example.howscat.ACTION_FEEDING";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        int alarmId    = intent.getIntExtra("alarmId", 0);
        int alarmHour  = intent.getIntExtra("alarmHour", -1);
        int alarmMin   = intent.getIntExtra("alarmMinute", 0);
        String title   = intent.getStringExtra("title");
        String memo    = intent.getStringExtra("memo");

        // 내일 동일 시각에 재예약 (setExactAndAllowWhileIdle은 1회성)
        if (alarmHour >= 0) rescheduleNextDay(context, alarmId, alarmHour, alarmMin, title, memo);
        if (title == null || title.isEmpty()) title = "급여 알림";

        Intent open = new Intent(context, HomeActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("from", "feeding_alarm");
        int requestCode = 3000 + alarmId;
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, requestCode, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "급여 알림", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("밥 줄 시간 알림");
            nm.createNotificationChannel(ch);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        String catName = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("lastViewedCatName", "");
        String catBody = (catName != null && !catName.isEmpty())
                ? catName + "가 기다리고 있어요."
                : "우리 고양이가 기다리고 있어요.";
        if (memo != null && !memo.isEmpty()) {
            catBody = memo;
        }

        builder.setSmallIcon(R.drawable.ic_heart_filled)
                .setContentTitle(title)
                .setContentText(catBody)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(contentIntent);

        nm.notify(requestCode, builder.build());
    }

    private void rescheduleNextDay(Context context, int alarmId, int hour, int minute,
                                   String title, String memo) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent next = new Intent(context, FeedingAlarmReceiver.class);
        next.setAction(ACTION_FEEDING);
        next.putExtra("alarmId", alarmId);
        next.putExtra("alarmHour", hour);
        next.putExtra("alarmMinute", minute);
        next.putExtra("title", title != null ? title : "급여 알림");
        next.putExtra("memo", memo != null ? memo : "");

        int requestCode = 3000 + alarmId;
        PendingIntent pi = PendingIntent.getBroadcast(
                context, requestCode, next,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }
}
