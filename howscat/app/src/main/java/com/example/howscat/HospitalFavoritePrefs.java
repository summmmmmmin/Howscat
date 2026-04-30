package com.example.howscat;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 카카오 장소 id 기준 찜 목록 (로컬 전용).
 */
public final class HospitalFavoritePrefs {

    private static final String PREF = "hospital_favorites_v1";
    private static final String KEY_IDS = "kakao_place_ids";

    private HospitalFavoritePrefs() {
    }

    private static SharedPreferences prefs(Context c) {
        String userId = c.getApplicationContext()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("loginId", "guest");
        return c.getApplicationContext().getSharedPreferences(PREF + "_" + userId, Context.MODE_PRIVATE);
    }

    public static boolean isFavorite(Context c, String kakaoPlaceId) {
        if (kakaoPlaceId == null || kakaoPlaceId.isEmpty()) return false;
        return loadSet(c).contains(kakaoPlaceId);
    }

    public static void setFavorite(Context c, String kakaoPlaceId, boolean on) {
        if (kakaoPlaceId == null || kakaoPlaceId.isEmpty()) return;
        Set<String> s = new HashSet<>(loadSet(c));
        if (on) {
            s.add(kakaoPlaceId);
        } else {
            s.remove(kakaoPlaceId);
        }
        saveSet(c, s);
    }

    private static Set<String> loadSet(Context c) {
        String csv = prefs(c).getString(KEY_IDS, "");
        if (csv == null || csv.isEmpty()) {
            return new HashSet<>();
        }
        Set<String> out = new HashSet<>();
        for (String p : csv.split(",")) {
            if (p != null && !p.isEmpty()) {
                out.add(p);
            }
        }
        return out;
    }

    private static void saveSet(Context c, Set<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (String id : ids) {
            if (id == null || id.contains(",")) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(id);
        }
        prefs(c).edit().putString(KEY_IDS, sb.toString()).apply();
    }

    public static void clearForCurrentUser(Context c) {
        prefs(c).edit().clear().apply();
    }

    /** 디버그용 */
    public static Set<String> snapshot(Context c) {
        return Collections.unmodifiableSet(loadSet(c));
    }
}
