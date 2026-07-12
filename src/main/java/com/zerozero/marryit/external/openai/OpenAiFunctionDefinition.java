package com.zerozero.marryit.external.openai;

import java.util.Map;

public record OpenAiFunctionDefinition(String name, String description, Map<String, Object> parameters) {
}
