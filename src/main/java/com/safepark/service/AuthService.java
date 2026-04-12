package com.safepark.service;

import com.safepark.dto.AuthResponse;
import com.safepark.dto.LoginRequest;
import com.safepark.dto.RegisterRequest;
import com.safepark.entity.RefreshToken;
import com.safepark.entity.User;
import com.safepark.repository.RefreshTokenRepository;
import com.safepark.repository.UserRepository;
import com.safepark.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // 중복 검사
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 존재하는 사용자명입니다");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다");
        }

        // 사용자 생성
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(savedUser.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(savedUser.getUsername());

        // Refresh Token 저장
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUserId(savedUser.getId());
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshTokenEntity);

        // 응답 생성
        return new AuthResponse(
                accessToken,
                refreshToken,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getRole()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다");
        }

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());

        // 기존 Refresh Token 삭제
        refreshTokenRepository.deleteByUserId(user.getId());

        // 새로운 Refresh Token 저장
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setUserId(user.getId());
        refreshTokenEntity.setToken(refreshToken);
        refreshTokenEntity.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(refreshTokenEntity);

        // 응답 생성
        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }
}