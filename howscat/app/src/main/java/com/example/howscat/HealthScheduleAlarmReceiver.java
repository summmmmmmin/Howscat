package com.example.howscat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.Nullable;

import java.util.Objects;

public class HealthScheduleAlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "health_schedule_channel";
    public static final String ACTION_ALARM = "com.example.howscat.ACTION_HEALTH_SCHEDULE_ALARM";
    public static final String ACTION_SNOOZE = "com.example.howscat.ACTION_HEALTH_SCHEDULE_SNOOZE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        String action = intent.getAction();
        long scheduleId = intent.getLongExtra("scheduleId", -1L);
        String title = intent.getStringExtra("title");
        String content = intent.getStringExtra("content");
        int dayOffset = intent.getIntExtra("dayOffset", 0);

        if (scheduleId < 0) return;
        if (ACTION_SNOOZE.equals(action)) {
            scheduleSnooze(context, scheduleId, title, content, dayOffset);
            return;
        }

        // Notification tap -> HomeActivity로
        Intent open = new Intent(context, HomeActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("from", "health_alarm");

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                (int) (scheduleId % Integer.MAX_VALUE),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "건강 스케줄 알림",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("건강검진/예방접종 일정 알림");
            nm.createNotificationChannel(channel);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        builder
                .setSmallIcon(R.drawable.ic_calendar)
                .setContentTitle(title != null ? title : "건강 일정")
                .setContentText(content != null ? content : "일정이 있습니다.")
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(contentIntent);
        builder.addAction(buildSnoozeAction(context, scheduleId, title, content, dayOffset));

        Notification notification = builder.build();
        nm.notify((int) (scheduleId % Integer.MAX_VALUE), notification);
    }

    private Notification.Action buildSnoozeAction(Context context, long scheduleId, @Nullable String title, @Nullable String content, int dayOffset) {
        Intent snoozeIntent = new Intent(context, HealthScheduleAlarmReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE);
        snoozeIntent.putExtra("scheduleId", scheduleId);
        snoozeIntent.putExtra("title", title);
        snoozeIntent.putExtra("content", content);
        snoozeIntent.putExtra("dayOffset", dayOffset);
        PendingIntent snoozePi = PendingIntent.getBroadcast(
                context,
                (int) ((scheduleId * 100L + dayOffset) % Integer.MAX_VALUE),
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        return new Notification.Action.Builder(0, "1시간 뒤 다시", snoozePi).build();
    }

    private void scheduleSnooze(Context context, long scheduleId, @Nullable String title, @Nullable String content, int dayOffset) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Intent next = new Intent(context, HealthScheduleAlarmReceiver.class);
        next.setAction(ACTION_ALARM);
        next.putExtra("scheduleId", scheduleId);
        next.putExtra("dayOffset", dayOffset);
        next.putExtra("title", title != null ? title : "건강 일정");
        next.putExtra("content", (content != null ? content : "일정 알림") + " (스누즈)");
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                (int) ((scheduleId * 1000L + dayOffset) % Integer.MAX_VALUE),
                next,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        long triggerAt = System.currentTimeMillis() + 60L * 60L * 1000L;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }
}

