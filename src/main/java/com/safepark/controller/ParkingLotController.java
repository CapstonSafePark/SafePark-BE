package com.safepark.controller;

import com.safepark.dto.ApiResponse;
import com.safepark.dto.NearbyParkingLotResponse;
import com.safepark.dto.ParkingCheckResponse;
import com.safepark.dto.ParkingLotDetailResponse;
import com.safepark.dto.ParkingLotResponse;
import com.safepark.entity.ParkingLot;
import com.safepark.repository.ParkingLotRepository;
import com.safepark.service.GyeonggiParkingApiService;
import com.safepark.service.ModuParkingApiService;
import com.safepark.service.ParkingCheckService;
import com.safepark.service.ParkingLotService;
import com.safepark.service.SeoulParkingApiService;
import com.safepark.service.UiwangParkingApiService;
import com.safepark.util.DistanceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/parking-lots")
@RequiredArgsConstructor
public class ParkingLotController {

    private final ParkingLotService parkingLotService;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingCheckService parkingCheckService;
    private final UiwangParkingApiService uiwangParkingApiService;
    private final SeoulParkingApiService seoulParkingApiService;
    private final GyeonggiParkingApiService gyeonggiParkingApiService;
    private final ModuParkingApiService moduParkingApiService;

    // 주차장 목록 조회
    @GetMapping
    public ResponseEntity<?> getAllParkingLots() {
        List<ParkingLotResponse> parkingLots = parkingLotService.getAllParkingLots();
        return ResponseEntity.ok(new ApiResponse<>(true, parkingLots));
    }

