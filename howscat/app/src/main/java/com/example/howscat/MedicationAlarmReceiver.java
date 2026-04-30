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

public class MedicationAlarmReceiver extends BroadcastReceiver {

    public static final String CHANNEL_ID = "medication_alarm_channel";
    public static final String ACTION_ALARM = "com.example.howscat.ACTION_MEDICATION_ALARM";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        long medicationId = intent.getLongExtra("medicationId", -1L);
        long catId        = intent.getLongExtra("catId", -1L);
        int  alarmHour    = intent.getIntExtra("alarmHour", -1);
        int  alarmMinute  = intent.getIntExtra("alarmMinute", 0);
        int  slot         = intent.getIntExtra("slot", 0);
        String endDate = intent.getStringExtra("endDate");
        String medName = intent.getStringExtra("medName");
        String catName = intent.getStringExtra("catName");
        if (medicationId < 0) return;

        // 복용 종료일이 지나지 않은 경우에만 다음 날 재예약
        if (catId > 0 && alarmHour >= 0 && !isPastEndDate(endDate)) {
            rescheduleNextDay(context, catId, medicationId, alarmHour, alarmMinute, slot, endDate, medName, catName);
        }

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

    /**
     * 알람이 울린 뒤 내일 동일 시각으로 재예약합니다.
     * setExactAndAllowWhileIdle은 1회성이기 때문에 반드시 재예약이 필요합니다.
     */
    /** 종료일이 오늘 이전이면 true (더 이상 복용 안 함) */
    private boolean isPastEndDate(String endDate) {
        if (endDate == null || endDate.isEmpty()) return false; // 종료일 없으면 계속 복용
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            String today = sdf.format(new java.util.Date());
            return today.compareTo(endDate) > 0; // 오늘이 종료일보다 크면 종료
        } catch (Exception e) {
            return false;
        }
    }

    private void rescheduleNextDay(Context context, long catId, long medicationId,
                                   int hour, int minute, int slot, String endDate,
                                   String medName, String catName) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent next = new Intent(context, MedicationAlarmReceiver.class);
        next.setAction(ACTION_ALARM);
        next.putExtra("medicationId", medicationId);
        next.putExtra("catId", catId);
        next.putExtra("alarmHour", hour);
        next.putExtra("alarmMinute", minute);
        next.putExtra("slot", slot);
        next.putExtra("endDate", endDate != null ? endDate : "");
        next.putExtra("medName", medName != null ? medName : "약");
        next.putExtra("catName", catName != null ? catName : "");

        // requestCode = catId*1_000_000 + medicationId*10 + slot (MedicationAlarmScheduler와 동일)
        long seed = catId * 1_000_000L + medicationId * 10L + slot;
        int requestCode = (int) (Math.abs(seed) % Integer.MAX_VALUE);

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
