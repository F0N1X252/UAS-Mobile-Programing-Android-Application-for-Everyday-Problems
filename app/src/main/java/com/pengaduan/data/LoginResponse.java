package com.pengaduan.data;

public class LoginResponse {
    private boolean success;
    private String message;
    private User user;
    private String token;

    public LoginResponse(boolean success, String message, User user, String token) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.token = token;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public User getUser() { return user; }
    public String getToken() { return token; }
}
