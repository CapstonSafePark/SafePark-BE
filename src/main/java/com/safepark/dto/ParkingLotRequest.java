package com.safepark.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ParkingLotRequest {

    @NotBlank(message = "주차장명은 필수입니다")
    private String lotName;  // 변경: name → lotName

    private String address;

    @NotNull(message = "위도는 필수입니다")
    private Float lat;  // 변경: latitude → lat

    @NotNull(message = "경도는 필수입니다")
    private Float lng;  // 변경: longitude → lng

    private String lotType;  // 변경: type → lotType (공영/민영/노상)

    private Integer lotPrice;  // 변경: basicFee → lotPrice

    private Integer freeYn;  // 추가: 0=유료, 1=무료

    private String operatingHours;

    private Integer totalSpaces;  // 변경: capacity → totalSpaces

    private Integer availableSpaces;  // 추가: 현재 가능 면수
}