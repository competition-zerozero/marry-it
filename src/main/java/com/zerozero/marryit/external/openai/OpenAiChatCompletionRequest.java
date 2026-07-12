package com.zerozero.marryit.external.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiChatCompletionRequest(
        String model,
        List<OpenAiChatMessage> messages,
        List<OpenAiToolDefinition> tools,
        @JsonProperty("tool_choice") String toolChoice
) {
}
