package com.zerozero.marryit.external.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;

record KakaoPlaceDocument(
        String id,
        @JsonProperty("place_name")
        String placeName,
        @JsonProperty("address_name")
        String addressName,
        @JsonProperty("road_address_name")
        String roadAddressName,
        String phone,
        String x,
        String y,
        @JsonProperty("place_url")
        String placeUrl
) {
}
