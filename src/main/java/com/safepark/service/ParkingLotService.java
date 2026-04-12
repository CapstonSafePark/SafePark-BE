package com.safepark.service;

import com.safepark.entity.ParkingLot;
import com.safepark.repository.ParkingLotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;

    // 주차장 등록
    public ParkingLot createParkingLot(ParkingLot parkingLot) {
        return parkingLotRepository.save(parkingLot);
    }

    // 주차장 목록 조회
    public List<ParkingLot> getAllParkingLots() {
        return parkingLotRepository.findAll();
    }

    // 주차장 상세 조회
    public ParkingLot getParkingLotById(Long id) {
        return parkingLotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("주차장을 찾을 수 없습니다"));
    }

    // 주차장 수정
    public ParkingLot updateParkingLot(Long id, ParkingLot updatedData) {
        ParkingLot parkingLot = getParkingLotById(id);

        parkingLot.setName(updatedData.getName());
        parkingLot.setAddress(updatedData.getAddress());
        parkingLot.setLatitude(updatedData.getLatitude());
        parkingLot.setLongitude(updatedData.getLongitude());
        parkingLot.setType(updatedData.getType());
        parkingLot.setCapacity(updatedData.getCapacity());
        parkingLot.setOperatingHours(updatedData.getOperatingHours());
        parkingLot.setBasicFee(updatedData.getBasicFee());
        parkingLot.setAdditionalFee(updatedData.getAdditionalFee());

        return parkingLotRepository.save(parkingLot);
    }

    // 주차장 삭제
    public void deleteParkingLot(Long id) {
        ParkingLot parkingLot = getParkingLotById(id);
        parkingLotRepository.delete(parkingLot);
    }

    // 주차장 검색 (이름)
    public List<ParkingLot> searchParkingLots(String keyword) {
        return parkingLotRepository.findByNameContaining(keyword);
    }
}