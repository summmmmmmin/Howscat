package com.example.howscat.kakao;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 카카오 로컬 키워드 검색 API 응답 일부.
 * https://developers.kakao.com/docs/latest/ko/local/dev-guide#search-by-keyword
 */
public class KakaoLocalKeywordResponse {

    private List<Document> documents;

    public List<Document> getDocuments() {
        return documents;
    }

    public static class Document {
        private String id;
        @SerializedName("place_name")
        private String placeName;
        @SerializedName("address_name")
        private String addressName;
        @SerializedName("road_address_name")
        private String roadAddressName;
        /** 질의 좌표로부터 거리(m), 문자열 */
        private String distance;
        private String x;
        private String y;
        @SerializedName("place_url")
        private String placeUrl;
        private String phone;

        public String getId() {
            return id;
        }

        public String getPlaceName() {
            return placeName;
        }

        public String getAddressName() {
            return addressName;
        }

        public String getRoadAddressName() {
            return roadAddressName;
        }

        public String getDistance() {
            return distance;
        }

        public String getX() {
            return x;
        }

        public String getY() {
            return y;
        }

        public String getPlaceUrl() {
            return placeUrl;
        }

        public String getPhone() {
            return phone;
        }
    }
}
