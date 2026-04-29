package com.safepark.service;

import com.safepark.dto.ParkingLotRequest;
import com.safepark.dto.ParkingLotResponse;
import com.safepark.entity.ParkingLot;
import com.safepark.repository.ParkingLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;

    // 주차장 등록
    @Transactional
    public ParkingLotResponse createParkingLot(ParkingLotRequest request) {
        ParkingLot parkingLot = new ParkingLot();
        parkingLot.setLotName(request.getLotName());
        parkingLot.setAddress(request.getAddress());
        parkingLot.setLat(request.getLat());
        parkingLot.setLng(request.getLng());
        parkingLot.setLotType(request.getLotType());
        parkingLot.setLotPrice(request.getLotPrice());
        parkingLot.setFreeYn(request.getFreeYn());
        parkingLot.setOperatingHours(request.getOperatingHours());
        parkingLot.setTotalSpaces(request.getTotalSpaces());
        parkingLot.setAvailableSpaces(request.getAvailableSpaces());

        ParkingLot saved = parkingLotRepository.save(parkingLot);
        return ParkingLotResponse.from(saved);
    }

    // 주차장 수정
    @Transactional
    public ParkingLotResponse updateParkingLot(Long id, ParkingLotRequest request) {
        ParkingLot parkingLot = parkingLotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("주차장을 찾을 수 없습니다"));

        parkingLot.setLotName(request.getLotName());
        parkingLot.setAddress(request.getAddress());
        parkingLot.setLat(request.getLat());
        parkingLot.setLng(request.getLng());
        parkingLot.setLotType(request.getLotType());
        parkingLot.setLotPrice(request.getLotPrice());
        parkingLot.setFreeYn(request.getFreeYn());
        parkingLot.setOperatingHours(request.getOperatingHours());
        parkingLot.setTotalSpaces(request.getTotalSpaces());
        parkingLot.setAvailableSpaces(request.getAvailableSpaces());

        ParkingLot updated = parkingLotRepository.save(parkingLot);
        return ParkingLotResponse.from(updated);
    }

    // 주차장 삭제
    @Transactional
    public void deleteParkingLot(Long id) {
        if (!parkingLotRepository.existsById(id)) {
            throw new RuntimeException("주차장을 찾을 수 없습니다");
        }
        parkingLotRepository.deleteById(id);
    }

    // 주차장 목록 조회
    public List<ParkingLotResponse> getAllParkingLots() {
        return parkingLotRepository.findAll().stream()
                .map(ParkingLotResponse::from)
                .collect(Collectors.toList());
    }

    // 주차장 상세 조회
    public ParkingLotResponse getParkingLotById(Long id) {
        ParkingLot parkingLot = parkingLotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("주차장을 찾을 수 없습니다"));
        return ParkingLotResponse.from(parkingLot);
    }

    // 주차장 검색
    public List<ParkingLotResponse> searchParkingLots(String keyword) {
        return parkingLotRepository.findByLotNameContaining(keyword).stream()  // 변경: name → lotName
                .map(ParkingLotResponse::from)
                .collect(Collectors.toList());
    }
}