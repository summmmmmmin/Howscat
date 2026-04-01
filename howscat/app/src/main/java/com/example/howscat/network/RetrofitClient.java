package com.example.howscat.network;

import android.content.Context;

import com.example.howscat.BuildConfig;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // NOTE: BuildConfig(API_BASE_URL) 사용 시 빌드 설정 의존 문제가 있어,
    // BuildConfig가 생성되므로 여기서는 빌드 설정 값을 사용합니다.
    private static final String BASE_URL = BuildConfig.API_BASE_URL;

    public static Retrofit getClient(Context context) {

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(context))
                .authenticator(new AuthAuthenticator(context))
                .build();

        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(
                        new GsonBuilder().serializeNulls().create()
                ))
                .build();
    }

    public static ApiService getApiService(Context context) {
        return getClient(context).create(ApiService.class);
    }
}
