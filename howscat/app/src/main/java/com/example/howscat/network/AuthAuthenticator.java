package com.example.howscat.network;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.example.howscat.BuildConfig;

import org.json.JSONObject;

import okhttp3.Authenticator;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Route;

public class AuthAuthenticator implements Authenticator {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final Context context;
    private static volatile boolean redirecting = false;

    public AuthAuthenticator(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Request authenticate(Route route, Response response) {
        if (response.request().url().encodedPath().contains("/api/users/refresh")) {
            return null;
        }
        if (responseCount(response) >= 2) {
            return null;
        }

        SharedPreferences prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        String refreshToken = prefs.getString("refreshToken", null);
        if (refreshToken == null || refreshToken.isEmpty()) {
            redirectToLogin(prefs);
            return null;
        }

        try {
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("refreshToken", refreshToken);
            RequestBody body = RequestBody.create(bodyJson.toString(), JSON);

            Request refreshRequest = new Request.Builder()
                    .url(BuildConfig.API_BASE_URL + "api/users/refresh")
                    .post(body)
                    .build();

            OkHttpClient client = new OkHttpClient.Builder().build();
            okhttp3.Response refreshResponse = client.newCall(refreshRequest).execute();
            if (!refreshResponse.isSuccessful() || refreshResponse.body() == null) {
                redirectToLogin(prefs);
                return null;
            }

            String responseText = refreshResponse.body().string();
            JSONObject json = new JSONObject(responseText);
            String newAccess = json.optString("accessToken", "");
            String newRefresh = json.optString("refreshToken", "");
            if (newAccess.isEmpty() || newRefresh.isEmpty()) {
                redirectToLogin(prefs);
                return null;
            }

            prefs.edit()
                    .putString("accessToken", newAccess)
                    .putString("refreshToken", newRefresh)
                    .apply();

            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + newAccess)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private void redirectToLogin(SharedPreferences prefs) {
        if (redirecting) return;
        redirecting = true;

        // 1. userId 스코핑된 케어 결과는 auth 삭제 전에 먼저 지워야 올바른 키를 읽을 수 있음
        com.example.howscat.CareResultPrefs.clear(context);

        // 2. AlarmManager PendingIntent 취소 (prefs 지우기 전에 ID를 읽어야 하므로 먼저 실행)
        com.example.howscat.FeedingAlarmScheduler.cancelAll(context);
        com.example.howscat.MedicationAlarmScheduler.cancelAll(context);
        com.example.howscat.HealthScheduleAlarmScheduler.cancelAll(context);

        // 3. 모든 SharedPreferences 초기화
        prefs.edit().clear().apply(); // auth
        context.getSharedPreferences("profile",               Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences("medication_alarm",      Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences("feeding_alarm",         Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences("health_schedule_alarm", Context.MODE_PRIVATE).edit().clear().apply();
        context.getSharedPreferences("alarm_cat_ids",         Context.MODE_PRIVATE).edit().clear().apply();

        RetrofitClient.reset();
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Class<?> loginClass = Class.forName(context.getPackageName() + ".LoginActivity");
                Intent intent = new Intent(context, loginClass);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                context.startActivity(intent);
            } catch (ClassNotFoundException ignored) {
            } finally {
                redirecting = false;
            }
        });
    }

    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }
}
