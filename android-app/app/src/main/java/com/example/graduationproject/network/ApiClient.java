package com.example.graduationproject.network;

import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    public static final String BASE_URL = "http://100.64.87.9:8000";
    private static OkHttpClient client;

    public static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(360, TimeUnit.SECONDS)
                    .readTimeout(360, TimeUnit.SECONDS)
                    .writeTimeout(360, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }
}