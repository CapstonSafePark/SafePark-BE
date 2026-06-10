package com.safepark.controller;

import com.safepark.dto.AnalysisLogResponse;
import com.safepark.entity.AnalysisLog;
import com.safepark.entity.CrackZone;
import com.safepark.entity.User;
import com.safepark.repository.AnalysisLogRepository;
import com.safepark.repository.CrackZoneRepository;
import com.safepark.repository.UserRepository;
import com.safepark.security.JwtTokenProvider;
import com.safepark.service.DsAnalysisService;
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
    private final CrackZoneRepository crackZoneRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final DsAnalysisService dsAnalysisService;

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

            // 이미지 저장 (절대경로로 저장, 상대경로를 DB/응답에 사용)
            String uploadDir = System.getProperty("user.dir") + "/uploads/analysis/";
            new File(uploadDir).mkdirs();
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            File savedFile = new File(uploadDir + fileName);
            String imagePath = "/uploads/analysis/" + fileName; // 상대경로
            image.transferTo(savedFile);

            // DS 모델에 이미지 분석 요청
            Map<String, Object> dsResult = dsAnalysisService.analyzeImage(savedFile);

            String lineColor = (String) dsResult.get("lineColor");
            String riskLevel = (String) dsResult.get("riskLevel");
            int probability = (int) dsResult.get("probability");
            String reasoning = (String) dsResult.get("reasoning");

            // 분석 로그 생성 (DS 모델 분석 결과 반영)
            AnalysisLog log = new AnalysisLog();
            log.setUserId(user.getId());
            log.setReqLat(latitude);
            log.setReqLng(longitude);
            log.setImagePath(imagePath);
            log.setRiskLevel(riskLevel);
            log.setProbability(probability);
            log.setLineColor(lineColor);
            log.setReasoning(reasoning);
            log.setResult("분석 완료");

            // 가장 가까운 단속구역 조회 (반경 0.1km = 100m)
            List<CrackZone> nearbyZones = crackZoneRepository.findNearbyZones(latitude, longitude, 0.1);
            if (!nearbyZones.isEmpty()) {
                CrackZone nearest = nearbyZones.get(0);
                log.setZoneId(nearest.getId());
                log.setZoneName(nearest.getZoneName());
                log.setZoneType(nearest.getZoneType());
                log.setStartTime(nearest.getStartTime());
                log.setEndTime(nearest.getEndTime());
            }

            AnalysisLog saved = analysisLogRepository.save(log);

            Map<String, Object> data = new HashMap<>();
            data.put("analysisId", saved.getId());
            data.put("imagePath", saved.getImagePath());
            data.put("probability", saved.getProbability());
            data.put("riskLevel", saved.getRiskLevel());
            data.put("lineColor", saved.getLineColor());
            data.put("reasoning", saved.getReasoning());
            data.put("startTime", saved.getStartTime());
            data.put("endTime", saved.getEndTime());
            data.put("zoneId", saved.getZoneId());
            data.put("zoneName", saved.getZoneName());
            data.put("zoneType", saved.getZoneType());
            data.put("status", "분석 완료");
            data.put("message", "이미지가 업로드되었습니다. 분석 결과를 확인하세요.");

            return ResponseEntity.ok(Map.of("success", true, "data", data));
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

    /**
     * 분석 이력 목록 조회 (FE 호환용)
     * GET /api/analysis/history
     */
    @GetMapping("/history")
    public ResponseEntity<?> getAnalysisHistory(
            @RequestHeader("Authorization") String token
    ) {
        try {
            User user = getUserFromToken(token);
            List<AnalysisLog> logs = analysisLogRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            List<AnalysisLogResponse> result = logs.stream()
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
