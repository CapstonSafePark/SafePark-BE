package com.safepark.controller;

import com.safepark.dto.AnalysisLogResponse;
import com.safepark.entity.AnalysisLog;
import com.safepark.entity.User;
import com.safepark.repository.AnalysisLogRepository;
import com.safepark.repository.UserRepository;
import com.safepark.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final AnalysisLogRepository analysisLogRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 분석 이력 목록 조회 (페이징)
     * GET /api/history?page=0&limit=10&startDate=...&endDate=...
     */
    @GetMapping
    public ResponseEntity<?> getHistory(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {
        try {
            User user = getUserFromToken(token);
            PageRequest pageable = PageRequest.of(page, limit);

            Page<AnalysisLog> logs;
            if (startDate != null && endDate != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
                LocalDateTime start = LocalDateTime.parse(startDate, fmt);
                LocalDateTime end = LocalDateTime.parse(endDate, fmt);
                logs = analysisLogRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                        user.getId(), start, end, pageable);
            } else {
                logs = analysisLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
            }

            Map<String, Object> pagination = new HashMap<>();
            pagination.put("currentPage", logs.getNumber());
            pagination.put("totalPages", logs.getTotalPages());
            pagination.put("totalItems", logs.getTotalElements());
            pagination.put("itemsPerPage", logs.getSize());

            Map<String, Object> data = new HashMap<>();
            data.put("logs", logs.getContent().stream().map(AnalysisLogResponse::fromEntitySimple).toList());
            data.put("pagination", pagination);

            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 분석 이력 상세 조회
     * GET /api/history/{historyId}
     */
    @GetMapping("/{historyId}")
    public ResponseEntity<?> getHistoryDetail(
            @RequestHeader("Authorization") String token,
            @PathVariable Long historyId
    ) {
        try {
            User user = getUserFromToken(token);

            AnalysisLog log = analysisLogRepository.findById(historyId)
                    .orElseThrow(() -> new RuntimeException("분석 이력을 찾을 수 없습니다"));

            if (!log.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "접근 권한이 없습니다"));
            }

            return ResponseEntity.ok(Map.of("success", true, "data", AnalysisLogResponse.fromEntity(log)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 분석 이력 삭제
     * DELETE /api/history/{historyId}
     */
    @Transactional
    @DeleteMapping("/{historyId}")
    public ResponseEntity<?> deleteHistory(
            @RequestHeader("Authorization") String token,
            @PathVariable Long historyId
    ) {
        try {
            User user = getUserFromToken(token);

            AnalysisLog log = analysisLogRepository.findById(historyId)
                    .orElseThrow(() -> new RuntimeException("분석 이력을 찾을 수 없습니다"));

            if (!log.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "접근 권한이 없습니다"));
            }

            analysisLogRepository.delete(log);

            return ResponseEntity.ok(Map.of("success", true, "message", "분석 이력이 삭제되었습니다"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 전체 분석 이력 삭제
     * DELETE /api/history/all
     */
    @Transactional
    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllHistory(@RequestHeader("Authorization") String token) {
        try {
            User user = getUserFromToken(token);

            long count = analysisLogRepository.countByUserId(user.getId());
            analysisLogRepository.deleteByUserId(user.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of("deletedCount", count),
                    "message", "전체 분석 이력이 삭제되었습니다"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    private User getUserFromToken(String token) {
        String jwt = token.replace("Bearer ", "");
        String username = jwtTokenProvider.getUsernameFromToken(jwt);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }
}
