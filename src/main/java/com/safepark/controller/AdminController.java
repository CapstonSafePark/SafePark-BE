package com.safepark.controller;

import java.time.LocalDateTime;
import com.safepark.dto.ApiResponse;
import com.safepark.dto.ParkingLotRequest;
import com.safepark.dto.ParkingLotResponse;
import com.safepark.entity.AnalysisLog;
import com.safepark.entity.User;
import com.safepark.repository.AnalysisLogRepository;
import com.safepark.repository.ParkingLotRepository;
import com.safepark.repository.UserRepository;
import com.safepark.service.ParkingFeeUpdateService;
import com.safepark.service.ParkingLotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final AnalysisLogRepository analysisLogRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingLotService parkingLotService;
    private final ParkingFeeUpdateService parkingFeeUpdateService;

    // 전체 사용자 목록 조회 - Pagination + 검색
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search_keyword
    ) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<User> userPage;

        if (search_keyword != null && !search_keyword.isEmpty()) {
            userPage = userRepository.findByUsernameContainingOrEmailContaining(
                    search_keyword, search_keyword, pageable
            );
        } else {
            userPage = userRepository.findAll(pageable);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("users", userPage.getContent());

        Map<String, Object> pagination = new HashMap<>();
        pagination.put("current_page", page);
        pagination.put("total_pages", userPage.getTotalPages());
        pagination.put("total_users", userPage.getTotalElements());
        pagination.put("users_per_page", limit);

        response.put("pagination", pagination);

        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    // 사용자 상세 조회 - statistics 추가
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        // 사용자별 분석 통계
        long totalAnalysis = analysisLogRepository.countByUserId(id);
        long highRiskCount = analysisLogRepository.countByUserIdAndRiskLevel(id, "HIGH");
        long mediumRiskCount = analysisLogRepository.countByUserIdAndRiskLevel(id, "MEDIUM");
        long lowRiskCount = analysisLogRepository.countByUserIdAndRiskLevel(id, "LOW");

        // 최근 분석 날짜
        AnalysisLog lastAnalysis = analysisLogRepository.findFirstByUserIdOrderByCreatedAtDesc(id).orElse(null);
        LocalDateTime lastAnalysisDate = lastAnalysis != null ? lastAnalysis.getCreatedAt() : null;

        // Statistics 객체 구성
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total_analysis", totalAnalysis);
        statistics.put("high_risk_count", highRiskCount);
        statistics.put("medium_risk_count", mediumRiskCount);
        statistics.put("low_risk_count", lowRiskCount);
        statistics.put("last_analysis_date", lastAnalysisDate);

        // Response 구성
        Map<String, Object> response = new HashMap<>();
        response.put("user", user);
        response.put("statistics", statistics);

        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

    // 통계 조회 - 상세 정보 추가
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalUsers = userRepository.count();
        long totalAnalysis = analysisLogRepository.count();

        long highRiskAnalysis = analysisLogRepository.countByRiskLevel("HIGH");
        long mediumRiskAnalysis = analysisLogRepository.countByRiskLevel("MEDIUM");
        long lowRiskAnalysis = analysisLogRepository.countByRiskLevel("LOW");

        long activeUsersToday = analysisLogRepository.countDistinctUsersByCreatedAtAfter(
                java.time.LocalDateTime.now().minusDays(1)
        );

        long totalParkingLots = parkingLotRepository.count();

        List<AnalysisLog> recentAnalysis = analysisLogRepository.findTop10ByOrderByCreatedAtDesc();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_users", totalUsers);
        stats.put("total_analysis", totalAnalysis);
        stats.put("high_risk_analysis", highRiskAnalysis);
        stats.put("medium_risk_analysis", mediumRiskAnalysis);
        stats.put("low_risk_analysis", lowRiskAnalysis);
        stats.put("active_users_today", activeUsersToday);
        stats.put("total_parking_lots", totalParkingLots);
        stats.put("recent_analysis", recentAnalysis);

        return ResponseEntity.ok(new ApiResponse<>(true, stats));
    }

    // 주차장 등록
    @PostMapping("/parking-lots")
    public ResponseEntity<?> createParkingLot(@Valid @RequestBody ParkingLotRequest request) {
        ParkingLotResponse parkingLot = parkingLotService.createParkingLot(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, parkingLot));
    }

    // 주차장 수정
    @PutMapping("/parking-lots/{id}")
    public ResponseEntity<?> updateParkingLot(
            @PathVariable Long id,
            @Valid @RequestBody ParkingLotRequest request
    ) {
        ParkingLotResponse parkingLot = parkingLotService.updateParkingLot(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, parkingLot));
    }

    // 주차장 삭제
    @DeleteMapping("/parking-lots/{id}")
    public ResponseEntity<?> deleteParkingLot(@PathVariable Long id) {
        parkingLotService.deleteParkingLot(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "주차장이 성공적으로 삭제되었습니다"));
    }

    // 경기도 주차장 요금 일괄 업데이트 (공공데이터 API 기반)
    @PostMapping("/update-parking-fees")
    public ResponseEntity<?> updateParkingFees() {
        try {
            Map<String, Integer> result = parkingFeeUpdateService.updateAllFees();
            return ResponseEntity.ok(new ApiResponse<>(true, result));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "요금 업데이트 실패: " + e.getMessage()));
        }
    }
}