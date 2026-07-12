package com.zerozero.marryit.external.openai;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RestClientOpenAiChatClient implements OpenAiChatClient {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private final OpenAiClientProperties properties;
    private final RestClient restClient;

    public RestClientOpenAiChatClient(OpenAiClientProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl() == null || properties.baseUrl().isBlank()
                        ? "https://api.openai.com"
                        : properties.baseUrl())
                .build();
    }

    @Override
    public boolean isConfigured() {
        return properties.apiKey() != null && !properties.apiKey().isBlank();
    }

    @Override
    public OpenAiChatCompletionResponse complete(List<OpenAiChatMessage> messages, List<OpenAiToolDefinition> tools) {
        if (!isConfigured()) {
            throw new IllegalStateException("OpenAI API key is not configured.");
        }

        boolean hasTools = tools != null && !tools.isEmpty();
        OpenAiChatCompletionRequest request = new OpenAiChatCompletionRequest(
                properties.model() == null || properties.model().isBlank() ? DEFAULT_MODEL : properties.model(),
                messages,
                hasTools ? tools : null,
                hasTools ? "auto" : null
        );

        OpenAiChatCompletionResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OpenAiChatCompletionResponse.class);

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI returned an empty response.");
        }
        return response;
    }
}
