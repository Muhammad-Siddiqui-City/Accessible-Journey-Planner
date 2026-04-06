package com.example.ajp.api;

import com.example.ajp.utils.ApiKeyManager;
import java.util.concurrent.TimeUnit;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Singleton Retrofit + OkHttp; app_key query param injected on each request. */
public class RetrofitClient {

    private static final String BASE_URL = "https://api.tfl.gov.uk/";

    private static volatile TflApi api;

    public static TflApi getApi() {
        if (api == null) {
            synchronized (RetrofitClient.class) {
                if (api == null) {
                    OkHttpClient client = new OkHttpClient.Builder()
                            // Defaults (~10s read) are tight for cold TLS + slow TfL; journey JSON can be large.
                            .connectTimeout(20, TimeUnit.SECONDS)
                            .readTimeout(45, TimeUnit.SECONDS)
                            .writeTimeout(20, TimeUnit.SECONDS)
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

