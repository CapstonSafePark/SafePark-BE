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

            // 위험도 계산
            String riskLevel;
            int probability;
            String reasoning;
            if (!nearbyZones.isEmpty()) {
                CrackZone closestZone = nearbyZones.get(0);
                if ("스쿨존".equals(closestZone.getZoneType()) || "버스정류장".equals(closestZone.getZoneType())) {
                    riskLevel = "HIGH";
                    probability = 90;
                    reasoning = String.format("반경 100m 내 %s(%s) 단속구역이 존재합니다. 과태료 확률이 매우 높습니다.",
                            closestZone.getZoneType(), closestZone.getZoneName());
                } else {
                    riskLevel = "MEDIUM";
                    probability = 60;
                    reasoning = String.format("반경 100m 내 %s(%s) 단속구역이 존재합니다. 주의가 필요합니다.",
                            closestZone.getZoneType(), closestZone.getZoneName());
                }
            } else {
                riskLevel = "LOW";
                probability = 10;
                reasoning = "반경 100m 내 단속구역이 확인되지 않았습니다. 주차 가능성이 높습니다.";
            }

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
            data.put("nearbyZones", zoneList);
            data.put("nearbyLots", lotList);

            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
