package com.safepark.controller;

import com.safepark.entity.User;
import com.safepark.repository.RefreshTokenRepository;
import com.safepark.repository.UserRepository;
import com.safepark.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    // 내 정보 조회
    // GET /api/users/me
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@RequestHeader("Authorization") String token) {
        try {
            User user = getUserFromToken(token);

            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("name", user.getName());
            data.put("phone", user.getPhone());
            data.put("role", user.getRole());
            data.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // 내 정보 수정 (name, phone)
    // PUT /api/users/me
    @PutMapping("/me")
    public ResponseEntity<?> updateMyInfo(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> request
    ) {
        try {
            User user = getUserFromToken(token);

            if (request.containsKey("name")) {
                user.setName(request.get("name"));
            }
            if (request.containsKey("phone")) {
                user.setPhone(request.get("phone"));
            }
            userRepository.save(user);

            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("email", user.getEmail());
            data.put("name", user.getName());
            data.put("phone", user.getPhone());

            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // 비밀번호 변경
    // PUT /api/users/me/password
    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> request
    ) {
        try {
            User user = getUserFromToken(token);

            String currentPassword = request.get("currentPassword");
            String newPassword = request.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.status(400).body(Map.of("success", false, "error", "currentPassword, newPassword 필드가 필요합니다"));
            }

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                return ResponseEntity.status(400).body(Map.of("success", false, "error", "현재 비밀번호가 올바르지 않습니다"));
            }

            if (newPassword.length() < 8) {
                return ResponseEntity.status(400).body(Map.of("success", false, "error", "새 비밀번호는 8자 이상이어야 합니다"));
            }

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("success", true, "message", "비밀번호가 변경되었습니다"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // 회원 탈퇴
    // DELETE /api/users/me
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMyAccount(@RequestHeader("Authorization") String token) {
        try {
            User user = getUserFromToken(token);

            refreshTokenRepository.deleteByUserId(user.getId());
            userRepository.delete(user);

            return ResponseEntity.ok(Map.of("success", true, "message", "회원 탈퇴가 완료되었습니다"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private User getUserFromToken(String token) {
        String jwt = token.replace("Bearer ", "");
        String username = jwtTokenProvider.getUsernameFromToken(jwt);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }
}
