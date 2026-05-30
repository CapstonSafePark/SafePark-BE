package com.safepark.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private String operatingHours;   // 통합 운영시간 문자열 (레거시)
    private String parkingFeeDesc;
    private Integer totalSpaces;
    private Integer availableSpots;
    private Double distanceKm;
    private String source; // "DB", "의왕시", "서울시", "경기도", "모두의주차장"
    private Integer feeUnit;      // 기본 주차 시간(분)
    private Integer addUnitTime;  // 추가 단위 시간(분)
    private Integer addUnitPrice; // 추가 요금(원)

    // 요일별 운영시간
    private String weekdayHours;   // 평일 운영시간 (예: "00:00~24:00")
    private String saturdayHours;  // 토요일 운영시간
    private String sundayHours;    // 일요일 운영시간
    private String holidayHours;   // 공휴일 운영시간

    // 요금 상세
    private Integer initialFreeMinutes; // 초기무료 시간(분)
    private Integer dailyMaxFee;        // 1일 최대요금(원)
    private String lotType;             // 민영/공영/노상 등

    // 모두의주차장 티켓 목록 (종일권, 주간권 등)
    private List<ModuTicketDto> tickets;
}
