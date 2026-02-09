package com.example.ajp.api;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * TfL Unified API interface. Add in Commit 3 with RetrofitClient.
 * Used for nearby stops, arrivals, line status, search, and journey planning.
 */
public interface TflApi {

    /* --- BLOCK: Nearby stops – buses ---
     * PURPOSE: Fetch bus stop points within 300m of location.
     * WHY: TfL uses radius=300 for buses so list stays manageable; NaptanPublicBusCoachTram
     *      covers bus/tram. StopsViewModel uses this for "all buses" bucket.
     * ISSUES: None.
     */
    @GET("StopPoint?stopTypes=NaptanPublicBusCoachTram&radius=300")
    Call<StopPointResponse> getNearbyBuses(@Query("lat") double lat, @Query("lon") double lon);

    /* --- BLOCK: Nearby stops – tube/rail ---
     * PURPOSE: Fetch tube and rail stations within 3.5km.
     * WHY: 3500m so District and similar lines are not missed; NaptanMetroStation,NaptanRailStation
     *      for Tube, DLR, Overground, Elizabeth, National Rail.
     * ISSUES: None.
     */
    @GET("StopPoint?stopTypes=NaptanMetroStation,NaptanRailStation&radius=3500")
    Call<StopPointResponse> getNearbyTrains(@Query("lat") double lat, @Query("lon") double lon);

    /* --- BLOCK: Single stop by ID ---
     * PURPOSE: Get StopPoint details including child platforms (for National Rail).
     * WHY: Needed when expanding a station or fetching arrivals for a specific Naptan ID.
     * ISSUES: None.
     */
    @GET("StopPoint/{id}")
    Call<StopPoint> getStopPoint(@Path("id") String stopId);

    /* --- BLOCK: Arrivals at stop ---
     * PURPOSE: Live predictions for a stop by Naptan ID.
     * WHY: Primary source for "next bus/train"; returns list of ArrivalPrediction.
     * ISSUES: For some National Rail stations (e.g. Barnes) TfL returns empty; use National Rail
     *      fallback (Commit 9).
     */
    @GET("StopPoint/{id}/Arrivals")
    Call<List<ArrivalPrediction>> getArrivals(@Path("id") String stopId);

    /* --- BLOCK: Line-specific arrivals ---
     * PURPOSE: Fallback when StopPoint/Arrivals is empty (e.g. NR).
     * WHY: Line/{ids}/Arrivals/{stopPointId} can return data when generic Arrivals does not.
     * ISSUES: Not always used; National Rail uses OpenLDBWS instead (Commit 9).
     */
    @GET("Line/{lineIds}/Arrivals/{stopPointId}")
    Call<List<ArrivalPrediction>> getLineArrivals(@Path("lineIds") String lineIds, @Path("stopPointId") String stopPointId);

    /* --- BLOCK: Line status ---
     * PURPOSE: Disruption/status for given line IDs.
     * WHY: Home and nearby list show "Good Service" or disruption; comma-separated ids.
     * ISSUES: None.
     */
    @GET("Line/{ids}/Status")
    Call<List<LineStatus>> getLineStatus(@Path("ids") String commaSeparatedIds);

    /* --- BLOCK: Search by name ---
     * PURPOSE: Search stops/stations by name (e.g. "Kings Cross").
     * WHY: TflSearchResponse has "matches" array; used in StationSearchFragment.
     * ISSUES: None; place search (Geocoder) added separately in Commit 14.
     */
    @GET("StopPoint/Search/{query}")
    Call<TflSearchResponse> searchStops(@Path("query") String query);

    /* --- BLOCK: Journey plan ---
     * PURPOSE: Plan journey from/to coordinates (lat,lon). Time HHmm, date yyyyMMdd.
     * WHY: Journey API supports walkingSpeed, maxWalkingMinutes, accessibilityPreference (e.g.
     *      noSolidStairs) for step-free routing. timeIs=Departing or Arriving.
     * ISSUES: from/to must be "lat,lon"; JourneyViewModel builds from user inputs and
     *      optional PlaceSearch resolution.
     */
    @GET("Journey/JourneyResults/{from}/to/{to}")
    Call<JourneyResponse> getJourneyResults(
            @Path("from") String from,
            @Path("to") String to,
            @Query("time") String time,
            @Query("date") String date,
            @Query("timeIs") String timeIs,
            @Query("walkingSpeed") String walkingSpeed,
            @Query("maxWalkingMinutes") Integer maxWalkingMinutes,
            @Query("accessibilityPreference") String accessibilityPreference);
}
