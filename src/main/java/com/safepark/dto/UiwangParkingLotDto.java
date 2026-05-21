package com.safepark.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UiwangParkingLotDto {

    private String siteId;
    private String siteName;
    private String latitude;
    private String longitude;
    private Integer capacity;
    private Integer occupancy;
    private String parkingFeeDesc;
    private String operationHourDesc;
    private Boolean isCounting;

    public int getAvailableSpots() {
        if (capacity == null || occupancy == null) return 0;
        return Math.max(0, capacity - occupancy);
    }

    public double getLatDouble() {
        try {
            return Double.parseDouble(latitude);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public double getLngDouble() {
        try {
            return Double.parseDouble(longitude);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
