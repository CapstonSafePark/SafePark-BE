package com.safepark.dto;

public class ParkingLotDetailResponse {

    private Long id;
    private String lotName;
    private String address;
    private Double lat;
    private Double lng;
    private String lotType;
    private Integer lotPrice;
    private Boolean freeYn;
    private String operatingHours;
    private Integer totalSpaces;
    private Integer availableSpaces;

    public ParkingLotDetailResponse(
            Long id,
            String lotName,
            String address,
            Double lat,
            Double lng,
            String lotType,
            Integer lotPrice,
            Boolean freeYn,
            String operatingHours,
            Integer totalSpaces,
            Integer availableSpaces
    ) {
        this.id = id;
        this.lotName = lotName;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.lotType = lotType;
        this.lotPrice = lotPrice;
        this.freeYn = freeYn;
        this.operatingHours = operatingHours;
        this.totalSpaces = totalSpaces;
        this.availableSpaces = availableSpaces;
    }

    public Long getId() {
        return id;
    }

    public String getLotName() {
        return lotName;
    }

    public String getAddress() {
        return address;
    }

    public Double getLat() {
        return lat;
    }

    public Double getLng() {
        return lng;
    }

    public String getLotType() {
        return lotType;
    }

    public Integer getLotPrice() {
        return lotPrice;
    }

    public Boolean getFreeYn() {
        return freeYn;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public Integer getTotalSpaces() {
        return totalSpaces;
    }

    public Integer getAvailableSpaces() {
        return availableSpaces;
    }
}