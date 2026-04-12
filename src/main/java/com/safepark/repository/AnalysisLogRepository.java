package com.safepark.repository;

import com.safepark.entity.AnalysisLog;
import com.safepark.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalysisLogRepository extends JpaRepository<AnalysisLog, Long> {

    // 사용자별 분석 이력 조회
    List<AnalysisLog> findByUser(User user);

    // 사용자별 분석 이력 (최신순)
    List<AnalysisLog> findByUserOrderByCreatedAtDesc(User user);

    // 특정 기간 내 분석 이력
    List<AnalysisLog> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    // 위험도별 필터링
    List<AnalysisLog> findByRiskLevel(String riskLevel);

    // 사용자별 분석 개수
    long countByUser(User user);
}