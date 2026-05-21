package com.safepark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyParkingLotResponse {

    private Long id;
    private String lotName;
    private String address;
    private Double lat;
    private Double lng;
    private Integer lotPrice;
    private Boolean freeYn;
    private String operatingHours;
    private String parkingFeeDesc;
    private Integer totalSpaces;
    private Integer availableSpots;
    private Double distanceKm;
    private String source; // "DB" or "의왕시"
    private Integer feeUnit;      // 기본 주차 시간(분)
    private Integer addUnitTime;  // 추가 단위 시간(분)
    private Integer addUnitPrice; // 추가 요금(원)
}
