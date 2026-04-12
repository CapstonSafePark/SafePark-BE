package com.safepark.controller;

import com.safepark.entity.ParkingLot;
import com.safepark.service.ParkingLotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    // 관리자: 주차장 등록
    @PostMapping("/admin/parking-lots")
    public ResponseEntity<?> createParkingLot(@RequestBody ParkingLot parkingLot) {
        try {
            ParkingLot created = parkingLotService.createParkingLot(parkingLot);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", created);

            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.status(500).body(error);
        }
    }

    // 관리자: 주차장 수정
    @PutMapping("/admin/parking-lots/{id}")
    public ResponseEntity<?> updateParkingLot(@PathVariable Long id,
                                              @RequestBody ParkingLot parkingLot) {
        try {
            ParkingLot updated = parkingLotService.updateParkingLot(id, parkingLot);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", updated);

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.status(404).body(error);
        }
    }

    // 관리자: 주차장 삭제
    @DeleteMapping("/admin/parking-lots/{id}")
    public ResponseEntity<?> deleteParkingLot(@PathVariable Long id) {
        try {
            parkingLotService.deleteParkingLot(id);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "주차장이 삭제되었습니다");

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.status(404).body(error);
        }
    }

    // 사용자: 주차장 목록 조회
    @GetMapping("/parking-lots")
    public ResponseEntity<?> getAllParkingLots() {
        try {
            List<ParkingLot> parkingLots = parkingLotService.getAllParkingLots();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", parkingLots);
            result.put("count", parkingLots.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.status(500).body(error);
        }
    }

    // 사용자: 주차장 상세 조회
    @GetMapping("/parking-lots/{id}")
    public ResponseEntity<?> getParkingLotById(@PathVariable Long id) {
        try {
            ParkingLot parkingLot = parkingLotService.getParkingLotById(id);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", parkingLot);

            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.status(404).body(error);
        }
    }

    // 사용자: 주차장 검색
    @GetMapping("/parking-lots/search")
    public ResponseEntity<?> searchParkingLots(@RequestParam String keyword) {
        try {
            List<ParkingLot> parkingLots = parkingLotService.searchParkingLots(keyword);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("data", parkingLots);
            result.put("count", parkingLots.size());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());

            return ResponseEntity.status(500).body(error);
        }
    }
}