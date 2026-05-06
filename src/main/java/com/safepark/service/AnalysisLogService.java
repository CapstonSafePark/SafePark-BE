package com.safepark.service;

import com.safepark.dto.AnalysisLogResponse;
import com.safepark.entity.AnalysisLog;
import com.safepark.entity.User;
import com.safepark.repository.AnalysisLogRepository;
import com.safepark.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisLogService {

    private final AnalysisLogRepository analysisLogRepository;
    private final UserRepository userRepository;

    /**
     * 분석 이력 목록 조회 (페이징)
     * @param userId 사용자 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param limit 페이지당 개수
     * @param startDate 시작일 (선택)
     * @param endDate 종료일 (선택)
     * @return 분석 이력 목록
     */
    public Page<AnalysisLogResponse> getAnalysisLogs(
            Long userId,
            int page,
            int limit,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Pageable pageable = PageRequest.of(page, limit);
        Page<AnalysisLog> logs = analysisLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // Entity -> DTO 변환 (목록용 간단 버전)
        return logs.map(AnalysisLogResponse::fromEntitySimple);
    }

    /**
     * 분석 이력 상세 조회
     * @param userId 사용자 ID
     * @param logId 분석 이력 ID
     * @return 분석 이력 상세 정보
     */
    public AnalysisLogResponse getAnalysisLogDetail(Long userId, Long logId) {
        AnalysisLog log = analysisLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("분석 이력을 찾을 수 없습니다."));

        // 본인의 분석 이력인지 확인
        if (!log.getUser().getId().equals(userId)) {
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        // Entity -> DTO 변환 (상세 정보 포함)
        return AnalysisLogResponse.fromEntity(log);
    }

    /**
     * 분석 이력 삭제
     * @param userId 사용자 ID
     * @param logId 분석 이력 ID
     */
    @Transactional
    public void deleteAnalysisLog(Long userId, Long logId) {
        AnalysisLog log = analysisLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("분석 이력을 찾을 수 없습니다."));

        // 본인의 분석 이력인지 확인
        if (!log.getUser().getId().equals(userId)) {
            throw new RuntimeException("삭제 권한이 없습니다.");
        }

        analysisLogRepository.delete(log);
    }

    /**
     * 전체 분석 이력 삭제
     * @param userId 사용자 ID
     * @return 삭제된 개수
     */
    @Transactional
    public int deleteAllAnalysisLogs(Long userId) {
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 삭제 전 개수 확인
        Long count = analysisLogRepository.countByUserId(userId);

        // 전체 삭제
        analysisLogRepository.deleteByUserId(userId);

        return count.intValue();
    }

    /**
     * 사용자 통계 정보 조회 (관리자용)
     * @param userId 사용자 ID
     * @return 통계 정보
     */
    public UserAnalysisStats getUserStats(Long userId) {
        Long totalCount = analysisLogRepository.countByUserId(userId);
        Long highCount = analysisLogRepository.countByUserIdAndRiskLevel(userId, "HIGH");
        Long mediumCount = analysisLogRepository.countByUserIdAndRiskLevel(userId, "MEDIUM");
        Long lowCount = analysisLogRepository.countByUserIdAndRiskLevel(userId, "LOW");

        return new UserAnalysisStats(totalCount, highCount, mediumCount, lowCount);
    }

    // 내부 클래스: 사용자 통계
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class UserAnalysisStats {
        private Long totalCount;
        private Long highRiskCount;
        private Long mediumRiskCount;
        private Long lowRiskCount;
    }
}