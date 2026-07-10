package com.pengaduan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.pengaduan.MainActivity;
import com.pengaduan.R;
import com.pengaduan.api.ApiClient;
import com.pengaduan.data.LoginResponse;
import com.pengaduan.data.SessionManager;
import com.pengaduan.databinding.ActivityLoginBinding;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            startMainActivity();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (validateInput(email, password)) {
                performLogin(email, password);
            }
        });

        binding.getRoot().findViewById(R.id.btnLogin).setOnLongClickListener(v -> {
            showServerSettingsDialog();
            return true;
        });
        
        Toast.makeText(this, "Tip: Tahan lama tombol MASUK untuk ganti IP Server", Toast.LENGTH_SHORT).show();
    }

    private void showServerSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_server_settings, null);
        EditText etIp = view.findViewById(R.id.etServerIp);
        etIp.setText(sessionManager.getServerIp());

        new AlertDialog.Builder(this)
                .setView(view)
                .setPositiveButton("Simpan", (dialog, which) -> {
                    String newIp = etIp.getText().toString().trim();
                    if (!newIp.isEmpty()) {
                        sessionManager.saveServerIp(newIp);
                        Toast.makeText(this, "IP Server diperbarui: " + newIp, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty()) {
            binding.etEmail.setError("Email tidak boleh kosong");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Format email tidak valid");
            return false;
        }
        if (password.isEmpty()) {
            binding.etPassword.setError("Password tidak boleh kosong");
            return false;
        }
        if (password.length() < 6) {
            binding.etPassword.setError("Password minimal 6 karakter");
            return false;
        }
        return true;
    }

    private void performLogin(String email, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("email", email);
        params.put("password", password);

        ApiClient.getService(this).login(params).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    LoginResponse loginResponse = response.body();
                    sessionManager.saveSession(loginResponse.getToken(), loginResponse.getUser());
                    startMainActivity();
                } else {
                    String message = "Login gagal";
                    if (response.body() != null) {
                        message = response.body().getMessage();
                    } else if (response.errorBody() != null) {
                        message = "Server Error (" + response.code() + ")";
                    }
                    Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Koneksi Gagal: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void startMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
