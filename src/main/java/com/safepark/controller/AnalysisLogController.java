package com.safepark.controller;

import com.safepark.dto.AnalysisLogResponse;
import com.safepark.entity.AnalysisLog;
import com.safepark.entity.CrackZone;
import com.safepark.entity.User;
import com.safepark.repository.AnalysisLogRepository;
import com.safepark.repository.CrackZoneRepository;
import com.safepark.repository.UserRepository;
import com.safepark.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
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
            String uploadDir = System.getProperty("user.home") + "/uploads/analysis/";
            new File(uploadDir).mkdirs();
            String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
            String imagePath = uploadDir + fileName;
            image.transferTo(new File(imagePath));

            // DS 서버 호출 (차선 분석)
            String lineColor = null;
            try {
                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("image", new FileSystemResource(new File(imagePath)));
                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                Map dsResponse = restTemplate.postForObject(
                        "http://localhost:5000/ds/line-detect", requestEntity, Map.class);
                if (dsResponse != null && dsResponse.containsKey("line_type")) {
                    lineColor = dsResponse.get("line_type").toString();
                }
            } catch (Exception ignored) {
                // DS 서버 호출 실패해도 룰 기반으로 계속 진행
            }

            // 룰 기반 위험도 분석
            // 반경 0.03km(30m) 내 SCHOOL_ZONE, BUS_STOP 조회
            List<CrackZone> nearZones30m = crackZoneRepository.findNearbyZones(latitude, longitude, 0.03);
            // 반경 0.1km(100m) 내 CCTV 조회
            List<CrackZone> nearZones100m = crackZoneRepository.findNearbyZones(latitude, longitude, 0.1);

            String riskLevel;
            int probability;
            String result;
            CrackZone closestZone = null;

            boolean hasSchoolOrBus = nearZones30m.stream()
                    .anyMatch(z -> "SCHOOL_ZONE".equals(z.getZoneType()) || "BUS_STOP".equals(z.getZoneType()));
            boolean hasCctv = nearZones100m.stream()
                    .anyMatch(z -> "CCTV".equals(z.getZoneType()));

            // 차선 + 위치 조합 분석
            // yellow_double: 황색 이중선 - 절대 주정차 금지
            // yellow_single: 황색 단선 - 주정차 금지 (시간제 가능)
            // yellow_dashed: 황색 점선 - 주차 금지, 정차만 가능
            // 그 외(흰선/null): 제한 없음 → 위치로만 판단
            if ("yellow_double".equals(lineColor)) {
                riskLevel = "HIGH";
                probability = 95;
                result = "황색 이중선 구역입니다. 주정차가 절대 금지됩니다.";
                closestZone = nearZones30m.isEmpty() ? null : nearZones30m.get(0);
            } else if ("yellow_single".equals(lineColor)) {
                if (hasSchoolOrBus) {
                    riskLevel = "HIGH";
                    probability = 92;
                    result = "황색 단선 + 주정차 금지구역입니다. 단속 위험이 매우 높습니다.";
                    closestZone = nearZones30m.stream()
                            .filter(z -> "SCHOOL_ZONE".equals(z.getZoneType()) || "BUS_STOP".equals(z.getZoneType()))
                            .findFirst().orElse(null);
                } else if (hasCctv) {
                    riskLevel = "HIGH";
                    probability = 85;
                    result = "황색 단선 구역이며 근처에 단속카메라가 있습니다.";
                    closestZone = nearZones100m.stream()
                            .filter(z -> "CCTV".equals(z.getZoneType()))
                            .findFirst().orElse(null);
                } else {
                    riskLevel = "HIGH";
                    probability = 80;
                    result = "황색 단선 구역입니다. 주정차가 금지됩니다.";
                }
            } else if ("yellow_dashed".equals(lineColor)) {
                if (hasSchoolOrBus) {
                    riskLevel = "HIGH";
                    probability = 88;
                    result = "황색 점선 + 주정차 금지구역입니다. 주차는 절대 금지됩니다.";
                    closestZone = nearZones30m.stream()
                            .filter(z -> "SCHOOL_ZONE".equals(z.getZoneType()) || "BUS_STOP".equals(z.getZoneType()))
                            .findFirst().orElse(null);
                } else if (hasCctv) {
                    riskLevel = "MEDIUM";
                    probability = 70;
                    result = "황색 점선 구역입니다. 주차는 금지되며 근처에 단속카메라가 있습니다.";
                    closestZone = nearZones100m.stream()
                            .filter(z -> "CCTV".equals(z.getZoneType()))
                            .findFirst().orElse(null);
                } else {
                    riskLevel = "MEDIUM";
                    probability = 55;
                    result = "황색 점선 구역입니다. 주차는 금지되나 정차는 가능합니다.";
                }
            } else {
                // 흰선 또는 차선 미감지 → 위치 기반으로만 판단
                if (hasSchoolOrBus) {
                    riskLevel = "HIGH";
                    probability = 90;
                    result = "주정차 금지구역 근처입니다. 단속 위험이 매우 높습니다.";
                    closestZone = nearZones30m.stream()
                            .filter(z -> "SCHOOL_ZONE".equals(z.getZoneType()) || "BUS_STOP".equals(z.getZoneType()))
                            .findFirst().orElse(null);
                } else if (hasCctv) {
                    riskLevel = "MEDIUM";
                    probability = 60;
                    result = "근처에 무인단속카메라가 있습니다. 주의가 필요합니다.";
                    closestZone = nearZones100m.stream()
                            .filter(z -> "CCTV".equals(z.getZoneType()))
                            .findFirst().orElse(null);
                } else {
                    riskLevel = "LOW";
                    probability = 20;
                    result = "주변에 단속구역이 없습니다. 비교적 안전한 구역입니다.";
                }
            }

            // 판단 근거 텍스트 생성
            StringBuilder reasoning = new StringBuilder();
            reasoning.append("위치(").append(latitude).append(", ").append(longitude).append(") 분석 결과. ");
            if (lineColor != null) {
                String lineDesc = switch (lineColor) {
                    case "yellow_double" -> "황색 이중선(절대 주정차 금지)";
                    case "yellow_single" -> "황색 단선(주정차 금지)";
                    case "yellow_dashed" -> "황색 점선(주차 금지)";
                    default -> lineColor;
                };
                reasoning.append("차선: ").append(lineDesc).append(". ");
            }
            if (closestZone != null) {
                String zoneTypeName = switch (closestZone.getZoneType()) {
                    case "SCHOOL_ZONE" -> "어린이보호구역(스쿨존)";
                    case "BUS_STOP" -> "버스정류장";
                    case "CCTV" -> "무인단속카메라";
                    default -> closestZone.getZoneType();
                };
                reasoning.append("반경 내 ").append(zoneTypeName);
                if (closestZone.getZoneName() != null) {
                    reasoning.append("(").append(closestZone.getZoneName()).append(")");
                }
                reasoning.append(" 감지.");
            } else {
                reasoning.append("반경 내 단속구역 없음.");
            }
            reasoning.append(" 과태료 확률: ").append(probability).append("%.");

            // 분석 로그 생성
            AnalysisLog log = new AnalysisLog();
            log.setUserId(user.getId());
            log.setReqLat(latitude);
            log.setReqLng(longitude);
            log.setImagePath(imagePath);
            log.setRiskLevel(riskLevel);
            log.setProbability(probability);
            log.setResult(result);
            log.setLineColor(lineColor);
            log.setReasoning(reasoning.toString());
            if (closestZone != null) {
                log.setZoneId(closestZone.getId());
                log.setZoneName(closestZone.getZoneName());
                log.setZoneType(closestZone.getZoneType());
            }

            AnalysisLog saved = analysisLogRepository.save(log);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "analysisId", saved.getId(),
                            "imagePath", saved.getImagePath(),
                            "riskLevel", saved.getRiskLevel(),
                            "probability", saved.getProbability(),
                            "result", saved.getResult()
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
