package com.example.howscat;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 홈 화면 지급량 요약 · 비만도 다이얼로그 몸무게 필드 표시 여부
 * 모든 키는 catId 단위로 분리되어 고양이 전환 시 데이터가 유지됩니다.
 */
public final class CareResultPrefs {

    private static final String PREF = "care_result";

    private CareResultPrefs() {
    }

    private static SharedPreferences prefs(Context ctx) {
        String userId = ctx.getApplicationContext()
                .getSharedPreferences("auth", Context.MODE_PRIVATE)
                .getString("loginId", "guest");
        return ctx.getSharedPreferences(PREF + "_" + userId, Context.MODE_PRIVATE);
    }

    public static void saveWaterFood(Context ctx, long catId, double waterMl, double foodG, double weightKg) {
        prefs(ctx).edit()
                .putFloat("water_ml_" + catId, (float) waterMl)
                .putFloat("food_g_" + catId, (float) foodG)
                .putFloat("weight_kg_" + catId, (float) weightKg)
                .putString("source_" + catId, "water_food")
                .putBoolean("has_water_food_" + catId, true)
                .putBoolean("has_weight_from_water_" + catId, true)
                .apply();
    }

    public static void saveObesity(Context ctx, long catId, double waterMl, double foodG, String level) {
        prefs(ctx).edit()
                .putFloat("water_ml_" + catId, (float) waterMl)
                .putFloat("food_g_" + catId, (float) foodG)
                .putString("source_" + catId, "obesity")
                .putString("obesity_level_" + catId, level != null ? level : "")
                .putBoolean("has_water_food_" + catId, true)
                .putBoolean("has_weight_from_water_" + catId, false)
                .apply();
    }

    public static boolean hasSummaryForCat(Context ctx, long catId) {
        return prefs(ctx)
                .getBoolean("has_water_food_" + catId, false);
    }

    public static String getSummaryText(Context ctx, long catId) {
        SharedPreferences p = prefs(ctx);
        if (!p.getBoolean("has_water_food_" + catId, false)) return "";
        float w = p.getFloat("water_ml_" + catId, 0);
        float f = p.getFloat("food_g_" + catId, 0);
        return String.format(java.util.Locale.getDefault(),
                "적정 물 지급량 | %.0f mL\n적정 사료 지급량 | %.0f g/일", w, f);
    }

    public static boolean hasWeightFromWaterFood(Context ctx, long catId) {
        return prefs(ctx)
                .getBoolean("has_weight_from_water_" + catId, false);
    }

    public static float getLastWeightKgForCat(Context ctx, long catId) {
        return prefs(ctx)
                .getFloat("weight_kg_" + catId, 0f);
    }

    /** 해당 고양이의 케어 결과만 초기화 */
    public static void clearForCat(Context ctx, long catId) {
        prefs(ctx).edit()
                .remove("water_ml_" + catId)
                .remove("food_g_" + catId)
                .remove("weight_kg_" + catId)
                .remove("source_" + catId)
                .remove("obesity_level_" + catId)
                .remove("has_water_food_" + catId)
                .remove("has_weight_from_water_" + catId)
                .apply();
    }

    /** 모든 고양이의 케어 결과 전체 초기화 (새 계정 등록 시 사용) */
    public static void clear(Context ctx) {
        prefs(ctx).edit().clear().apply();
    }

    /** 서버에서 오는 영문 레벨을 짧은 한글로 */
    public static String obesityLevelLabel(String level) {
        if (level == null || level.isEmpty()) return "";
        switch (level) {
            case "UNDERWEIGHT":         return "저체중";
            case "SLIGHTLY_UNDERWEIGHT":return "약간 저체중";
            case "NORMAL":              return "정상";
            case "SLIGHTLY_OVERWEIGHT": return "약간 과체중";
            case "OVERWEIGHT":          return "과체중";
            default:                    return level;
        }
    }
}
