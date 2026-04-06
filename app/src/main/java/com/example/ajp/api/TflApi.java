package com.example.ajp.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Retrofit interface for TfL Unified API endpoints used by the app. */
public interface TflApi {

    @GET("StopPoint?stopTypes=NaptanPublicBusCoachTram&radius=300")
    Call<StopPointResponse> getNearbyBuses(@Query("lat") double lat, @Query("lon") double lon);

    @GET("StopPoint?stopTypes=NaptanMetroStation,NaptanRailStation&radius=3500")
    Call<StopPointResponse> getNearbyTrains(@Query("lat") double lat, @Query("lon") double lon);

    @GET("StopPoint/{id}")
    Call<StopPoint> getStopPoint(@Path("id") String stopId);

    @GET("StopPoint/{id}/Arrivals")
    Call<List<ArrivalPrediction>> getArrivals(@Path("id") String stopId);

    @GET("Line/{lineIds}/Arrivals/{stopPointId}")
    Call<List<ArrivalPrediction>> getLineArrivals(@Path("lineIds") String lineIds, @Path("stopPointId") String stopPointId);

    @GET("Line/{ids}/Status")
    Call<List<LineStatus>> getLineStatus(@Path("ids") String commaSeparatedIds);

    @GET("StopPoint/{id}/Disruption")
    Call<List<Object>> getStopPointDisruptions(@Path("id") String stopId);

    @GET("StopPoint/Search/{query}")
    Call<TflSearchResponse> searchStops(@Path("query") String query);

    @GET("Journey/JourneyResults/{from}/to/{to}")
    Call<JourneyResponse> getJourneyResults(
            @Path("from") String from,
            @Path("to") String to,
            @Query("time") String time,
            @Query("date") String date,
            @Query("timeIs") String timeIs,
            @Query("walkingSpeed") String walkingSpeed,
            @Query("maxWalkingMinutes") Integer maxWalkingMinutes,
            @Query("accessibilityPreference") String accessibilityPreference,
            @Query("mode") String mode);
}

