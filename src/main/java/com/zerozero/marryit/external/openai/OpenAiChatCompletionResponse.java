package com.zerozero.marryit.external.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record OpenAiChatCompletionResponse(List<OpenAiChoice> choices) {

    public record OpenAiChoice(OpenAiChatMessage message, @JsonProperty("finish_reason") String finishReason) {
    }
}
