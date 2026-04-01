package com.example.howscat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class MedicationAlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "medication_alarm_channel";
    public static final String ACTION_ALARM = "com.example.howscat.ACTION_MEDICATION_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        long medicationId = intent.getLongExtra("medicationId", -1L);
        String medName = intent.getStringExtra("medName");
        String catName = intent.getStringExtra("catName");
        if (medicationId < 0) return;

        Intent open = new Intent(context, HomeActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, (int) (medicationId % Integer.MAX_VALUE), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "투약 알림", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("약 복용 시간 알림");
            nm.createNotificationChannel(ch);
        }

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        String catPrefix = (catName != null && !catName.isEmpty()) ? catName + " · " : "";
        String title = catPrefix + "투약 시간이에요!";
        String body = (medName != null && !medName.isEmpty() ? medName : "약") + " 복용 시간입니다.";

        builder.setSmallIcon(R.drawable.ic_vaccine_syringe)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setContentIntent(contentIntent);

        nm.notify((int) (medicationId % Integer.MAX_VALUE), builder.build());
    }
}
