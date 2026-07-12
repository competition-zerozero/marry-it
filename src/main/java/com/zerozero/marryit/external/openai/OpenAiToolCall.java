package com.zerozero.marryit.external.openai;

public record OpenAiToolCall(String id, String type, OpenAiFunctionCall function) {
}
