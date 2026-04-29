package com.safepark.controller;

import com.safepark.dto.ApiResponse;
import com.safepark.dto.ParkingLotResponse;
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
}