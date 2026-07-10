package com.pengaduan.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import androidx.core.content.FileProvider;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.pengaduan.api.ApiClient;
import com.pengaduan.data.BaseResponse;
import com.pengaduan.data.SessionManager;
import com.pengaduan.databinding.ActivityFormLaporanBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FormLaporanActivity extends AppCompatActivity {
    private ActivityFormLaporanBinding binding;
    private SessionManager sessionManager;
    private String uploadedPhotoUrl = "";
    private double selectedLat = 0.0;
    private double selectedLng = 0.0;
    private boolean locationPicked = false;
    private boolean isUploading = false;
    private Uri cameraImageUri;

    private final ActivityResultLauncher<Intent> locationPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::handleLocationPickerResult
    );

    private void handleLocationPickerResult(ActivityResult result) {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            String address = result.getData().getStringExtra(LocationPickerActivity.EXTRA_ADDRESS);
            selectedLat = result.getData().getDoubleExtra(LocationPickerActivity.EXTRA_LATITUDE, 0.0);
            selectedLng = result.getData().getDoubleExtra(LocationPickerActivity.EXTRA_LONGITUDE, 0.0);
            locationPicked = true;
            binding.etLocation.setText(address);
        }
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        handleImageSelection(selectedImageUri);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Uri> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            isSuccess -> {
                if (isSuccess && cameraImageUri != null) {
                    handleImageSelection(cameraImageUri);
                }
            }
    );

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean cameraGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                Boolean storageGranted = result.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false);
                if (Build.VERSION.SDK_INT >= 33) {
                    storageGranted = result.getOrDefault(Manifest.permission.READ_MEDIA_IMAGES, false);
                }
                
                // Note: Logic here depends on which action triggered the request. 
                // For simplicity, we just check if the one we need is granted.
                // The dialog flow handles specific cases.
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFormLaporanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);

        setupCategorySpinner();

        binding.btnUploadPhoto.setOnClickListener(v -> showImageSourceDialog());

        View.OnClickListener openLocationPicker = v ->
                locationPickerLauncher.launch(new Intent(this, LocationPickerActivity.class));
        binding.etLocation.setOnClickListener(openLocationPicker);
        binding.btnPickLocation.setOnClickListener(openLocationPicker);

        binding.btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void setupCategorySpinner() {
        String[] categories = {"Jalan Rusak", "Lampu Mati", "Drainase Tersumbat", "Sampah Menumpuk", "Pohon Tumbang", "Fasilitas Publik Rusak", "Lainnya"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        binding.actCategory.setAdapter(adapter);
    }

    private void showImageSourceDialog() {
        String[] options = {"Kamera", "Galeri"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Pilih Sumber Foto")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermission();
                    } else {
                        checkStoragePermission();
                    }
                })
                .show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            permissionLauncher.launch(new String[]{Manifest.permission.CAMERA});
        }
    }

    private void checkStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= 33 ? Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            permissionLauncher.launch(new String[]{permission});
        }
    }

    private void openCamera() {
        File tempFile = new File(getCacheDir(), "camera_image_" + System.currentTimeMillis() + ".jpg");
        cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile);
        cameraLauncher.launch(cameraImageUri);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void handleImageSelection(Uri imageUri) {
        isUploading = true;
        binding.btnUploadPhoto.setEnabled(false);
        binding.btnUploadPhoto.setText("Mengunggah...");
        binding.btnSubmit.setEnabled(false);

        File tempFile = createTempFileFromUri(imageUri);
        if (tempFile == null) {
            Toast.makeText(this, "Gagal memproses gambar", Toast.LENGTH_SHORT).show();
            isUploading = false;
            binding.btnUploadPhoto.setEnabled(true);
            binding.btnUploadPhoto.setText("Upload Foto (Opsional)");
            binding.btnSubmit.setEnabled(true);
            return;
        }

        uploadPhoto(tempFile, imageUri);
    }

    private File createTempFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File tempFile = new File(getCacheDir(), "upload_image_" + System.currentTimeMillis() + ".jpg");
            OutputStream outputStream = new FileOutputStream(tempFile);
            
            byte[] buffer = new byte[4 * 1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    private void uploadPhoto(File file, Uri displayUri) {
        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("photo", file.getName(), requestFile);

        ApiClient.getService(this).uploadPhoto(body).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                isUploading = false;
                binding.btnUploadPhoto.setEnabled(true);
                binding.btnSubmit.setEnabled(true);
                
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String rawResponse = response.body().string();
                        android.util.Log.d("FormLaporan", "Raw Server Response: " + rawResponse);
                        
                        // Try to parse as JSON first
                        try {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType();
                            Map<String, Object> res = gson.fromJson(rawResponse, type);
                            
                            Object successObj = res.get("success");
                            boolean success = false;
                            if (successObj instanceof Boolean) success = (Boolean) successObj;
                            else if (successObj instanceof String) success = "true".equalsIgnoreCase((String) successObj);
                            else if (successObj instanceof Double) success = (Double) successObj == 1.0;

                            if (success) {
                                uploadedPhotoUrl = findPhotoUrlInResponse(res);
                            }
                        } catch (Exception e) {
                            // If not JSON, it might be an error message (HTML) or a direct path
                            // Strip HTML tags if present
                            String cleanRaw = rawResponse.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
                            if (cleanRaw.contains("uploads/") || cleanRaw.toLowerCase().endsWith(".jpg") || cleanRaw.toLowerCase().endsWith(".png")) {
                                // Find the specific part that looks like a path
                                for (String part : cleanRaw.split(" ")) {
                                    if (part.contains("uploads/") || part.toLowerCase().endsWith(".jpg")) {
                                        uploadedPhotoUrl = part.replace("\"", "").trim();
                                        break;
                                    }
                                }
                            }
                        }

                        if (uploadedPhotoUrl.isEmpty()) {
                            android.util.Log.e("FormLaporan", "SUCCESS but NO PHOTO URL FOUND in response!");
                            String debugInfo = rawResponse.length() > 100 ? rawResponse.substring(0, 97) + "..." : rawResponse;
                            Toast.makeText(FormLaporanActivity.this, "Peringatan: Server tidak mengirim path gambar. Respon: " + debugInfo, Toast.LENGTH_LONG).show();
                        } else {
                            android.util.Log.d("FormLaporan", "Captured Photo URL: " + uploadedPhotoUrl);
                            Toast.makeText(FormLaporanActivity.this, "Foto berhasil diunggah", Toast.LENGTH_SHORT).show();
                        }

                        binding.ivPreview.setVisibility(View.VISIBLE);
                        binding.ivPreview.setImageURI(displayUri);
                        binding.btnUploadPhoto.setText("Ganti Foto");
                    } else {
                        binding.btnUploadPhoto.setText("Upload Foto (Opsional)");
                        Toast.makeText(FormLaporanActivity.this, "Server Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                } catch (java.io.IOException e) {
                    Toast.makeText(FormLaporanActivity.this, "Error reading response: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                } finally {
                    file.delete(); // Delete temp file
                }
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                isUploading = false;
                binding.btnUploadPhoto.setEnabled(true);
                binding.btnSubmit.setEnabled(true);
                binding.btnUploadPhoto.setText("Upload Foto (Opsional)");
                Toast.makeText(FormLaporanActivity.this, "Error upload: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                file.delete(); // Delete temp file
            }
        });
    }

    private String findPhotoUrlInResponse(Object obj) {
        if (obj == null) return "";
        
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            // 1. Check known keys first
            String[] commonKeys = {"photo_url", "url", "data", "path", "file", "filename", "image", "src"};
            for (String key : commonKeys) {
                if (map.containsKey(key) && map.get(key) instanceof String) {
                    return (String) map.get(key);
                }
            }
            // 2. Recursive search in map values
            for (Object val : map.values()) {
                String result = findPhotoUrlInResponse(val);
                if (!result.isEmpty()) return result;
            }
        } else if (obj instanceof Iterable) {
            // Recursive search in list/set
            for (Object item : (Iterable<?>) obj) {
                String result = findPhotoUrlInResponse(item);
                if (!result.isEmpty()) return result;
            }
        } else if (obj instanceof String) {
            String str = (String) obj;
            String lower = str.toLowerCase();
            if (lower.contains("uploads/") || lower.endsWith(".jpg") || lower.endsWith(".png") || lower.endsWith(".jpeg")) {
                return str;
            }
        }
        
        return "";
    }

    private void validateAndSubmit() {
        if (isUploading) {
            Toast.makeText(this, "Mohon tunggu, foto sedang diunggah", Toast.LENGTH_SHORT).show();
            return;
        }

        String category = binding.actCategory.getText().toString();
        String description = binding.etDescription.getText().toString().trim();
        String location = binding.etLocation.getText().toString().trim();

        if (category.isEmpty()) {
            Toast.makeText(this, "Silakan pilih kategori laporan", Toast.LENGTH_SHORT).show();
            binding.actCategory.showDropDown();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(this, "Silakan isi deskripsi laporan", Toast.LENGTH_SHORT).show();
            binding.etDescription.requestFocus();
            return;
        }

        if (description.length() < 10) {
            Toast.makeText(this, "Deskripsi minimal 10 karakter", Toast.LENGTH_SHORT).show();
            binding.etDescription.requestFocus();
            return;
        }

        if (location.isEmpty()) {
            Toast.makeText(this, "Silakan pilih lokasi laporan di peta", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!locationPicked) {
            Toast.makeText(this, "Silakan pilih lokasi di peta terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        submitReport(category, description, location);
    }

    private void submitReport(String category, String description, String location) {
        Map<String, Object> params = new HashMap<>();
        params.put("user_id", sessionManager.getUserId());
        params.put("category", category);
        params.put("description", description);
        params.put("location", location);
        params.put("latitude", selectedLat);
        params.put("longitude", selectedLng);
        
        // Send under ALL possible common keys
        String[] keys = {
                "photo_url", "photo", "image", "image_url", 
                "file", "filename", "path", "img", 
                "picture", "attachment", "media", "url"
        };
        for (String key : keys) {
            params.put(key, uploadedPhotoUrl);
        }
        
        params.put("status", "new");

        android.util.Log.d("FormLaporan", "SUBMITTING JSON PARAMS: " + params.toString());

        ApiClient.getService(this).createReport(params).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                try {
                    String raw = response.isSuccessful() && response.body() != null ? response.body().string() : "";
                    android.util.Log.d("FormLaporan", "Create Report Response: " + raw);
                    
                    boolean success = false;
                    if (!raw.isEmpty()) {
                        try {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            com.pengaduan.data.BaseResponse res = gson.fromJson(raw, com.pengaduan.data.BaseResponse.class);
                            success = res.isSuccess();
                        } catch (Exception e) {
                            // If it's not JSON, it might be an error message or "1"
                            success = raw.trim().equals("1") || raw.toLowerCase().contains("success");
                        }
                    }

                    if (success) {
                        Toast.makeText(FormLaporanActivity.this, "Laporan berhasil dikirim", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        String errorMsg = raw.isEmpty() ? "Server returned empty response" : raw;
                        Toast.makeText(FormLaporanActivity.this, "Gagal: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                } catch (java.io.IOException e) {
                    Toast.makeText(FormLaporanActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                Toast.makeText(FormLaporanActivity.this, "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
