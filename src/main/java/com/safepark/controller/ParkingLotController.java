package com.safepark.controller;

import com.safepark.dto.ParkingLotDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parking-lots")
public class ParkingLotController {

    @GetMapping("/{parkingLotId}")
    public ParkingLotDetailResponse getParkingLotDetail(@PathVariable Long parkingLotId) {
        return new ParkingLotDetailResponse(
                parkingLotId,
                "의왕역 공영주차장",
                "경기도 의왕시 부곡중앙남3길 2",
                37.3203,
                126.9481,
                "PUBLIC",
                1000,
                false,
                "24시간",
                120,
                35
        );
    }
}