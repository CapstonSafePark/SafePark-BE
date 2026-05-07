package com.safepark.dto;

public class PasswordChangeRequest {

    private String currentPassword;
    private String newPassword;

    public PasswordChangeRequest() {
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }
}