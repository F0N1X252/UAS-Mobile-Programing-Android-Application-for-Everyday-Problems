package com.pengaduan.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.pengaduan.R;
import com.pengaduan.databinding.ActivityLocationPickerBinding;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;


public class LocationPickerActivity extends AppCompatActivity {

    public static final String EXTRA_ADDRESS = "extra_address";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";

    private ActivityLocationPickerBinding binding;
    private MapView map;
    private FusedLocationProviderClient fusedLocationClient;

    private String selectedAddress = "";
    private double selectedLat = 0.0;
    private double selectedLng = 0.0;

    private static final GeoPoint DEFAULT_LOCATION = new GeoPoint(-6.2088, 106.8456);


    private final Handler idleHandler = new Handler(Looper.getMainLooper());
    private Runnable idleRunnable;

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    moveToCurrentLocation();
                } else {
                    Toast.makeText(this, "Izin lokasi diperlukan untuk fitur ini", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<IntentSenderRequest> settingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    moveToCurrentLocation();
                } else {
                    Toast.makeText(this, "GPS harus aktif untuk mendapatkan lokasi", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SharedPreferences osmPrefs = getApplicationContext()
                .getSharedPreferences("osmdroid_prefs", MODE_PRIVATE);
        Configuration.getInstance().load(getApplicationContext(), osmPrefs);
        Configuration.getInstance().setUserAgentValue(getPackageName());

        super.onCreate(savedInstanceState);
        binding = ActivityLocationPickerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        map = binding.map;
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        map.getController().setCenter(DEFAULT_LOCATION);


        map.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                scheduleIdleCheck();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                scheduleIdleCheck();
                return true;
            }
        });

        binding.fabMyLocation.setOnClickListener(v -> checkPermissionAndLocate());

        binding.btnConfirmLocation.setOnClickListener(v -> {
            if (selectedLat == 0.0 && selectedLng == 0.0) {
                Toast.makeText(this, "Tunggu sebentar, sedang mengambil lokasi...", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent result = new Intent();
            result.putExtra(EXTRA_ADDRESS, selectedAddress);
            result.putExtra(EXTRA_LATITUDE, selectedLat);
            result.putExtra(EXTRA_LONGITUDE, selectedLng);
            setResult(RESULT_OK, result);
            finish();
        });

        checkPermissionAndLocate();
        scheduleIdleCheck();
    }

    private void scheduleIdleCheck() {
        if (idleRunnable != null) {
            idleHandler.removeCallbacks(idleRunnable);
        }
        idleRunnable = () -> {
            GeoPoint center = (GeoPoint) map.getMapCenter();
            selectedLat = center.getLatitude();
            selectedLng = center.getLongitude();
            reverseGeocode(center);
        };
        idleHandler.postDelayed(idleRunnable, 500);
    }

    private void checkPermissionAndLocate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            moveToCurrentLocation();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    private void moveToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        // Define location request for settings check and fallback
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        client.checkLocationSettings(builder.build())
                .addOnSuccessListener(this, response -> {
                    // GPS is active and settings are satisfied
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener(this, location -> {
                                binding.progressBar.setVisibility(View.GONE);
                                if (location != null && map != null) {
                                    GeoPoint current = new GeoPoint(location.getLatitude(), location.getLongitude());
                                    map.getController().setZoom(17.0);
                                    map.getController().animateTo(current);
                                } else {
                                    // Fallback to active updates if getCurrentLocation is null
                                    requestSingleUpdate(locationRequest);
                                }
                            })
                            .addOnFailureListener(this, e -> {
                                // If getCurrentLocation fails, try updates
                                requestSingleUpdate(locationRequest);
                            });
                })
                .addOnFailureListener(this, e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    if (e instanceof ResolvableApiException) {
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            IntentSenderRequest intentSenderRequest = new IntentSenderRequest.Builder(
                                    resolvable.getResolution().getIntentSender()).build();
                            settingsLauncher.launch(intentSenderRequest);
                        } catch (Exception sendEx) {
                            Toast.makeText(this, "Gagal mengaktifkan GPS", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Gagal mendeteksi status GPS: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void requestSingleUpdate(LocationRequest locationRequest) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                fusedLocationClient.removeLocationUpdates(this);
                binding.progressBar.setVisibility(View.GONE);
                Location location = locationResult.getLastLocation();
                if (location != null && map != null) {
                    GeoPoint current = new GeoPoint(location.getLatitude(), location.getLongitude());
                    map.getController().setZoom(17.0);
                    map.getController().animateTo(current);
                } else {
                    Toast.makeText(LocationPickerActivity.this, "Gagal mendapatkan lokasi, pastikan Anda berada di ruang terbuka", Toast.LENGTH_SHORT).show();
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // Safety timeout
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            if (binding.progressBar.getVisibility() == View.VISIBLE) {
                binding.progressBar.setVisibility(View.GONE);
            }
        }, 15000); // 15 seconds timeout
    }

    private void reverseGeocode(GeoPoint point) {
        binding.tvSelectedAddress.setText("Mencari alamat...");
        new Thread(() -> {
            String addressText;
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(point.getLatitude(), point.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(address.getAddressLine(i));
                    }
                    addressText = sb.length() > 0 ? sb.toString()
                            : String.format(Locale.getDefault(), "%.6f, %.6f", point.getLatitude(), point.getLongitude());
                } else {
                    addressText = String.format(Locale.getDefault(), "%.6f, %.6f", point.getLatitude(), point.getLongitude());
                }
            } catch (IOException e) {
                // Geocoder butuh koneksi internet; kalau gagal, fallback tampilkan koordinat mentah
                addressText = String.format(Locale.getDefault(), "%.6f, %.6f", point.getLatitude(), point.getLongitude());
            }

            String finalAddressText = addressText;
            runOnUiThread(() -> {
                selectedAddress = finalAddressText;
                binding.tvSelectedAddress.setText(finalAddressText);
            });
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (map != null) map.onPause();
    }
}
