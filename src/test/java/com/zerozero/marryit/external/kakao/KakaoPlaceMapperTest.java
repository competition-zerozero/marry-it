package com.zerozero.marryit.external.kakao;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class KakaoPlaceMapperTest {

    private final KakaoPlaceMapper mapper = new KakaoPlaceMapper();

    @Test
    void mapsOnlyRequiredKakaoPlaceFields() {
        KakaoPlaceDocument document = new KakaoPlaceDocument(
                "12345",
                "A 플라워",
                "서울 강남구",
                "서울 강남구 테헤란로",
                "02-123-4567",
                "127.0276100",
                "37.4980950",
                "https://place.map.kakao.com/12345"
        );

        KakaoPlaceResponse response = mapper.map(document);

        assertThat(response.kakaoPlaceId()).isEqualTo("12345");
        assertThat(response.name()).isEqualTo("A 플라워");
        assertThat(response.latitude()).isEqualByComparingTo(new BigDecimal("37.4980950"));
        assertThat(response.longitude()).isEqualByComparingTo(new BigDecimal("127.0276100"));
    }
}
