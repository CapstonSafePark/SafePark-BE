package com.safepark.dto;

import java.util.List;

public record ParkingCheckResponse(
        Double reqLat,
        Double reqLng,
        String riskLevel,
        Integer probability,
        String reasoning,
        List<NearbyParkingLotResponse> nearbyParkingLots
) {
}