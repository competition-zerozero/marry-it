package com.zerozero.marryit.external.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAiChatMessage(
        String role,
        String content,
        @JsonProperty("tool_calls") List<OpenAiToolCall> toolCalls,
        @JsonProperty("tool_call_id") String toolCallId
) {

    public static OpenAiChatMessage system(String content) {
        return new OpenAiChatMessage("system", content, null, null);
    }

    public static OpenAiChatMessage user(String content) {
        return new OpenAiChatMessage("user", content, null, null);
    }

    public static OpenAiChatMessage assistant(String content, List<OpenAiToolCall> toolCalls) {
        return new OpenAiChatMessage("assistant", content, toolCalls, null);
    }

    public static OpenAiChatMessage toolResult(String toolCallId, String content) {
        return new OpenAiChatMessage("tool", content, null, toolCallId);
    }
}
