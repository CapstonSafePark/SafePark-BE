package com.safepark.controller;

import com.safepark.dto.ApiResponse;
import com.safepark.dto.NearbyParkingLotResponse;
import com.safepark.dto.ParkingCheckResponse;
import com.safepark.dto.ParkingLotDetailResponse;
import com.safepark.dto.ParkingLotResponse;
import com.safepark.entity.ParkingLot;
import com.safepark.repository.ParkingLotRepository;
import com.safepark.service.ParkingCheckService;
import com.safepark.service.ParkingLotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parking-lots")
@RequiredArgsConstructor
public class ParkingLotController {

    private final ParkingLotService parkingLotService;
    private final ParkingLotRepository parkingLotRepository;
    private final ParkingCheckService parkingCheckService;

    // 주차장 목록 조회
    @GetMapping
    public ResponseEntity<?> getAllParkingLots() {
        List<ParkingLotResponse> parkingLots = parkingLotService.getAllParkingLots();
        return ResponseEntity.ok(new ApiResponse<>(true, parkingLots));
    }

    // 주차장 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getParkingLotById(@PathVariable Long id) {
        ParkingLotResponse parkingLot = parkingLotService.getParkingLotById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, parkingLot));
    }

    // 주차장 검색
    @GetMapping("/search")
    public ResponseEntity<?> searchParkingLots(@RequestParam String keyword) {
        List<ParkingLotResponse> parkingLots = parkingLotService.searchParkingLots(keyword);
        return ResponseEntity.ok(new ApiResponse<>(true, parkingLots));
    }

    // 주변 주차장 검색
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyParkingLots(
            @RequestParam float latitude,
            @RequestParam float longitude,
            @RequestParam(defaultValue = "1.0") double radius
    ) {
        try {
            List<ParkingLot> lots = parkingLotRepository.findNearbyLots(latitude, longitude, radius);
            List<ParkingLotResponse> result = lots.stream()
                    .map(ParkingLotResponse::from)
                    .toList();
            return ResponseEntity.ok(new ApiResponse<>(true, result));
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

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}