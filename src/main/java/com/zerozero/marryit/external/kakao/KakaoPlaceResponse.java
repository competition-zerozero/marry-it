package com.zerozero.marryit.external.kakao;

import java.math.BigDecimal;

public record KakaoPlaceResponse(
        String kakaoPlaceId,
        String name,
        String address,
        String roadAddress,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        String placeUrl
) {
}
