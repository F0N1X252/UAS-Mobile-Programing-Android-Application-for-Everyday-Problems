package com.pengaduan;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.pengaduan.api.ApiClient;
import com.pengaduan.data.Report;
import com.pengaduan.data.SessionManager;
import com.pengaduan.databinding.ActivityMainBinding;
import com.pengaduan.ui.FormLaporanActivity;
import com.pengaduan.ui.LoginActivity;
import com.pengaduan.ui.ReportAdapter;
import com.pengaduan.ui.ReportDetailActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private ReportAdapter adapter;
    private SessionManager sessionManager;
    private List<Report> allReports = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        setupRecyclerView();
        setupChips();
        fetchReports();

        binding.fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(this, FormLaporanActivity.class));
        });
    }

    private void setupRecyclerView() {
        adapter = new ReportAdapter(new ArrayList<>(), report -> {
            Intent intent = new Intent(this, ReportDetailActivity.class);
            intent.putExtra("REPORT_ID", report.getId());
            startActivity(intent);
        });
        binding.rvReports.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReports.setAdapter(adapter);
    }

    private void setupChips() {
        binding.chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            List<Report> filteredList = new ArrayList<>();
            if (checkedId == R.id.chipRoads) {
                for (Report r : allReports) if ("Jalan Rusak".equals(r.getCategory())) filteredList.add(r);
            } else if (checkedId == R.id.chipLights) {
                for (Report r : allReports) if ("Lampu Mati".equals(r.getCategory())) filteredList.add(r);
            } else if (checkedId == R.id.chipTrash) {
                for (Report r : allReports) if ("Sampah".equals(r.getCategory())) filteredList.add(r);
            } else {
                filteredList = allReports;
            }
            adapter.updateData(filteredList);
        });
    }

    private void fetchReports() {
        ApiClient.getService(this).getReports().enqueue(new Callback<List<Report>>() {
            @Override
            public void onResponse(Call<List<Report>> call, Response<List<Report>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allReports = response.body();
                    adapter.updateData(allReports);
                } else {
                    Toast.makeText(MainActivity.this, "Gagal mengambil data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Report>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            performLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void performLogout() {
        sessionManager.clearSession();
        Toast.makeText(this, "Berhasil keluar", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchReports();
    }
}
