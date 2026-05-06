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

    // 기존에 있던 생성자 1 (success + data)
    public ApiResponse(boolean success, T data) {
        this.success = success;
        this.data = data;
        this.message = null;
    }

    // 기존에 있던 생성자 2 (success + message)
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.data = null;
        this.message = message;
    }

    // ⭐ 방금 추가한 성공/실패 static 메서드 3개
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, null, message);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
