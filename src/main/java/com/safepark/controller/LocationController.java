package com.safepark.controller;

import com.safepark.entity.AnalysisLog;
import com.safepark.entity.CrackZone;
import com.safepark.entity.ParkingLot;
import com.safepark.entity.User;
import com.safepark.repository.AnalysisLogRepository;
import com.safepark.repository.CrackZoneRepository;
import com.safepark.repository.ParkingLotRepository;
import com.safepark.repository.UserRepository;
import com.safepark.security.JwtTokenProvider;
import com.safepark.util.RiskCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final CrackZoneRepository crackZoneRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final AnalysisLogRepository analysisLogRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 현재 위치 주차 가능 여부 확인
     * POST /api/location/check-parking
     * body: { latitude, longitude, address }
     */
    @PostMapping("/check-parking")
    public ResponseEntity<?> checkParking(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, Object> request) {
        try {
            float latitude = Float.parseFloat(request.get("latitude").toString());
            float longitude = Float.parseFloat(request.get("longitude").toString());
            String address = request.getOrDefault("address", "").toString();

            // 반경 0.1km(100m) 내 단속구역 조회
            List<CrackZone> nearbyZones = crackZoneRepository.findNearbyZones(latitude, longitude, 0.1);

            // 반경 0.5km 내 주차장 조회
            List<ParkingLot> nearbyLots = parkingLotRepository.findNearbyLots(latitude, longitude, 0.5);

            // zone + 현재 시각으로 위험도 계산 (우선순위: 스쿨존 > 버스정류장 > 주정차금지 > CCTV > 거리순)
            CrackZone closestZone = nearbyZones.isEmpty() ? null : selectPriorityZone(nearbyZones);
            RiskCalculator.RiskResult risk = RiskCalculator.calculateWithZoneOnly(closestZone);
            String riskLevel = risk.riskLevel;
            int probability = risk.probability;
            String reasoning = risk.reasoning;

            // 단속구역 정보 변환
            List<Map<String, Object>> zoneList = nearbyZones.stream().map(zone -> {
                Map<String, Object> z = new HashMap<>();
                z.put("id", zone.getId());
                z.put("zoneName", zone.getZoneName());
                z.put("zoneType", zone.getZoneType());
                z.put("lat", zone.getLat());
                z.put("lng", zone.getLng());
                z.put("startTime", zone.getStartTime());
                z.put("endTime", zone.getEndTime());
                return z;
            }).toList();

            // 주차장 정보 변환
            List<Map<String, Object>> lotList = nearbyLots.stream().map(lot -> {
                Map<String, Object> l = new HashMap<>();
                l.put("id", lot.getId());
                l.put("lotName", lot.getLotName());
                l.put("address", lot.getAddress());
                l.put("lat", lot.getLat());
                l.put("lng", lot.getLng());
                l.put("lotPrice", lot.getLotPrice());
                l.put("freeYn", lot.getFreeYn());
                l.put("availableSpaces", lot.getAvailableSpaces());
                return l;
            }).toList();

            // 분석 로그 DB 저장
            if (token != null && token.startsWith("Bearer ")) {
                try {
                    String jwt = token.replace("Bearer ", "");
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);
                    User user = userRepository.findByUsername(username).orElse(null);
                    if (user != null) {
                        AnalysisLog log = new AnalysisLog();
                        log.setUserId(user.getId());
                        log.setReqLat(latitude);
                        log.setReqLng(longitude);
                        log.setAddress(address);
                        log.setRiskLevel(riskLevel);
                        log.setProbability(probability);
                        log.setReasoning(reasoning);
                        log.setLineColor("없음");
                        log.setResult("위치 기반 분석 완료");
                        analysisLogRepository.save(log);
                    }
                } catch (Exception ignored) {}
            }

            Map<String, Object> data = new HashMap<>();
            data.put("latitude", latitude);
            data.put("longitude", longitude);
            data.put("address", address);
            data.put("riskLevel", riskLevel);
            data.put("probability", probability);
            data.put("reasoning", reasoning);
            data.put("zoneType", closestZone != null ? closestZone.getZoneType() : null);
            data.put("startTime", closestZone != null ? closestZone.getStartTime() : null);
            data.put("endTime", closestZone != null ? closestZone.getEndTime() : null);
            data.put("nearbyZones", zoneList);
            data.put("nearbyLots", lotList);

            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 반경 내 zone 중 위험도 우선순위가 높은 zone 선택
     * 스쿨존 > 버스정류장 > 주정차금지 > CCTV > 거리순 첫 번째
     */
    private CrackZone selectPriorityZone(List<CrackZone> zones) {
        java.util.Map<String, Integer> priority = new java.util.HashMap<>();
        priority.put("스쿨존", 1);
        priority.put("버스정류장", 2);
        priority.put("주정차금지", 3);
        priority.put("CCTV", 4);
        return zones.stream()
                .min(java.util.Comparator.comparingInt(z -> priority.getOrDefault(z.getZoneType(), 99)))
                .orElse(zones.get(0));
    }
}
