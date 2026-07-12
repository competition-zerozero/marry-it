package com.zerozero.marryit.agent.tool;

import java.util.Map;

/** Model-supplied tool arguments arrive as loosely-typed JSON (numbers as Integer/Long/Double). */
final class AgentToolArguments {

    private AgentToolArguments() {
    }

    static Long requireLong(Map<String, Object> arguments, String key) {
        Long value = optionalLong(arguments, key);
        if (value == null) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        return value;
    }

    static Long optionalLong(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    static String optionalString(Map<String, Object> arguments, String key) {
        Object value = arguments.get(key);
        return value == null ? null : value.toString();
    }
}
