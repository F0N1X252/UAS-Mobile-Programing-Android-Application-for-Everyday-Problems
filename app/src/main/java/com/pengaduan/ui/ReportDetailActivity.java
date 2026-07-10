package com.pengaduan.ui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.pengaduan.R;
import com.pengaduan.api.ApiClient;
import com.pengaduan.data.BaseResponse;
import com.pengaduan.data.Report;
import com.pengaduan.data.SessionManager;
import com.pengaduan.databinding.ActivityReportDetailBinding;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportDetailActivity extends AppCompatActivity {
    private ActivityReportDetailBinding binding;
    private SessionManager sessionManager;
    private int reportId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        reportId = getIntent().getIntExtra("REPORT_ID", -1);

        if (reportId == -1) {
            Toast.makeText(this, "ID Laporan tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        fetchReportDetail();

        if ("admin".equals(sessionManager.getRole())) {
            binding.btnUpdateStatus.setVisibility(View.VISIBLE);
            binding.btnUpdateStatus.setOnClickListener(v -> showUpdateStatusDialog());
        }

        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void fetchReportDetail() {
        ApiClient.getService(this).getReportDetail(reportId).enqueue(new Callback<Report>() {
            @Override
            public void onResponse(@NonNull Call<Report> call, @NonNull Response<Report> response) {
                if (response.isSuccessful() && response.body() != null) {
                    displayReport(response.body());
                } else {
                    Toast.makeText(ReportDetailActivity.this, "Gagal memuat detail", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Report> call, @NonNull Throwable t) {
                Toast.makeText(ReportDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayReport(Report report) {
        binding.tvDetailCategory.setText(report.getCategory());
        binding.tvDetailDescription.setText(report.getDescription());
        binding.tvDetailLocation.setText(report.getLocation());
        binding.tvDetailDate.setText(report.getReportDate());
        binding.tvDetailStatus.setText(formatStatus(report.getStatus()));

        int statusColor;
        switch (report.getStatus()) {
            case "new": statusColor = R.color.status_new; break;
            case "processing": statusColor = R.color.status_processing; break;
            case "completed": statusColor = R.color.status_completed; break;
            default: statusColor = R.color.primary; break;
        }
        binding.tvDetailStatus.setBackgroundResource(statusColor);

        if (report.hasCoordinates()) {
            binding.btnOpenMap.setVisibility(View.VISIBLE);
            binding.btnOpenMap.setOnClickListener(v -> {
                // Skema URI "geo:" didukung oleh Google Maps, Waze, dll.
                String uri = "geo:" + report.getLatitude() + "," + report.getLongitude()
                        + "?q=" + report.getLatitude() + "," + report.getLongitude()
                        + "(" + Uri.encode(report.getLocation()) + ")";

                Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));

                // Membuat Chooser agar Android menampilkan dialog pilihan: Google Maps, Waze, dll.
                Intent chooser = Intent.createChooser(mapIntent, "Buka peta menggunakan:");

                try {
                    startActivity(chooser);
                } catch (android.content.ActivityNotFoundException e) {
                    // FALLBACK: Jika di HP tidak terpasang aplikasi peta apa pun (sangat jarang terjadi),
                    // maka arahkan untuk membuka Google Maps via browser web.
                    String webUri = "https://www.google.com/maps/search/?api=1&query="
                            + report.getLatitude() + "," + report.getLongitude();
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
                    startActivity(webIntent);
                }
            });
        } else {
            binding.btnOpenMap.setVisibility(View.GONE);
        }

        String fullPhotoUrl = ApiClient.getImageUrl(this, report.getPhotoUrl());
        Log.d("ReportDetail", "Loading image from: " + fullPhotoUrl);

        Glide.with(this)
                .load(fullPhotoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.ic_launcher_background)
                .error(android.R.drawable.ic_menu_report_image)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        Log.e("ReportDetail", "Glide Load Failed for: " + fullPhotoUrl, e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        Log.d("ReportDetail", "Glide Load Success");
                        return false;
                    }
                })
                .into(binding.ivDetail);
    }

    private String formatStatus(String status) {
        switch (status) {
            case "new": return "Baru";
            case "processing": return "Diproses";
            case "completed": return "Selesai";
            default: return status;
        }
    }

    private void showUpdateStatusDialog() {
        String[] statuses = {"new", "processing", "completed"};
        String[] displayStatuses = {"Baru", "Diproses", "Selesai"};
        new AlertDialog.Builder(this)
                .setTitle("Update Status")
                .setItems(displayStatuses, (dialog, which) -> updateStatus(statuses[which]))
                .show();
    }

    private void updateStatus(String newStatus) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", reportId);
        params.put("status", newStatus);

        ApiClient.getService(this).updateReportStatus(params).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                try {
                    String raw = response.isSuccessful() && response.body() != null ? response.body().string() : "";
                    boolean success = false;
                    if (!raw.isEmpty()) {
                        try {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            com.pengaduan.data.BaseResponse res = gson.fromJson(raw, com.pengaduan.data.BaseResponse.class);
                            success = res.isSuccess();
                        } catch (Exception e) {
                            success = raw.trim().equals("1") || raw.toLowerCase().contains("success");
                        }
                    }

                    if (success) {
                        Toast.makeText(ReportDetailActivity.this, "Status berhasil diperbarui", Toast.LENGTH_SHORT).show();
                        fetchReportDetail();
                    } else {
                        Toast.makeText(ReportDetailActivity.this, "Gagal: " + raw, Toast.LENGTH_SHORT).show();
                    }
                } catch (java.io.IOException e) {
                    Toast.makeText(ReportDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(ReportDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Hapus Laporan")
                .setMessage("Apakah Anda yakin ingin menghapus laporan ini?")
                .setPositiveButton("Hapus", (dialog, which) -> deleteReport())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteReport() {
        ApiClient.getService(this).deleteReport(reportId).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                try {
                    String raw = response.isSuccessful() && response.body() != null ? response.body().string() : "";
                    boolean success = false;
                    if (!raw.isEmpty()) {
                        try {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            com.pengaduan.data.BaseResponse res = gson.fromJson(raw, com.pengaduan.data.BaseResponse.class);
                            success = res.isSuccess();
                        } catch (Exception e) {
                            success = raw.trim().equals("1") || raw.toLowerCase().contains("success");
                        }
                    }

                    if (success) {
                        Toast.makeText(ReportDetailActivity.this, "Laporan berhasil dihapus", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ReportDetailActivity.this, "Gagal: " + raw, Toast.LENGTH_SHORT).show();
                    }
                } catch (java.io.IOException e) {
                    Toast.makeText(ReportDetailActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(ReportDetailActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
