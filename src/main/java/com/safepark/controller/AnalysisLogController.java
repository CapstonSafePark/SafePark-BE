package com.safepark.controller;

import com.safepark.dto.AnalysisLogResponse;
import com.safepark.entity.AnalysisLog;
import com.safepark.entity.User;
import com.safepark.repository.AnalysisLogRepository;
import com.safepark.repository.UserRepository;
import com.safepark.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisLogController {

    private final AnalysisLogRepository analysisLogRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 주차위반 이미지 업로드 및 분석
     * POST /api/analysis/upload-image
     * multipart: image, latitude, longitude
     */
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(
            @RequestHeader("Authorization") String token,
            @RequestParam("image") MultipartFile image,
            @RequestParam("latitude") Float latitude,
            @RequestParam("longitude") Float longitude
    ) {
        try {
            User user = getUserFromToken(token);

            // 이미지 저장
            String uploadDir = "uploads/analysis/";
            new File(uploadDir).mkdirs();
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            String imagePath = uploadDir + fileName;
            image.transferTo(new File(imagePath));

            // 분석 로그 생성 (AI 분석 결과는 별도 서비스에서 처리)
            AnalysisLog log = new AnalysisLog();
            log.setUserId(user.getId());
            log.setReqLat(latitude);
            log.setReqLng(longitude);
            log.setImagePath(imagePath);
            log.setRiskLevel("MEDIUM");   // AI 분석 전 임시값
            log.setProbability(50);       // AI 분석 전 임시값
            log.setResult("분석 대기 중");

            AnalysisLog saved = analysisLogRepository.save(log);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "analysisId", saved.getId(),
                            "imagePath", saved.getImagePath(),
                            "status", "분석 완료",
                            "message", "이미지가 업로드되었습니다. 분석 결과를 확인하세요."
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 분석 결과 상세 조회
     * GET /api/analysis/{analysisId}
     */
    @GetMapping("/{analysisId}")
    public ResponseEntity<?> getAnalysisResult(
            @RequestHeader("Authorization") String token,
            @PathVariable Long analysisId
    ) {
        try {
            User user = getUserFromToken(token);

            AnalysisLog log = analysisLogRepository.findById(analysisId)
                    .orElseThrow(() -> new RuntimeException("분석 결과를 찾을 수 없습니다"));

            if (!log.getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "접근 권한이 없습니다"));
            }

            return ResponseEntity.ok(Map.of("success", true, "data", AnalysisLogResponse.fromEntity(log)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 최근 분석 결과 목록
     * GET /api/analysis/recent?limit=10
     */
    @GetMapping("/recent")
    public ResponseEntity<?> getRecentAnalysis(
            @RequestHeader("Authorization") String token,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            User user = getUserFromToken(token);

            List<AnalysisLog> logs = analysisLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            List<AnalysisLogResponse> result = logs.stream()
                    .limit(limit)
                    .map(AnalysisLogResponse::fromEntitySimple)
                    .toList();

            return ResponseEntity.ok(Map.of("success", true, "data", result));
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
