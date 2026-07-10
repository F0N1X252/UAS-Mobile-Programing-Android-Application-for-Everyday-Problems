package com.pengaduan.data;

import com.google.gson.annotations.SerializedName;

public class Report {
    private int id;
    @SerializedName("user_id")
    private int userId;
    private String category;
    private String description;
    private String location;
    private Double latitude;
    private Double longitude;
    @SerializedName("photo_url")
    private String photoUrl;
    private String status;
    @SerializedName("report_date")
    private String reportDate;

    public Report(int id, int userId, String category, String description, String location, String photoUrl, String status, String reportDate) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.description = description;
        this.location = location;
        this.photoUrl = photoUrl;
        this.status = status;
        this.reportDate = reportDate;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public boolean hasCoordinates() { return latitude != null && longitude != null; }
    public String getPhotoUrl() { return photoUrl; }
    public String getStatus() { return status; }
    public String getReportDate() { return reportDate; }
}
