package com.safepark.controller;

import com.safepark.dto.NearbyParkingLotResponse;
import com.safepark.dto.ParkingCheckResponse;
import com.safepark.dto.ParkingLotDetailResponse;
import com.safepark.service.ParkingCheckService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/parking-lots")
public class ParkingLotController {

    private final ParkingCheckService parkingCheckService;

    public ParkingLotController(ParkingCheckService parkingCheckService) {
        this.parkingCheckService = parkingCheckService;
    }

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

    @GetMapping("/nearby")
    public List<NearbyParkingLotResponse> getNearbyParkingLots(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radius
    ) {
        List<NearbyParkingLotResponse> parkingLots = new ArrayList<>();

        parkingLots.add(new NearbyParkingLotResponse(
                1L,
                "의왕역 공영주차장",
                "경기도 의왕시 부곡중앙남3길 2",
                37.3203,
                126.9481,
                1000,
                false,
                calculateDistance(latitude, longitude, 37.3203, 126.9481)
        ));

        parkingLots.add(new NearbyParkingLotResponse(
                2L,
                "오전동 임시주차장",
                "경기도 의왕시 오전동 123-4",
                37.3275,
                126.9682,
                0,
                true,
                calculateDistance(latitude, longitude, 37.3275, 126.9682)
        ));

        parkingLots.add(new NearbyParkingLotResponse(
                3L,
                "내손동 공영주차장",
                "경기도 의왕시 내손동 456-7",
                37.3791,
                126.9804,
                500,
                false,
                calculateDistance(latitude, longitude, 37.3791, 126.9804)
        ));

        List<NearbyParkingLotResponse> result = new ArrayList<>();
        for (NearbyParkingLotResponse lot : parkingLots) {
            if (lot.getDistanceKm() <= radius) {
                result.add(lot);
            }
        }

        return result;
    }

    @GetMapping("/check")
    public ParkingCheckResponse checkParkingAvailability(
            @RequestParam Double lat,
            @RequestParam Double lng
    ) {
        return parkingCheckService.checkParkingAvailability(lat, lng);
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