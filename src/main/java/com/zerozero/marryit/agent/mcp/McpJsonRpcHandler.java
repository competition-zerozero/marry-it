package com.zerozero.marryit.agent.mcp;

import com.zerozero.marryit.agent.tool.AgentTool;
import com.zerozero.marryit.agent.tool.AgentToolContext;
import com.zerozero.marryit.agent.tool.AgentToolRegistry;
import java.lang.reflect.RecordComponent;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class McpJsonRpcHandler {

    private static final String PROTOCOL_VERSION = "2025-03-26";

    private final AgentToolRegistry toolRegistry;
    private final McpToolCatalog toolCatalog;
    private final ObjectMapper objectMapper;
    private final Long workspaceId;
    private final Long userId;

    public McpJsonRpcHandler(
            AgentToolRegistry toolRegistry,
            McpToolCatalog toolCatalog,
            ObjectMapper objectMapper,
            @Value("${mcp.workspace-id:0}") Long workspaceId,
            @Value("${mcp.user-id:0}") Long userId
    ) {
        this.toolRegistry = toolRegistry;
        this.toolCatalog = toolCatalog;
        this.objectMapper = objectMapper;
        this.workspaceId = workspaceId;
        this.userId = userId;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> handle(Map<String, Object> request) {
        Object id = request.get("id");
        String method = request.get("method") == null ? null : request.get("method").toString();
        if (id == null && method != null && method.startsWith("notifications/")) {
            return null;
        }

        try {
            Object result = switch (method) {
                case "initialize" -> initializeResult();
                case "tools/list" -> toolsListResult();
                case "tools/call" -> toolsCallResult((Map<String, Object>) request.getOrDefault("params", Map.of()));
                default -> throw new McpException(-32601, "Method not found: " + method);
            };
            return response(id, result);
        } catch (McpException exception) {
            return error(id, exception.code(), exception.getMessage());
        } catch (Exception exception) {
            return error(id, -32603, exception.getMessage() == null ? "Internal error" : exception.getMessage());
        }
    }

    public Map<String, Object> parseError() {
        return error(null, -32700, "Invalid JSON");
    }

    public String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("MCP response serialization failed.", exception);
        }
    }

    private Map<String, Object> initializeResult() {
        return Map.of(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", Map.of("tools", Map.of("listChanged", false)),
                "serverInfo", Map.of(
                        "name", "marry-it-mcp",
                        "version", "0.0.1"
                )
        );
    }

    private Map<String, Object> toolsListResult() {
        List<Map<String, Object>> tools = toolRegistry.tools().stream()
                .filter(tool -> toolCatalog.isAllowed(tool.name()))
                .map(this::toolDefinition)
                .toList();
        return Map.of("tools", tools);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toolsCallResult(Map<String, Object> params) {
        assertContextConfigured();

        String name = params.get("name") == null ? null : params.get("name").toString();
        if (name == null || name.isBlank()) {
            throw new McpException(-32602, "Missing tool name");
        }
        if (!toolCatalog.isAllowed(name)) {
            throw new McpException(-32602, "Unknown tool: " + name);
        }
        Map<String, Object> arguments = params.get("arguments") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();

        Object toolResult = toolRegistry.execute(name, arguments, new AgentToolContext(workspaceId, userId));
        return Map.of(
                "content", List.of(Map.of(
                        "type", "text",
                        "text", toCompactMarkdown(name, toolResult)
                )),
                "structuredContent", Map.of(
                        "tool", name,
                        "result", toolResult
                ),
                "isError", false
        );
    }

    private Map<String, Object> toolDefinition(AgentTool tool) {
        McpToolCatalog.ToolMetadata metadata = toolCatalog.metadata(tool.name());
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("name", tool.name());
        definition.put("title", metadata.title());
        definition.put("description", metadata.description());
        definition.put("inputSchema", tool.parametersSchema());
        definition.put("annotations", toolCatalog.annotations(tool.name()));
        return definition;
    }

    private void assertContextConfigured() {
        if (workspaceId == null || workspaceId <= 0 || userId == null || userId <= 0) {
            throw new McpException(
                    -32000,
                    "MCP_WORKSPACE_ID and MCP_USER_ID must be set to execute Marry-It tools."
            );
        }
    }

    private Map<String, Object> response(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        return response;
    }

    private Map<String, Object> error(Object id, int code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("jsonrpc", "2.0");
        error.put("id", id);
        error.put("error", Map.of(
                "code", code,
                "message", message
        ));
        return error;
    }

    private String toCompactMarkdown(String toolName, Object toolResult) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# ").append(toolCatalog.metadata(toolName).title()).append("\n\n");
        appendValue(markdown, toolResult, 0);
        return markdown.toString();
    }

    private void appendValue(StringBuilder markdown, Object value, int depth) {
        if (value == null) {
            markdown.append("No data.");
            return;
        }
        if (isScalar(value)) {
            markdown.append(value);
            return;
        }
        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                markdown.append("No items.");
                return;
            }
            int index = 1;
            for (Object item : collection.stream().limit(5).toList()) {
                indent(markdown, depth).append("- ");
                if (isScalar(item)) {
                    markdown.append(item).append("\n");
                } else {
                    markdown.append("Item ").append(index).append("\n");
                    appendValue(markdown, item, depth + 1);
                }
                index++;
            }
            if (collection.size() > 5) {
                indent(markdown, depth).append("- ").append(collection.size() - 5).append(" more items omitted.\n");
            }
            return;
        }
        if (value instanceof Map<?, ?> map) {
            appendMap(markdown, map, depth);
            return;
        }
        if (value.getClass().isRecord()) {
            appendRecord(markdown, value, depth);
            return;
        }
        markdown.append(writeJson(value));
    }

    private void appendRecord(StringBuilder markdown, Object record, int depth) {
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            try {
                Object componentValue = component.getAccessor().invoke(record);
                if (isEmpty(componentValue)) {
                    continue;
                }
                indent(markdown, depth)
                        .append("- ")
                        .append(component.getName())
                        .append(": ");
                appendInlineOrNested(markdown, componentValue, depth);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Cannot render MCP tool result.", exception);
            }
        }
    }

    private void appendMap(StringBuilder markdown, Map<?, ?> map, int depth) {
        map.entrySet().stream()
                .filter(entry -> !isEmpty(entry.getValue()))
                .forEach(entry -> {
                    indent(markdown, depth)
                            .append("- ")
                            .append(entry.getKey())
                            .append(": ");
                    appendInlineOrNested(markdown, entry.getValue(), depth);
                });
    }

    private void appendInlineOrNested(StringBuilder markdown, Object value, int depth) {
        if (isScalar(value)) {
            markdown.append(value).append("\n");
            return;
        }
        markdown.append("\n");
        appendValue(markdown, value, depth + 1);
    }

    private boolean isScalar(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>
                || value instanceof TemporalAccessor;
    }

    private boolean isEmpty(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof CharSequence sequence) {
            return sequence.isEmpty();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        return Objects.equals(value, false);
    }

    private StringBuilder indent(StringBuilder markdown, int depth) {
        return markdown.append("  ".repeat(Math.max(0, depth)));
    }

    private static class McpException extends RuntimeException {

        private final int code;

        McpException(int code, String message) {
            super(message);
            this.code = code;
        }

        int code() {
            return code;
        }
    }
}