    // 주차장 상세 조회 (DB 저장 주차장)
    @GetMapping("/{id}")
    public ResponseEntity<?> getParkingLotById(@PathVariable Long id) {
        ParkingLotResponse parkingLot = parkingLotService.getParkingLotById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, parkingLot));
    }

    /**
     * 주차장 상세 조회 (외부 API 포함)
     * FE에서 nearby 응답의 lat/lng/lotName 기반으로 상세 정보 조회
     * GET /api/parking-lots/detail?lat={lat}&lng={lng}&name={name}
     */
    @GetMapping("/detail")
    public ResponseEntity<?> getParkingLotDetail(
            @RequestParam float lat,
            @RequestParam float lng,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0.3") double radius
    ) {
        try {
            List<NearbyParkingLotResponse> result = new ArrayList<>();

            // DB 주차장 우선 조회
            List<ParkingLot> dbLots = parkingLotRepository.findNearbyLots(lat, lng, radius);
            for (ParkingLot lot : dbLots) {
                double dist = DistanceUtils.calculateDistanceKm(lat, lng, lot.getLat(), lot.getLng());
                result.add(NearbyParkingLotResponse.builder()
                        .id(lot.getId())
                        .lotName(lot.getLotName())
                        .address(lot.getAddress())
                        .lat(lot.getLat() != null ? lot.getLat().doubleValue() : null)
                        .lng(lot.getLng() != null ? lot.getLng().doubleValue() : null)
                        .lotType(lot.getLotType())
                        .source("DB")
                        .freeYn(lot.getFreeYn() != null ? lot.getFreeYn() == 1 : null)
                        .lotPrice(lot.getLotPrice())
                        .feeUnit(lot.getFeeUnit())
                        .addUnitTime(lot.getAddUnitTime())
                        .addUnitPrice(lot.getAddUnitPrice())
                        .totalSpaces(lot.getTotalSpaces())
                        .availableSpots(lot.getAvailableSpaces())
                        .operatingHours(lot.getOperatingHours())
                        .distanceKm(dist)
                        .build());
            }

            // 이름이나 좌표로 매칭되는 주차장 찾기
            NearbyParkingLotResponse matched = result.stream()
                    .filter(r -> {
                        if (name != null && r.getLotName() != null) {
                            return r.getLotName().replaceAll("\\s", "")
                                    .equals(name.replaceAll("\\s", ""));
                        }
                        return r.getDistanceKm() != null && r.getDistanceKm() <= 0.05; // 50m 이내
                    })
                    .findFirst()
                    .orElse(result.isEmpty() ? null : result.get(0));

            if (matched == null) {
                return ResponseEntity.notFound().build();
            }

            // 경기도 실시간 가용면수 보완
            if (matched.getAvailableSpots() == null && matched.getLotName() != null) {
                Map<String, Integer> gyeonggiAvailMap = gyeonggiParkingApiService
                        .getRealtimeAvailableMap(lat, lng, radius);
                String nameKey = matched.getLotName().replaceAll("\\s", "");
                Integer avail = gyeonggiAvailMap.get(nameKey);
                if (avail != null) matched.setAvailableSpots(avail);
            }

            return ResponseEntity.ok(new ApiResponse<>(true, ParkingLotDetailResponse.from(matched)));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    // 주차장 검색
    @GetMapping("/search")
    public ResponseEntity<?> searchParkingLots(@RequestParam String keyword) {
        List<ParkingLotResponse> parkingLots = parkingLotService.searchParkingLots(keyword);
        return ResponseEntity.ok(new ApiResponse<>(true, parkingLots));
    }

    // 주변 주차장 검색 (DB + 의왕시 실시간 API 통합)
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyParkingLots(
            @RequestParam float latitude,
            @RequestParam float longitude,
            @RequestParam(defaultValue = "1.0") double radius
    ) {
        try {
            List<NearbyParkingLotResponse> result = new ArrayList<>();

            // 1) DB에 저장된 주차장 조회
            List<ParkingLot> dbLots = parkingLotRepository.findNearbyLots(latitude, longitude, radius);

            // 1-1) 경기도 실시간 가용면수 map (DB 주차장 enrichment용)
            Map<String, Integer> gyeonggiAvailMap = gyeonggiParkingApiService.getRealtimeAvailableMap(latitude, longitude, radius);

            for (ParkingLot lot : dbLots) {
                double dist = DistanceUtils.calculateDistanceKm(latitude, longitude, lot.getLat(), lot.getLng());
                // 실시간 가용면수: DB값 우선, 없으면 경기도 API로 보완
                Integer availableSpots = lot.getAvailableSpaces();
                if (availableSpots == null && lot.getLotName() != null) {
                    String nameKey = lot.getLotName().replaceAll("\\s", "");
                    availableSpots = gyeonggiAvailMap.get(nameKey);
                }
                result.add(NearbyParkingLotResponse.builder()
                        .id(lot.getId())
                        .lotName(lot.getLotName())
                        .address(lot.getAddress())
                        .lat(lot.getLat() != null ? lot.getLat().doubleValue() : null)
                        .lng(lot.getLng() != null ? lot.getLng().doubleValue() : null)
                        .lotPrice(lot.getLotPrice())
                        .freeYn(lot.getFreeYn() != null ? lot.getFreeYn() == 1 : null)
                        .operatingHours(lot.getOperatingHours())
                        .parkingFeeDesc(null)
                        .totalSpaces(lot.getTotalSpaces())
                        .availableSpots(availableSpots)
                        .distanceKm(Math.round(dist * 1000.0) / 1000.0)
                        .source("DB")
                        .feeUnit(lot.getFeeUnit())
                        .addUnitTime(lot.getAddUnitTime())
                        .addUnitPrice(lot.getAddUnitPrice())
                        .build());
            }

            // 2) 의왕시 실시간 API 주차장 추가
            result.addAll(uiwangParkingApiService.getNearbyLots(latitude, longitude, radius));

            // 3) 서울시 실시간 API 주차장 추가
            result.addAll(seoulParkingApiService.getNearbyLots(latitude, longitude, radius));

            // 4) 경기도 실시간 API 주차장 추가
            result.addAll(gyeonggiParkingApiService.getNearbyLots(latitude, longitude, radius));

            // 5) 모두의주차장 API 주차장 추가
            result.addAll(moduParkingApiService.getNearbyLots(latitude, longitude, radius));

            // 6) 소스 우선순위 정렬 (실시간 데이터 우선, DB가 경기도 JSON보다 우선) → 중복 제거에 사용
            Map<String, Integer> sourcePriority = Map.of("의왕시", 1, "서울시", 2, "DB", 3, "경기도", 4, "모두의주차장", 5);
            result.sort(Comparator
                    .comparingInt((NearbyParkingLotResponse r) -> sourcePriority.getOrDefault(r.getSource(), 99))
                    .thenComparingDouble(NearbyParkingLotResponse::getDistanceKm));

            // 7) 좌표 기반 중복 제거 (100m 이내 = 같은 주차장, 우선순위 높은 쪽 유지)
            // DB lot이 탈락할 때, 요금/운영시간 정보를 실시간 lot에 보완
            List<NearbyParkingLotResponse> deduplicated = new ArrayList<>();
            for (NearbyParkingLotResponse candidate : result) {
                boolean isDuplicate = false;
                for (NearbyParkingLotResponse existing : deduplicated) {
                    if (existing.getLat() == null || existing.getLng() == null
                            || candidate.getLat() == null || candidate.getLng() == null) continue;
                    double dist = DistanceUtils.calculateDistanceKm(
                            existing.getLat(), existing.getLng(),
                            candidate.getLat(), candidate.getLng());
                    boolean sameName = existing.getLotName() != null && candidate.getLotName() != null
                            && existing.getLotName().replaceAll("\\s", "")
                               .equals(candidate.getLotName().replaceAll("\\s", ""));
                    if (dist <= 0.1 || sameName) { // 100m 이내 또는 이름 동일
                        isDuplicate = true;
                        // DB lot의 요금/운영시간 정보로 실시간 lot 보완
                        if ("DB".equals(candidate.getSource())) {
                            if (existing.getLotPrice() == null && candidate.getLotPrice() != null)
                                existing.setLotPrice(candidate.getLotPrice());
                            if (existing.getFreeYn() == null && candidate.getFreeYn() != null)
                                existing.setFreeYn(candidate.getFreeYn());
                            if (existing.getOperatingHours() == null && candidate.getOperatingHours() != null)
                                existing.setOperatingHours(candidate.getOperatingHours());
                            if (existing.getFeeUnit() == null && candidate.getFeeUnit() != null)
                                existing.setFeeUnit(candidate.getFeeUnit());
                            if (existing.getAddUnitTime() == null && candidate.getAddUnitTime() != null)
                                existing.setAddUnitTime(candidate.getAddUnitTime());
                            if (existing.getAddUnitPrice() == null && candidate.getAddUnitPrice() != null)
                                existing.setAddUnitPrice(candidate.getAddUnitPrice());
                        }
                        break;
                    }
                }
                if (!isDuplicate) deduplicated.add(candidate);
            }

            // 8) 이름 기반 요금 보완 (좌표 매칭 실패한 주차장 대상)
            // 의왕시/경기도 API 주차장 중 fee 없는 경우, DB lot 이름으로 한 번 더 시도
            Map<String, ParkingLot> dbLotByName = new java.util.HashMap<>();
            for (ParkingLot lot : dbLots) {
                if (lot.getLotName() != null) {
                    dbLotByName.put(lot.getLotName().replaceAll("\\s", ""), lot);
                }
            }
            for (NearbyParkingLotResponse r : deduplicated) {
                if (r.getLotPrice() != null || Boolean.TRUE.equals(r.getFreeYn())) continue;
                if (r.getLotName() == null) continue;
                ParkingLot dbLot = dbLotByName.get(r.getLotName().replaceAll("\\s", ""));
                if (dbLot == null) continue;
                if (dbLot.getLotPrice() != null) r.setLotPrice(dbLot.getLotPrice());
                if (dbLot.getFreeYn() != null) r.setFreeYn(dbLot.getFreeYn() == 1);
                if (r.getOperatingHours() == null && dbLot.getOperatingHours() != null)
                    r.setOperatingHours(dbLot.getOperatingHours());
                if (r.getFeeUnit() == null && dbLot.getFeeUnit() != null)
                    r.setFeeUnit(dbLot.getFeeUnit());
                if (r.getAddUnitTime() == null && dbLot.getAddUnitTime() != null)
                    r.setAddUnitTime(dbLot.getAddUnitTime());
                if (r.getAddUnitPrice() == null && dbLot.getAddUnitPrice() != null)
                    r.setAddUnitPrice(dbLot.getAddUnitPrice());
            }

            // 9) 최종 거리순 정렬
            deduplicated.sort(Comparator.comparingDouble(NearbyParkingLotResponse::getDistanceKm));

            return ResponseEntity.ok(new ApiResponse<>(true, deduplicated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    // 현재 위치 주차 가능 여부 확인
    @GetMapping("/check")
    public ResponseEntity<?> checkParkingAvailability(
            @RequestParam Double lat,
            @RequestParam Double lng
    ) {
        ParkingCheckResponse response = parkingCheckService.checkParkingAvailability(lat, lng);
        return ResponseEntity.ok(new ApiResponse<>(true, response));
    }

}