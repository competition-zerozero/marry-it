package com.zerozero.marryit.external.openai;

public record OpenAiToolDefinition(String type, OpenAiFunctionDefinition function) {

    public static OpenAiToolDefinition function(OpenAiFunctionDefinition function) {
        return new OpenAiToolDefinition("function", function);
    }
}
