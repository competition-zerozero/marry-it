package com.zerozero.marryit.external.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao.place")
public record KakaoPlaceClientProperties(
        String apiKey,
        String baseUrl
) {
}
