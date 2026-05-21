package com.safepark.service;

import com.safepark.dto.NearbyParkingLotResponse;
import com.safepark.dto.UiwangParkingLotDto;
import com.safepark.util.DistanceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class UiwangParkingApiService {

    private static final String UIWANG_API_URL =
            "https://parking.uiwang.go.kr/api/v1/sites?fields=spaceInfo,operationInfo";

    private static final String GYEONGGI_INFO_API_URL =
            "https://openapigits.gg.go.kr/api/rest/getParkingPlaceInfoList";

    private static final int UIWANG_LAE_ID = 31170;

    @Value("${gyeonggi.parking.api.key}")
    private String gyeonggiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 의왕시 실시간 주차장 API에서 반경 내 주차장 목록 조회
     * 경기도 info API로 요금 정보 보완
     */
    public List<NearbyParkingLotResponse> getNearbyLots(double lat, double lng, double radiusKm) {
        List<NearbyParkingLotResponse> result = new ArrayList<>();

        try {
            // 1) 의왕시 요금 정보 사전 로드 (이름 기준 Map)
            Map<String, Integer> feeByName = loadUiwangFeeMap();

            UiwangParkingLotDto[] lots = restTemplate.getForObject(UIWANG_API_URL, UiwangParkingLotDto[].class);
            if (lots == null) {
                log.warn("의왕시 API 응답이 비어있습니다.");
                return result;
            }

            for (UiwangParkingLotDto lot : lots) {
                if (Boolean.FALSE.equals(lot.getIsCounting())) continue;

                double lotLat = lot.getLatDouble();
                double lotLng = lot.getLngDouble();
                if (lotLat == 0.0 || lotLng == 0.0) continue;

                double distance = DistanceUtils.calculateDistanceKm(lat, lng, lotLat, lotLng);
                if (distance <= radiusKm) {
                    // parkingFeeDesc 파싱으로 요금 정보 추출 (우선순위 최고)
                    ParsedFee parsed = parseFeeDesc(lot.getParkingFeeDesc());

                    // 이름 기준 경기도 API 요금 (fallback)
                    String nameKey = lot.getSiteName() != null ? lot.getSiteName().replaceAll("\\s", "") : "";
                    Integer fallbackPrice = feeByName.get(nameKey);

                    Integer lotPrice = parsed.lotPrice != null ? parsed.lotPrice : fallbackPrice;
                    Boolean freeYn = parsed.freeYn != null ? parsed.freeYn
                            : (fallbackPrice != null ? (fallbackPrice == 0) : null);
                    // 무료이면 lotPrice 강제 null
                    if (Boolean.TRUE.equals(freeYn)) lotPrice = null;

                    result.add(NearbyParkingLotResponse.builder()
                            .id(null)
                            .lotName(lot.getSiteName())
                            .address(null)
                            .lat(lotLat)
                            .lng(lotLng)
                            .lotPrice(lotPrice)
                            .freeYn(freeYn)
                            .operatingHours(lot.getOperationHourDesc())
                            .parkingFeeDesc(lot.getParkingFeeDesc())
                            .totalSpaces(lot.getCapacity())
                            .availableSpots(lot.getAvailableSpots())
                            .distanceKm(Math.round(distance * 1000.0) / 1000.0)
                            .source("의왕시")
                            .feeUnit(parsed.feeUnit)
                            .addUnitTime(parsed.addUnitTime)
                            .addUnitPrice(parsed.addUnitPrice)
                            .build());
                }
            }

            result.sort(Comparator.comparingDouble(NearbyParkingLotResponse::getDistanceKm));
            log.info("의왕시 API: 반경 {}km 내 주차장 {}개 조회됨", radiusKm, result.size());

        } catch (Exception e) {
            log.error("의왕시 주차장 API 호출 실패: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 경기도 info API에서 의왕시 요금 정보 로드
     * key: 주차장명(공백제거), value: 기본요금
     */
    private Map<String, Integer> loadUiwangFeeMap() {
        Map<String, Integer> feeMap = new HashMap<>();
        try {
            String url = GYEONGGI_INFO_API_URL + "?serviceKey=" + gyeonggiApiKey + "&laeId=" + UIWANG_LAE_ID;
            String xml = restTemplate.getForObject(url, String.class);
            if (xml == null) return feeMap;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            doc.getDocumentElement().normalize();

            NodeList items = doc.getElementsByTagName("itemList");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String name = getText(item, "pkplcNm");
                String fareStr = getText(item, "parkingBscFare");
                if (name == null || fareStr == null) continue;
                try {
                    feeMap.put(name.replaceAll("\\s", ""), Integer.parseInt(fareStr.trim()));
                } catch (NumberFormatException ignored) {}
            }
            log.debug("의왕시 요금 정보 로드: {}개", feeMap.size());
        } catch (Exception e) {
            log.warn("의왕시 요금 정보 로드 실패: {}", e.getMessage());
        }
        return feeMap;
    }

    /**
     * parkingFeeDesc 파싱
     * 예: "기본요금: 800원/30분\n추가요금: 300원/10분"
     * 예: "무료 운영"
     */
    private ParsedFee parseFeeDesc(String desc) {
        ParsedFee result = new ParsedFee();
        if (desc == null || desc.isBlank()) return result;

        if (desc.contains("무료")) {
            result.freeYn = true;
            return result;
        }

        // 기본요금: 1,000원/60분 또는 기본요금: 800원/30분
        Pattern basicPattern = Pattern.compile("기본요금[:\\s]*([0-9,]+)원/(\\d+)분");
        Matcher basicMatcher = basicPattern.matcher(desc);
        if (basicMatcher.find()) {
            try {
                result.lotPrice = Integer.parseInt(basicMatcher.group(1).replace(",", ""));
                result.feeUnit = Integer.parseInt(basicMatcher.group(2));
                result.freeYn = false;
            } catch (NumberFormatException ignored) {}
        }

        // 추가요금: 300원/10분
        Pattern addPattern = Pattern.compile("추가요금[:\\s]*([0-9,]+)원/(\\d+)분");
        Matcher addMatcher = addPattern.matcher(desc);
        if (addMatcher.find()) {
            try {
                result.addUnitPrice = Integer.parseInt(addMatcher.group(1).replace(",", ""));
                result.addUnitTime = Integer.parseInt(addMatcher.group(2));
            } catch (NumberFormatException ignored) {}
        }

        return result;
    }

    private static class ParsedFee {
        Boolean freeYn;
        Integer lotPrice;
        Integer feeUnit;
        Integer addUnitTime;
        Integer addUnitPrice;
    }

    private String getText(Element el, String tagName) {
        NodeList nl = el.getElementsByTagName(tagName);
        if (nl.getLength() == 0) return null;
        String text = nl.item(0).getTextContent();
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
