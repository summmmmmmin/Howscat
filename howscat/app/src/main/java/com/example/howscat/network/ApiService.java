package com.example.howscat.network;

import com.example.howscat.dto.AiSummaryResponse;
import com.example.howscat.dto.CareWeightRequest;
import com.example.howscat.dto.CatRequest;
import com.example.howscat.dto.CatResponse;
import com.example.howscat.dto.CalendarEventItem;
import com.example.howscat.dto.CalendarMemoUpdateRequest;
import com.example.howscat.dto.CalendarMemoCreateRequest;
import com.example.howscat.dto.FoodInventoryItem;
import com.example.howscat.dto.FoodInventoryUpdateRequest;
import com.example.howscat.dto.HospitalNearbyResponse;
import com.example.howscat.dto.LitterBoxCreateRequest;
import com.example.howscat.dto.LitterBoxItem;
import com.example.howscat.dto.LoginRequest;
import com.example.howscat.dto.LoginResponse;
import com.example.howscat.dto.MedicationCreateRequest;
import com.example.howscat.dto.MedicationItem;
import com.example.howscat.dto.SignupRequest;
import com.example.howscat.dto.ApiResponse;
import com.example.howscat.dto.ObesityCheckRequest;
import com.example.howscat.dto.ObesityCheckResponse;
import com.example.howscat.dto.HealthScheduleCreateRequest;
import com.example.howscat.dto.HealthScheduleItem;
import com.example.howscat.dto.HealthScheduleUpdateRequest;
import com.example.howscat.dto.ObesityHistoryItem;
import com.example.howscat.dto.VetVisitCreateRequest;
import com.example.howscat.dto.VetVisitItem;
import com.example.howscat.dto.VomitAnalysisRequest;
import com.example.howscat.dto.VomitAnalysisResponse;
import com.example.howscat.dto.WeightHistoryItem;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.DELETE;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    @POST("api/users/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/users/signup")
    Call<ApiResponse> signup(@Body SignupRequest request);

    @POST("/api/users/logout")
    Call<ApiResponse> logout();

    @POST("cats")
    Call<ApiResponse> registerCat(@Body CatRequest request);

    @GET("/cats/{id}")
    Call<CatResponse> getCat(@Path("id") Long id);

    @GET("/cats/user")
    Call<List<CatResponse>> getUserCats();

    @POST("/cats/select/{catId}")
    Call<Void> selectCat(@Path("catId") Long catId);

    // ----------------------------
    // Hospitals
    // ----------------------------

    @GET("/hospitals/nearby")
    Call<List<HospitalNearbyResponse>> listNearbyHospitals(
            @Query("lat") Double lat,
            @Query("lng") Double lng,
            @Query("radius") Double radius,
            @Query("sort") String sort,
            @Query("only24h") Boolean only24h,
            @Query("onlyOperating") Boolean onlyOperating
    );

    @GET("/hospitals/favorites")
    Call<List<HospitalNearbyResponse>> listFavoriteHospitals(
            @Query("lat") Double lat,
            @Query("lng") Double lng,
            @Query("sort") String sort
    );

    @POST("/hospitals/{id}/favorite")
    Call<Void> addHospitalFavorite(
            @Path("id") Long hospitalId
    );

    @DELETE("/hospitals/{id}/favorite")
    Call<Void> removeHospitalFavorite(
            @Path("id") Long hospitalId
    );

    // ----------------------------
    // Calendar
    // ----------------------------
    @GET("/cats/{catId}/calendar")
    Call<List<CalendarEventItem>> getCalendarEvents(
            @Path("catId") Long catId,
            @Query("from") String from,
            @Query("to") String to
    );

    // ----------------------------
    // Calendar memo update
    // ----------------------------
    @PUT("/cats/{catId}/calendar-memos/{memoId}")
    Call<Void> updateCalendarMemo(
            @Path("catId") Long catId,
            @Path("memoId") Long memoId,
            @Body CalendarMemoUpdateRequest request
    );

    @POST("/cats/{catId}/calendar-memos")
    Call<Void> addCalendarMemo(
            @Path("catId") Long catId,
            @Body CalendarMemoCreateRequest request
    );

    @DELETE("/cats/{catId}/calendar-memos")
    Call<Void> deleteAllCalendarMemos(
            @Path("catId") Long catId
    );

    @DELETE("/cats/{catId}/calendar-memos/{memoId}")
    Call<Void> deleteCalendarMemo(
            @Path("catId") Long catId,
            @Path("memoId") Long memoId
    );

    // ----------------------------
    // AI health summary
    // ----------------------------
    @GET("/cats/{catId}/ai-summary")
    Call<AiSummaryResponse> getAiHealthSummary(@Path("catId") long catId);

    // ----------------------------
    // Cat care (weight / obesity / vomit)
    // ----------------------------
    @POST("/cats/{catId}/care-weight")
    Call<Void> recordCareWeight(
            @Path("catId") Long catId,
            @Body CareWeightRequest request
    );

    @POST("/cats/{catId}/obesity-check")
    Call<ObesityCheckResponse> checkObesity(
            @Path("catId") Long catId,
            @Body ObesityCheckRequest request
    );

    @POST("/cats/{catId}/vomit")
    Call<VomitAnalysisResponse> analyzeVomit(
            @Path("catId") Long catId,
            @Body VomitAnalysisRequest request
    );

    @DELETE("/cats/{catId}/vomit/{vomitId}")
    Call<Void> deleteVomitRecord(
            @Path("catId") Long catId,
            @Path("vomitId") Long vomitId
    );

    // ----------------------------
    // Cat care history (weight / obesity)
    // ----------------------------
    @GET("/cats/{catId}/weight-history")
    Call<List<WeightHistoryItem>> getWeightHistory(
            @Path("catId") Long catId,
            @Query("limit") Integer limit
    );

    @GET("/cats/{catId}/obesity-history")
    Call<List<ObesityHistoryItem>> getObesityHistory(
            @Path("catId") Long catId,
            @Query("limit") Integer limit
    );

    // ----------------------------
    // Health schedule (checkup / vaccine) for auto alarms
    // ----------------------------
    @GET("/cats/{catId}/health-schedules")
    Call<List<HealthScheduleItem>> getHealthSchedules(
            @Path("catId") Long catId
    );

    @POST("/cats/{catId}/health-schedules")
    Call<HealthScheduleItem> createHealthSchedule(
            @Path("catId") long catId,
            @Body HealthScheduleCreateRequest request
    );

    @PUT("/cats/{catId}/health-schedules/{scheduleId}")
    Call<Void> updateHealthSchedule(
            @Path("catId") Long catId,
            @Path("scheduleId") Long scheduleId,
            @Body HealthScheduleUpdateRequest request
    );

    @DELETE("/cats/{catId}/health-schedules/{scheduleId}")
    Call<Void> deleteHealthSchedule(
            @Path("catId") long catId,
            @Path("scheduleId") Long scheduleId
    );

    // ----------------------------
    // Medication records
    // ----------------------------
    @GET("/cats/{catId}/medications")
    Call<List<MedicationItem>> getMedications(@Path("catId") Long catId);

    @POST("/cats/{catId}/medications")
    Call<ApiResponse> addMedication(
            @Path("catId") Long catId,
            @Body MedicationCreateRequest request
    );

    @PUT("/cats/{catId}/medications/{medicationId}")
    Call<Void> updateMedication(
            @Path("catId") Long catId,
            @Path("medicationId") Long medicationId,
            @Body MedicationCreateRequest request
    );

    @DELETE("/cats/{catId}/medications/{medicationId}")
    Call<Void> deleteMedication(
            @Path("catId") Long catId,
            @Path("medicationId") Long medicationId
    );

    // ----------------------------
    // Litter box records
    // ----------------------------
    @GET("/cats/{catId}/litter-records")
    Call<List<LitterBoxItem>> getLitterRecords(
            @Path("catId") Long catId,
            @Query("limit") Integer limit
    );

    @POST("/cats/{catId}/litter-records")
    Call<ApiResponse> addLitterRecord(
            @Path("catId") Long catId,
            @Body LitterBoxCreateRequest request
    );

    @PUT("/cats/{catId}/litter-records/{recordId}")
    Call<Void> updateLitterRecord(
            @Path("catId") Long catId,
            @Path("recordId") Long recordId,
            @Body LitterBoxCreateRequest request
    );

    @DELETE("/cats/{catId}/litter-records/{recordId}")
    Call<Void> deleteLitterRecord(
            @Path("catId") Long catId,
            @Path("recordId") Long recordId
    );

    // ----------------------------
    // Vet visit records
    // ----------------------------
    @GET("/cats/{catId}/vet-visits")
    Call<List<VetVisitItem>> getVetVisits(@Path("catId") Long catId);

    @POST("/cats/{catId}/vet-visits")
    Call<ApiResponse> addVetVisit(
            @Path("catId") Long catId,
            @Body VetVisitCreateRequest request
    );

    @PUT("/cats/{catId}/vet-visits/{visitId}")
    Call<Void> updateVetVisit(
            @Path("catId") Long catId,
            @Path("visitId") Long visitId,
            @Body VetVisitCreateRequest request
    );

    @DELETE("/cats/{catId}/vet-visits/{visitId}")
    Call<Void> deleteVetVisit(
            @Path("catId") Long catId,
            @Path("visitId") Long visitId
    );

    // ----------------------------
    // Food inventory
    // ----------------------------
    @GET("/cats/{catId}/food-inventory")
    Call<List<FoodInventoryItem>> getFoodInventory(@Path("catId") Long catId);

    @POST("/cats/{catId}/food-inventory")
    Call<ApiResponse> addFoodInventory(
            @Path("catId") Long catId,
            @Body FoodInventoryUpdateRequest request
    );

    @PUT("/cats/{catId}/food-inventory/{inventoryId}")
    Call<Void> updateFoodInventory(
            @Path("catId") Long catId,
            @Path("inventoryId") Long inventoryId,
            @Body FoodInventoryUpdateRequest request
    );

    @DELETE("/cats/{catId}/food-inventory/{inventoryId}")
    Call<Void> deleteFoodInventory(
            @Path("catId") Long catId,
            @Path("inventoryId") Long inventoryId
    );
}
