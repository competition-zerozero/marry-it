package com.zerozero.marryit.agent.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny helper so every {@link AgentTool} doesn't hand-roll the same JSON Schema boilerplate. */
final class JsonSchema {

    private JsonSchema() {
    }

    static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);
        return schema;
    }

    static Map<String, Object> property(String type, String description) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", type);
        property.put("description", description);
        return property;
    }

    static Map<String, Object> enumProperty(String type, String description, List<String> values) {
        Map<String, Object> property = property(type, description);
        property.put("enum", values);
        return property;
    }
}
