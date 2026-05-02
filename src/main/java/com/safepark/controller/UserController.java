package com.safepark.controller;

import com.safepark.dto.PasswordChangeRequest;
import com.safepark.dto.PasswordChangeResponse;
import com.safepark.dto.UserDeleteResponse;
import com.safepark.dto.UserInfoResponse;
import com.safepark.dto.UserUpdateRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public UserInfoResponse getMyInfo(@RequestParam Long userId) {
        return new UserInfoResponse(
                userId,
                "testuser",
                "test@example.com",
                "홍길동",
                "010-1234-5678"
        );
    }

    @PutMapping("/me")
    public UserInfoResponse updateMyInfo(
            @RequestParam Long userId,
            @RequestBody UserUpdateRequest request
    ) {
        return new UserInfoResponse(
                userId,
                "testuser",
                "test@example.com",
                request.getName(),
                request.getPhone()
        );
    }

    @PutMapping("/me/password")
    public PasswordChangeResponse changePassword(
            @RequestParam Long userId,
            @RequestBody PasswordChangeRequest request
    ) {
        String savedPassword = "1234";

        if (!savedPassword.equals(request.getCurrentPassword())) {
            return new PasswordChangeResponse("현재 비밀번호가 일치하지 않습니다.");
        }

        return new PasswordChangeResponse("비밀번호가 성공적으로 변경되었습니다.");
    }

    @DeleteMapping("/me")
    public UserDeleteResponse deleteMyAccount(@RequestParam Long userId) {
        return new UserDeleteResponse("회원 탈퇴가 완료되었습니다.");
    }
}