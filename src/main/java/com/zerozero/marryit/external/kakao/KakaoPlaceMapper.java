package com.zerozero.marryit.external.kakao;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class KakaoPlaceMapper {

    public KakaoPlaceResponse map(KakaoPlaceDocument document) {
        return new KakaoPlaceResponse(
                document.id(),
                document.placeName(),
                document.addressName(),
                document.roadAddressName(),
                document.phone(),
                toDecimal(document.y()),
                toDecimal(document.x()),
                document.placeUrl()
        );
    }

    private BigDecimal toDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value);
    }
}
