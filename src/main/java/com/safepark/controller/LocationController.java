package com.safepark.controller;

import com.safepark.entity.CrackZone;
import com.safepark.entity.ParkingLot;
import com.safepark.repository.CrackZoneRepository;
import com.safepark.repository.ParkingLotRepository;
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

    /**
     * 현재 위치 주차 가능 여부 확인
     * POST /api/location/check-parking
     * body: { latitude, longitude, address }
     */
    @PostMapping("/check-parking")
    public ResponseEntity<?> checkParking(@RequestBody Map<String, Object> request) {
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
            if (!nearbyZones.isEmpty()) {
                CrackZone closestZone = nearbyZones.get(0);
                if ("스쿨존".equals(closestZone.getZoneType()) || "버스정류장".equals(closestZone.getZoneType())) {
                    riskLevel = "HIGH";
                    probability = 90;
                } else {
                    riskLevel = "MEDIUM";
                    probability = 60;
                }
            } else {
                riskLevel = "LOW";
                probability = 10;
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

            Map<String, Object> data = new HashMap<>();
            data.put("latitude", latitude);
            data.put("longitude", longitude);
            data.put("address", address);
            data.put("riskLevel", riskLevel);
            data.put("probability", probability);
            data.put("nearbyZones", zoneList);
            data.put("nearbyLots", lotList);

            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
