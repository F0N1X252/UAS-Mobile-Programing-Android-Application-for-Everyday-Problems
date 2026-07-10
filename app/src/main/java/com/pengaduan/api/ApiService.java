package com.pengaduan.api;

import com.pengaduan.data.BaseResponse;
import com.pengaduan.data.LoginResponse;
import com.pengaduan.data.Report;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ApiService {
    @POST("login.php")
    Call<LoginResponse> login(@Body Map<String, String> loginRequest);

    @GET("list.php")
    Call<List<Report>> getReports();

    @Multipart
    @POST("upload_photo.php")
    Call<okhttp3.ResponseBody> uploadPhoto(@Part MultipartBody.Part photo);

    @POST("create.php")
    Call<okhttp3.ResponseBody> createReport(@Body Map<String, Object> reportData);

    @GET("detail.php")
    Call<Report> getReportDetail(@Query("id") int id);

    @PUT("update_status.php")
    Call<okhttp3.ResponseBody> updateReportStatus(@Body Map<String, Object> statusData);

    @DELETE("delete.php")
    Call<okhttp3.ResponseBody> deleteReport(@Query("id") int id);
}
