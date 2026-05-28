package com.safepark.util;

/**
 * 위경도 → Geohash 인코딩 유틸리티
 * 모두의주차장 API 호출 시 사용
 */
public class GeohashUtils {

    private static final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";

    /**
     * 위경도를 geohash 문자열로 변환
     * @param lat 위도
     * @param lng 경도
     * @param precision geohash 길이 (5 = 약 5km 정밀도, 6 = 약 1km)
     */
    public static String encode(double lat, double lng, int precision) {
        double[] latRange = {-90.0, 90.0};
        double[] lngRange = {-180.0, 180.0};

        StringBuilder hash = new StringBuilder();
        boolean isLng = true;
        int bits = 0;
        int hashValue = 0;

        while (hash.length() < precision) {
            double mid;
            if (isLng) {
                mid = (lngRange[0] + lngRange[1]) / 2;
                if (lng >= mid) {
                    hashValue = (hashValue << 1) | 1;
                    lngRange[0] = mid;
                } else {
                    hashValue = hashValue << 1;
                    lngRange[1] = mid;
                }
            } else {
                mid = (latRange[0] + latRange[1]) / 2;
                if (lat >= mid) {
                    hashValue = (hashValue << 1) | 1;
                    latRange[0] = mid;
                } else {
                    hashValue = hashValue << 1;
                    latRange[1] = mid;
                }
            }
            isLng = !isLng;
            bits++;

            if (bits == 5) {
                hash.append(BASE32.charAt(hashValue));
                bits = 0;
                hashValue = 0;
            }
        }
        return hash.toString();
    }
}
