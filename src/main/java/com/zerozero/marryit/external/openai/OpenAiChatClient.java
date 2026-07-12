package com.zerozero.marryit.external.openai;

import java.util.List;

public interface OpenAiChatClient {

    boolean isConfigured();

    OpenAiChatCompletionResponse complete(List<OpenAiChatMessage> messages, List<OpenAiToolDefinition> tools);
}
