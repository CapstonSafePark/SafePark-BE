package com.safepark.service;

import com.safepark.dto.NearbyParkingLotResponse;
import com.safepark.dto.ParkingCheckResponse;
import com.safepark.util.DistanceUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ParkingCheckService {

    public ParkingCheckResponse checkParkingAvailability(Double lat, Double lng) {

        double nearestCrackZoneDistanceKm = 0.12;

        String riskLevel;
        int probability;
        String reasoning;

        if (nearestCrackZoneDistanceKm <= 0.05) {
            riskLevel = "HIGH";
            probability = 90;
            reasoning = "현재 위치가 단속 또는 주정차 금지 구역과 매우 가깝습니다.";
        } else if (nearestCrackZoneDistanceKm <= 0.2) {
            riskLevel = "MEDIUM";
            probability = 60;
            reasoning = "현재 위치 주변에 단속 또는 주정차 금지 구역이 존재합니다.";
        } else {
            riskLevel = "LOW";
            probability = 20;
            reasoning = "현재 위치 주변에 가까운 단속 구역이 확인되지 않았습니다.";
        }

        List<NearbyParkingLotResponse> nearbyParkingLots = List.of(
                        NearbyParkingLotResponse.builder()
                                .id(1L).lotName("의왕역 공영주차장").address("경기도 의왕시 부곡중앙남3길 2")
                                .lat(37.3203).lng(126.9481).lotPrice(1000).freeYn(false)
                                .distanceKm(DistanceUtils.calculateDistanceKm(lat, lng, 37.3203, 126.9481))
                                .source("DB").build(),

                        NearbyParkingLotResponse.builder()
                                .id(2L).lotName("오전동 임시주차장").address("경기도 의왕시 오전동 123-4")
                                .lat(37.3275).lng(126.9682).lotPrice(0).freeYn(true)
                                .distanceKm(DistanceUtils.calculateDistanceKm(lat, lng, 37.3275, 126.9682))
                                .source("DB").build(),

                        NearbyParkingLotResponse.builder()
                                .id(3L).lotName("내손동 공영주차장").address("경기도 의왕시 내손동 456-7")
                                .lat(37.3791).lng(126.9804).lotPrice(500).freeYn(false)
                                .distanceKm(DistanceUtils.calculateDistanceKm(lat, lng, 37.3791, 126.9804))
                                .source("DB").build()
                )
                .stream()
                .sorted(Comparator.comparing(NearbyParkingLotResponse::getDistanceKm))
                .limit(3)
                .toList();

        return new ParkingCheckResponse(
                lat,
                lng,
                riskLevel,
                probability,
                reasoning,
                nearbyParkingLots
        );
    }
}