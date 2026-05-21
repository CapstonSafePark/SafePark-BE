package com.safepark.service;

import com.safepark.entity.ParkingLot;
import com.safepark.repository.ParkingLotRepository;
import com.safepark.util.DistanceUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class ParkingFeeUpdateService {

    private static final String INFO_API_URL =
            "https://openapigits.gg.go.kr/api/rest/getParkingPlaceInfoList";

    // 좌표 매칭 임계값: 150m 이내면 같은 주차장으로 판단
    private static final double MATCH_THRESHOLD_KM = 0.15;

    // 경기도 전체 시군구 laeId 목록
    private static final List<Integer> ALL_LAE_IDS = List.of(
            31010, // 수원시
            31020, // 성남시
            31030, // 의정부시
            31040, // 안양시
            31050, // 부천시
            31060, // 광명시
            31070, // 평택시
            31080, // 동두천시
            31090, // 안산시
            31100, // 고양시
            31110, // 과천시
            31120, // 구리시
            31130, // 남양주시
            31140, // 오산시
            31150, // 시흥시
            31160, // 군포시
            31170, // 의왕시
            31180, // 하남시
            31190, // 용인시
            31200, // 파주시
            31210, // 이천시
            31220, // 안성시
            31230, // 김포시
            31240, // 화성시
            31250, // 광주시
            31260, // 양주시
            31270, // 포천시
            31280, // 여주시
            31350, // 연천군
            31370, // 가평군
            31380  // 양평군
    );

    @Value("${gyeonggi.parking.api.key}")
    private String apiKey;

    private final ParkingLotRepository parkingLotRepository;
    private final RestTemplate restTemplate;

    public ParkingFeeUpdateService(ParkingLotRepository parkingLotRepository) {
        this.parkingLotRepository = parkingLotRepository;
        RestTemplate rt = new RestTemplate();
        rt.getMessageConverters().stream()
                .filter(c -> c instanceof StringHttpMessageConverter)
                .forEach(c -> ((StringHttpMessageConverter) c).setDefaultCharset(StandardCharsets.UTF_8));
        this.restTemplate = rt;
    }

    /**
     * 경기도 전체 시군구 주차장 요금 정보를 가져와 DB 업데이트 (좌표 기반 매칭)
     */
    @Transactional
    public Map<String, Integer> updateAllFees() {
        List<ParkingLot> allLots = parkingLotRepository.findAll();
        log.info("DB 주차장 총 {}개 로드", allLots.size());

        int updated = 0;
        int failed = 0;

        for (int laeId : ALL_LAE_IDS) {
            try {
                int count = fetchAndUpdateByCoord(laeId, allLots);
                updated += count;
                log.info("laeId={} 업데이트 완료: {}개", laeId, count);
                Thread.sleep(300);
            } catch (Exception e) {
                log.error("laeId={} 처리 실패: {}", laeId, e.getMessage());
                failed++;
            }
        }

        // 변경된 것 저장 (무료 주차장은 lotPrice=null이지만 freeYn=1 이므로 freeYn 포함)
        List<ParkingLot> toSave = allLots.stream()
                .filter(lot -> lot.getLotPrice() != null || lot.getFreeYn() != null || lot.getOperatingHours() != null)
                .toList();
        parkingLotRepository.saveAll(toSave);

        log.info("요금 업데이트 완료 - 업데이트: {}개, 실패 laeId: {}개", updated, failed);
        return Map.of("updated", updated, "total", allLots.size(), "failedLaeIds", failed);
    }

    private int fetchAndUpdateByCoord(int laeId, List<ParkingLot> allLots) throws Exception {
        String url = INFO_API_URL + "?serviceKey=" + apiKey + "&laeId=" + laeId;
        String xml = restTemplate.getForObject(url, String.class);
        if (xml == null) return 0;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        NodeList items = doc.getElementsByTagName("itemList");
        int count = 0;

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);

            // API 좌표 파싱
            String latStr = getText(item, "latCrdn");
            String lngStr = getText(item, "lonCrdn");
            String fareStr = getText(item, "parkingBscFare");
            String weekdayStart = getText(item, "wkdayOprtStartTime");
            String weekdayEnd = getText(item, "wkdayOprtEndTime");

            if (latStr == null || lngStr == null) continue;

            double apiLat, apiLng;
            try {
                apiLat = Double.parseDouble(latStr);
                apiLng = Double.parseDouble(lngStr);
            } catch (NumberFormatException e) {
                continue;
            }
            if (apiLat == 0.0 || apiLng == 0.0) continue;

            // 요금 파싱
            Integer fare = null;
            if (fareStr != null && !fareStr.isBlank()) {
                try {
                    fare = Integer.parseInt(fareStr.trim());
                } catch (NumberFormatException ignored) {}
            }

            // DB에서 가장 가까운 주차장 찾기 (150m 이내)
            ParkingLot best = null;
            double bestDist = MATCH_THRESHOLD_KM;
            for (ParkingLot lot : allLots) {
                if (lot.getLat() == null || lot.getLng() == null) continue;
                double dist = DistanceUtils.calculateDistanceKm(
                        apiLat, apiLng, lot.getLat(), lot.getLng());
                if (dist < bestDist) {
                    bestDist = dist;
                    best = lot;
                }
            }

            if (best == null) continue;

            // 요금 업데이트
            if (fare != null) {
                best.setLotPrice(fare == 0 ? null : fare);
                best.setFreeYn(fare == 0 ? 1 : 0);
                count++;
            }

            // 운영 시간 업데이트 (없는 경우만)
            if (best.getOperatingHours() == null && weekdayStart != null && weekdayEnd != null
                    && !weekdayStart.equals("00:00") && !weekdayEnd.equals("00:00")) {
                best.setOperatingHours(weekdayStart + "~" + weekdayEnd);
            }
        }
        return count;
    }

    /**
     * 경기도 공공데이터 JSON 파일에서 요금 정보 읽어 DB 업데이트
     * PARKPLC_NM → lot_name 이름 매칭
     */
    @Transactional
    public Map<String, Integer> updateFeesFromJsonFile(String filePath) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(new java.io.File(filePath));

        List<ParkingLot> allLots = parkingLotRepository.findAll();
        Map<String, ParkingLot> nameToLot = new HashMap<>();
        for (ParkingLot lot : allLots) {
            if (lot.getLotName() != null) {
                nameToLot.put(lot.getLotName().replaceAll("\\s", ""), lot);
            }
        }

        int updated = 0;
        com.fasterxml.jackson.databind.JsonNode items = root.isArray() ? root : root.path("data");

        for (com.fasterxml.jackson.databind.JsonNode item : items) {
            String name = item.path("PARKPLC_NM").asText(null);
            String fareStr = item.path("PARKNG_BASIC_UTLZ_CHRG").asText("").trim();

            if (name == null || fareStr.isBlank()) continue;

            int fare;
            try { fare = Integer.parseInt(fareStr); } catch (NumberFormatException e) { continue; }
            if (fare <= 0) continue;

            ParkingLot lot = nameToLot.get(name.replaceAll("\\s", ""));
            if (lot == null) continue;

            lot.setLotPrice(fare);
            lot.setFreeYn(0);

            // 기본 주차 시간 (분)
            String feeUnitStr = item.path("PARKNG_BASIC_TM").asText("").trim();
            if (!feeUnitStr.isBlank()) {
                try { lot.setFeeUnit(Integer.parseInt(feeUnitStr)); } catch (NumberFormatException ignored) {}
            }

            // 추가 단위 시간 (분)
            String addUnitTimeStr = item.path("ADD_UNIT_TM").asText("").trim();
            if (!addUnitTimeStr.isBlank()) {
                try { lot.setAddUnitTime(Integer.parseInt(addUnitTimeStr)); } catch (NumberFormatException ignored) {}
            }

            // 추가 요금 (원)
            String addUnitPriceStr = item.path("ADD_UNIT_HR02_WITHIN_UTLZ_CHRG").asText("").trim();
            if (!addUnitPriceStr.isBlank()) {
                try { lot.setAddUnitPrice(Integer.parseInt(addUnitPriceStr)); } catch (NumberFormatException ignored) {}
            }

            // 운영시간
            if (lot.getOperatingHours() == null) {
                String start = item.path("WKDAY_OPERT_BEGIN_TM").asText("").trim();
                String end = item.path("WKDAY_OPERT_END_TM").asText("").trim();
                if (!start.isBlank() && !end.isBlank() && !start.equals("00:00") && !end.equals("23:59")) {
                    lot.setOperatingHours(start + "~" + end);
                }
            }
            updated++;
        }

        parkingLotRepository.saveAll(allLots.stream().filter(l -> l.getLotPrice() != null).toList());
        log.info("JSON 파일 요금 업데이트 완료: {}개", updated);
        return Map.of("updated", updated, "total", allLots.size());
    }

    private String getText(Element el, String tagName) {
        NodeList nl = el.getElementsByTagName(tagName);
        if (nl.getLength() == 0) return null;
        String text = nl.item(0).getTextContent();
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
