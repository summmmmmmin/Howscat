package com.example.howscat.network;

import android.content.Context;

import com.example.howscat.BuildConfig;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class RetrofitClient {

    private static final String BASE_URL = BuildConfig.API_BASE_URL;

    private static volatile Retrofit retrofit;
    private static volatile ApiService apiService;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            synchronized (RetrofitClient.class) {
                if (retrofit == null) {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .addInterceptor(new AuthInterceptor(context.getApplicationContext()))
                            .authenticator(new AuthAuthenticator(context.getApplicationContext()))
                            .build();

                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create(
                                    new GsonBuilder().serializeNulls().create()
                            ))
                            .build();
                }
            }
        }
        return retrofit;
    }

    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            synchronized (RetrofitClient.class) {
                if (apiService == null) {
                    apiService = getClient(context).create(ApiService.class);
                }
            }
        }
        return apiService;
    }

    /**
     * 로그아웃 시 호출 — 다음 요청에서 새 토큰으로 클라이언트를 재생성합니다.
     */
    public static synchronized void reset() {
        retrofit = null;
        apiService = null;
    }
}
