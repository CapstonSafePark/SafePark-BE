package com.safepark.repository;

import com.safepark.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    // 중복 체크 메서드 추가
    boolean existsByUsername(String username);  // 추가!

    boolean existsByEmail(String email);  // 추가!

    // Pagination + 검색
    Page<User> findByUsernameContainingOrEmailContaining(
            String username,
            String email,
            Pageable pageable
    );
}