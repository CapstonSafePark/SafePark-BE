package com.safepark.controller;

import com.safepark.dto.ApiResponse;
import com.safepark.dto.AuthResponse;
import com.safepark.dto.LoginRequest;
import com.safepark.dto.RegisterRequest;
import com.safepark.entity.RefreshToken;
import com.safepark.entity.User;
import com.safepark.repository.RefreshTokenRepository;
import com.safepark.repository.UserRepository;
import com.safepark.security.JwtTokenProvider;
import com.safepark.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", response);

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    // 테스트용 엔드포인트
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "SafePark Auth API is running!");

        return ResponseEntity.ok(result);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader("Authorization") String token
    ) {
        try {
            String jwt = token.replace("Bearer ", "");
            String username = jwtTokenProvider.getUsernameFromToken(jwt);

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            // Refresh Token 삭제
            refreshTokenRepository.deleteByUserId(user.getId());

            return ResponseEntity.ok(new ApiResponse<>(true, "로그아웃되었습니다"));
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(false, "인증 토큰이 없거나 유효하지 않습니다"));
        }
    }

    // 토큰 갱신
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestBody Map<String, String> request
    ) {
        try {
            String refreshToken = request.get("refreshToken");

            if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(400)
                        .body(new ApiResponse<>(false, "유효하지 않은 Refresh Token입니다"));
            }

            String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            // DB에 저장된 Refresh Token 확인
            RefreshToken savedToken = refreshTokenRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("저장된 Refresh Token이 없습니다"));

            if (!savedToken.getToken().equals(refreshToken)) {
                return ResponseEntity.status(401)
                        .body(new ApiResponse<>(false, "유효하지 않은 Refresh Token입니다"));
            }

            // 새로운 토큰 발급
            String newAccessToken = jwtTokenProvider.generateAccessToken(username);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

            // 새로운 Refresh Token 저장
            savedToken.setToken(newRefreshToken);
            savedToken.setExpiresAt(LocalDateTime.now().plusDays(7));
            refreshTokenRepository.save(savedToken);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("refreshToken", newRefreshToken);

            return ResponseEntity.ok(new ApiResponse<>(true, tokens));
        } catch (Exception e) {
            return ResponseEntity.status(400)
                    .body(new ApiResponse<>(false, "토큰 갱신에 실패했습니다"));
        }
    }
}