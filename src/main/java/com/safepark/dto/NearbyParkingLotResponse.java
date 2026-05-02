package com.safepark.dto;

public class NearbyParkingLotResponse {

    private Long id;
    private String lotName;
    private String address;
    private Double lat;
    private Double lng;
    private Integer lotPrice;
    private Boolean freeYn;
    private Double distanceKm;

    public NearbyParkingLotResponse(
            Long id,
            String lotName,
            String address,
            Double lat,
            Double lng,
            Integer lotPrice,
            Boolean freeYn,
            Double distanceKm
    ) {
        this.id = id;
        this.lotName = lotName;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
        this.lotPrice = lotPrice;
        this.freeYn = freeYn;
        this.distanceKm = distanceKm;
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

    public Integer getLotPrice() {
        return lotPrice;
    }

    public Boolean getFreeYn() {
        return freeYn;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }
}