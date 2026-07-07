package com.zerozero.marryit.external.kakao;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KakaoPlaceClientProperties.class)
public class KakaoPlaceClientConfig {
}
