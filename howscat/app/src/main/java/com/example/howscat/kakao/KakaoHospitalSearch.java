package com.example.howscat.kakao;

import android.content.Context;

import com.example.howscat.BuildConfig;
import com.example.howscat.HospitalFavoritePrefs;
import com.example.howscat.dto.HospitalNearbyResponse;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 카카오 로컬 API로 주변 동물병원을 검색합니다. (거리순 sort=distance)
 */
public final class KakaoHospitalSearch {

    private KakaoHospitalSearch() {
    }

    public static List<HospitalNearbyResponse> search(Context context, double lat, double lng) throws Exception {
        String key = BuildConfig.KAKAO_REST_API_KEY;
        if (key == null || key.isEmpty()) {
            return Collections.emptyList();
        }

        String q = "query=" + java.net.URLEncoder.encode("동물병원", StandardCharsets.UTF_8.name())
                + "&y=" + lat
                + "&x=" + lng
                + "&radius=20000"
                + "&sort=distance"
                + "&size=15";

        URL url = new URL("https://dapi.kakao.com/v2/local/search/keyword.json?" + q);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Authorization", "KakaoAK " + key);
        conn.setRequestMethod("GET");

        int code = conn.getResponseCode();
        InputStream in = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        conn.disconnect();

        if (code < 200 || code >= 300) {
            throw new IllegalStateException("Kakao HTTP " + code + ": " + sb);
        }

        KakaoLocalKeywordResponse parsed = new Gson().fromJson(sb.toString(), KakaoLocalKeywordResponse.class);
        if (parsed == null || parsed.getDocuments() == null) {
            return Collections.emptyList();
        }

        List<HospitalNearbyResponse> out = new ArrayList<>();
        for (KakaoLocalKeywordResponse.Document doc : parsed.getDocuments()) {
            if (doc == null || doc.getId() == null) continue;
            HospitalNearbyResponse row = new HospitalNearbyResponse();
            row.setKakaoPlaceId(doc.getId());
            row.setName(doc.getPlaceName() != null ? doc.getPlaceName() : "");
            String addr = doc.getRoadAddressName();
            if (addr == null || addr.isEmpty()) {
                addr = doc.getAddressName() != null ? doc.getAddressName() : "";
            }
            row.setAddress(addr);
            try {
                row.setLatitude(Double.parseDouble(doc.getY()));
                row.setLongitude(Double.parseDouble(doc.getX()));
            } catch (Exception ignored) {
                continue;
            }
            if (doc.getDistance() != null && !doc.getDistance().isEmpty()) {
                try {
                    row.setDistanceKm(Double.parseDouble(doc.getDistance()) / 1000.0);
                } catch (Exception ignored) {
                    row.setDistanceKm(null);
                }
            }
            row.setPhone(doc.getPhone());
            row.setPlaceUrl(doc.getPlaceUrl());
            row.setOpen24Hours(null);
            row.setOperating(null);
            row.setId(null);
            row.setFavorited(HospitalFavoritePrefs.isFavorite(context, doc.getId()));
            out.add(row);
        }
        return out;
    }
}
