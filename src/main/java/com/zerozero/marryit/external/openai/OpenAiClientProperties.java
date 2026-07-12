package com.zerozero.marryit.external.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiClientProperties(
        String apiKey,
        String model,
        String baseUrl
) {
}
