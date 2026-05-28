package com.safepark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotDetailResponse {

    // 기본 정보
    private Long id;
    private String lotName;
    private String address;
    private Double lat;
    private Double lng;
    private String lotType;      // 민영/공영/노상 등
    private String source;       // DB/의왕시/서울시/경기도/모두의주차장

    // 가용 면수
    private Integer totalSpaces;
    private Integer availableSpaces;

    // 요금 정보
    private Boolean freeYn;
    private Integer lotPrice;        // 기본 요금(원)
    private Integer feeUnit;         // 기본 시간(분)
    private Integer addUnitTime;     // 추가 단위 시간(분)
    private Integer addUnitPrice;    // 추가 요금(원)
    private Integer initialFreeMinutes; // 초기무료(분)
    private Integer dailyMaxFee;     // 1일 최대요금(원)
    private String parkingFeeDesc;   // 요금 설명 문자열

    // 운영시간
    private String operatingHours;   // 통합 운영시간 (레거시)
    private String weekdayHours;     // 평일
    private String saturdayHours;    // 토요일
    private String sundayHours;      // 일요일
    private String holidayHours;     // 공휴일

    /**
     * NearbyParkingLotResponse → ParkingLotDetailResponse 변환
     */
    public static ParkingLotDetailResponse from(NearbyParkingLotResponse r) {
        return ParkingLotDetailResponse.builder()
                .id(r.getId())
                .lotName(r.getLotName())
                .address(r.getAddress())
                .lat(r.getLat())
                .lng(r.getLng())
                .lotType(r.getLotType())
                .source(r.getSource())
                .totalSpaces(r.getTotalSpaces())
                .availableSpaces(r.getAvailableSpots())
                .freeYn(r.getFreeYn())
                .lotPrice(r.getLotPrice())
                .feeUnit(r.getFeeUnit())
                .addUnitTime(r.getAddUnitTime())
                .addUnitPrice(r.getAddUnitPrice())
                .initialFreeMinutes(r.getInitialFreeMinutes())
                .dailyMaxFee(r.getDailyMaxFee())
                .parkingFeeDesc(r.getParkingFeeDesc())
                .operatingHours(r.getOperatingHours())
                .weekdayHours(r.getWeekdayHours())
                .saturdayHours(r.getSaturdayHours())
                .sundayHours(r.getSundayHours())
                .holidayHours(r.getHolidayHours())
                .build();
    }
}
