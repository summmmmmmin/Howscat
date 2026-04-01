package com.example.howscat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class FeedingAlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "feeding_alarm_channel";
    public static final String ACTION_MORNING = "com.example.howscat.ACTION_FEEDING_MORNING";
    public static final String ACTION_EVENING = "com.example.howscat.ACTION_FEEDING_EVENING";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        String action = intent.getAction();
        boolean isMorning = ACTION_MORNING.equals(action);

        Intent open = new Intent(context, HomeActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra("from", "feeding_alarm");
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, isMorning ? 2001 : 2002, open,
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
        String catPrefix = (catName != null && !catName.isEmpty()) ? catName + " · " : "";
        String catBody = (catName != null && !catName.isEmpty()) ? catName + "가 기다리고 있어요." : "우리 고양이가 기다리고 있어요.";
        builder.setSmallIcon(R.drawable.ic_heart_filled)
                .setContentTitle(catPrefix + (isMorning ? "아침 밥 줄 시간이에요!" : "저녁 밥 줄 시간이에요!"))
                .setContentText(catBody)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(contentIntent);

        nm.notify(isMorning ? 2001 : 2002, builder.build());
    }
}
