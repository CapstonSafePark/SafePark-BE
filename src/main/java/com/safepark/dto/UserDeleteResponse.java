package com.safepark.dto;

public class UserDeleteResponse {

    private String message;

    public UserDeleteResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}