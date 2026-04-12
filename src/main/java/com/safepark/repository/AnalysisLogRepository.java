package com.safepark.repository;

import com.safepark.entity.AnalysisLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalysisLogRepository extends JpaRepository<AnalysisLog, Long> {

    // 위험도별 카운트
    long countByRiskLevel(String riskLevel);

    // 오늘 활동한 사용자 수
    @Query("SELECT COUNT(DISTINCT a.userId) FROM AnalysisLog a WHERE a.createdAt >= :startDate")
    long countDistinctUsersByCreatedAtAfter(LocalDateTime startDate);

    // 최근 분석 기록 10개
    List<AnalysisLog> findTop10ByOrderByCreatedAtDesc();

    // 사용자별 분석 통계
    long countByUserId(Long userId);

    long countByUserIdAndRiskLevel(Long userId, String riskLevel);

    // 사용자의 최근 분석 기록
    AnalysisLog findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}