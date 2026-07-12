package com.zerozero.marryit.external.openai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OpenAiClientProperties.class)
public class OpenAiClientConfig {
}
