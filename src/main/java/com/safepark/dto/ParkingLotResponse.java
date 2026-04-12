package com.safepark.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotResponse {

    private Long id;
    private String lotName;  // 변경: name → lotName
    private String address;
    private Float lat;  // 변경: latitude → lat
    private Float lng;  // 변경: longitude → lng
    private String lotType;  // 변경: type → lotType
    private Integer lotPrice;  // 변경: basicFee → lotPrice
    private Integer freeYn;  // 추가
    private String operatingHours;
    private Integer totalSpaces;  // 변경: capacity → totalSpaces
    private Integer availableSpaces;  // 추가

    // Entity → DTO 변환 메서드
    public static ParkingLotResponse from(com.safepark.entity.ParkingLot entity) {
        return new ParkingLotResponse(
                entity.getId(),
                entity.getLotName(),
                entity.getAddress(),
                entity.getLat(),
                entity.getLng(),
                entity.getLotType(),
                entity.getLotPrice(),
                entity.getFreeYn(),
                entity.getOperatingHours(),
                entity.getTotalSpaces(),
                entity.getAvailableSpaces()
        );
    }
}