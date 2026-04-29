package com.safepark.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private String message;

    // success + data
    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
        this.message = null;
    }

    // success + message
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.data = null;
        this.message = message;
    }
}