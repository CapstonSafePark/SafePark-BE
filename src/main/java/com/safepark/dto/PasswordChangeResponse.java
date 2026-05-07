package com.safepark.dto;

public class PasswordChangeResponse {

    private String message;

    public PasswordChangeResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}