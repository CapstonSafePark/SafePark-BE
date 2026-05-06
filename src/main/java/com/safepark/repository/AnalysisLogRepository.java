package com.safepark.repository;

import com.safepark.entity.AnalysisLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnalysisLogRepository extends JpaRepository<AnalysisLog, Long> {

    // 사용자별 분석 이력 조회 (페이징)
    Page<AnalysisLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 사용자별 분석 이력 조회 (전체)
    List<AnalysisLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 사용자별 분석 건수
    Long countByUserId(Long userId);

    // 사용자별 위험도별 건수
    Long countByUserIdAndRiskLevel(Long userId, String riskLevel);

    // 전체 통계용 메서드들 (기존에 있던 것들)
    Long countByRiskLevel(String riskLevel);

    Long countDistinctUsersByCreatedAtAfter(LocalDateTime date);

    List<AnalysisLog> findTop10ByOrderByCreatedAtDesc();

    // 특정 기간 내 분석 이력
    List<AnalysisLog> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);


    // 사용자의 모든 분석 이력 삭제
    void deleteByUserId(Long userId);

    Optional<AnalysisLog> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    // 날짜 필터 포함 페이징 조회
    Page<AnalysisLog> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long userId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
}