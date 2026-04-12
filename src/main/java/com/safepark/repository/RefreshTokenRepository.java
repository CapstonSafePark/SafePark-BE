package com.safepark.repository;

import com.safepark.entity.RefreshToken;
import com.safepark.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // 토큰으로 찾기
    Optional<RefreshToken> findByToken(String token);

    // 사용자로 찾기
    Optional<RefreshToken> findByUser(User user);

    // 토큰 존재 여부 확인
    boolean existsByToken(String token);

    // 사용자의 토큰 삭제 (로그아웃)
    void deleteByUser(User user);

    // 토큰으로 삭제
    void deleteByToken(String token);
}