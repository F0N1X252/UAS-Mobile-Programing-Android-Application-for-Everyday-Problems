package com.pengaduan.api;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pengaduan.data.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;

    public static ApiService getService(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        String serverIp = sessionManager.getServerIp();
        String baseUrl = "http://" + serverIp + "/laporrt/";

        // Rebuild if null or if the IP has changed
        if (retrofit == null || !retrofit.baseUrl().toString().equals(baseUrl)) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }

    public static String getImageUrl(Context context, String photoUrl) {
        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            return null;
        }
        
        // If it's already a full URL, return it
        if (photoUrl.startsWith("http://") || photoUrl.startsWith("https://")) {
            return photoUrl;
        }
        
        SessionManager sessionManager = new SessionManager(context);
        String serverIp = sessionManager.getServerIp();
        
        // Build base URL robustly
        String baseUrl = serverIp;
        if (!baseUrl.startsWith("http")) {
            baseUrl = "http://" + baseUrl;
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        // Project folder logic
        String projectFolder = "laporrt/";
        if (!baseUrl.contains(projectFolder)) {
            baseUrl += projectFolder;
        }

        // Clean up photoUrl (remove leading slash)
        String cleanPath = photoUrl.startsWith("/") ? photoUrl.substring(1) : photoUrl;
        
        // Avoid duplicate project folder in path (e.g. if path is "laporrt/uploads/...")
        if (cleanPath.startsWith(projectFolder)) {
            cleanPath = cleanPath.substring(projectFolder.length());
        }
        
        String finalUrl = baseUrl + cleanPath;
        
        // Final sanitization: remove double slashes after the protocol
        if (finalUrl.contains("://")) {
            String protocol = finalUrl.substring(0, finalUrl.indexOf("://") + 3);
            String rest = finalUrl.substring(finalUrl.indexOf("://") + 3).replace("//", "/");
            finalUrl = protocol + rest;
        }
        
        android.util.Log.d("ApiClient", "FINAL SANITIZED URL: " + finalUrl);
        return finalUrl;
    }
}
