package com.example.ajp.api;

import com.example.ajp.utils.ApiKeyManager;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit client for TfL API. Add in Commit 3 with TflApi and ApiKeyManager.
 * Adds app_key to every request via interceptor.
 * Key is supplied by ApiKeyManager (guard calls with ApiKeyManager.isTflKeyValid() first).
 */
public class RetrofitClient {

    /* --- BLOCK: Configuration constants ---
     * PURPOSE: Single base URL for all TfL API calls.
     * WHY: TfL Unified API uses https://api.tfl.gov.uk/ for all endpoints.
     * ISSUES: None.
     */
    private static final String BASE_URL = "https://api.tfl.gov.uk/";

    private static volatile TflApi api;

    /* --- BLOCK: Singleton TflApi with interceptor ---
     * PURPOSE: Provide one Retrofit-backed TflApi instance, with app_key on every request.
     * WHY: Double-checked locking for thread-safe lazy init; interceptor avoids adding app_key
     *      in each TflApi method; Gson for JSON. Call ApiKeyManager.isTflKeyValid() before
     *      using in ViewModels to avoid wasted requests when key is missing.
     * ISSUES: If key is placeholder, API returns 403; guard in JourneyViewModel/StopsViewModel.
     */
    public static TflApi getApi() {
        if (api == null) {
            synchronized (RetrofitClient.class) {
                if (api == null) {
                    OkHttpClient client = new OkHttpClient.Builder()
                            .addInterceptor(chain -> {
                                Request original = chain.request();
                                HttpUrl url = original.url().newBuilder()
                                        .addQueryParameter("app_key", ApiKeyManager.getTflKey())
                                        .build();
                                return chain.proceed(original.newBuilder().url(url).build());
                            })
                            .build();
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    api = retrofit.create(TflApi.class);
                }
            }
        }
        return api;
    }
}
