package com.zerozero.marryit.agent.mcp;

import com.zerozero.marryit.agent.tool.AgentTool;
import com.zerozero.marryit.agent.tool.AgentToolContext;
import com.zerozero.marryit.agent.tool.AgentToolRegistry;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("mcp")
public class McpStdioServer implements CommandLineRunner {

    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final AgentToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final Long workspaceId;
    private final Long userId;

    public McpStdioServer(
            AgentToolRegistry toolRegistry,
            ObjectMapper objectMapper,
            @Value("${mcp.workspace-id:0}") Long workspaceId,
            @Value("${mcp.user-id:0}") Long userId
    ) {
        this.toolRegistry = toolRegistry;
        this.objectMapper = objectMapper;
        this.workspaceId = workspaceId;
        this.userId = userId;
    }

    @Override
    public void run(String... args) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                handleLine(line, writer);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleLine(String line, PrintWriter writer) {
        if (line.isBlank()) {
            return;
        }

        Map<String, Object> request;
        try {
            request = objectMapper.readValue(line, Map.class);
        } catch (Exception exception) {
            write(writer, error(null, -32700, "Invalid JSON"));
            return;
        }

        Object id = request.get("id");
        String method = request.get("method") == null ? null : request.get("method").toString();
        if (id == null && method != null && method.startsWith("notifications/")) {
            return;
        }

        try {
            Object result = switch (method) {
                case "initialize" -> initializeResult();
                case "tools/list" -> toolsListResult();
                case "tools/call" -> toolsCallResult((Map<String, Object>) request.getOrDefault("params", Map.of()));
                default -> throw new McpException(-32601, "Method not found: " + method);
            };
            write(writer, response(id, result));
        } catch (McpException exception) {
            write(writer, error(id, exception.code(), exception.getMessage()));
        } catch (Exception exception) {
            write(writer, error(id, -32603, exception.getMessage() == null ? "Internal error" : exception.getMessage()));
        }
    }

    private Map<String, Object> initializeResult() {
        return Map.of(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", Map.of("tools", Map.of()),
                "serverInfo", Map.of(
                        "name", "marry-it-mcp",
                        "version", "0.0.1"
                )
        );
    }

    private Map<String, Object> toolsListResult() {
        List<Map<String, Object>> tools = toolRegistry.tools().stream()
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
        Map<String, Object> arguments = params.get("arguments") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Map.of();

        Object toolResult = toolRegistry.execute(name, arguments, new AgentToolContext(workspaceId, userId));
        return Map.of(
                "content", List.of(Map.of(
                        "type", "text",
                        "text", writeJson(toolResult)
                )),
                "isError", false
        );
    }

    private Map<String, Object> toolDefinition(AgentTool tool) {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("name", tool.name());
        definition.put("description", tool.description());
        definition.put("inputSchema", tool.parametersSchema());
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

    private void write(PrintWriter writer, Map<String, Object> response) {
        writer.println(writeJson(response));
        writer.flush();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("MCP response serialization failed.", exception);
        }
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
