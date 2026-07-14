package com.zerozero.marryit.agent.mcp;

import java.util.List;
import java.util.Map;
import com.zerozero.marryit.agent.tool.AgentToolContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("mcp")
public class McpHttpController {

    private final McpJsonRpcHandler jsonRpcHandler;
    private final McpRequestContextResolver contextResolver;

    public McpHttpController(McpJsonRpcHandler jsonRpcHandler, McpRequestContextResolver contextResolver) {
        this.jsonRpcHandler = jsonRpcHandler;
        this.contextResolver = contextResolver;
    }

    @GetMapping({"/", "/health"})
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "server", "marry-it-mcp",
                "transport", "http",
                "endpoint", "/mcp"
        );
    }

    @PostMapping(
            value = "/mcp",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Object> handle(@RequestBody Object request, HttpServletRequest servletRequest) {
        AgentToolContext context = resolveContextIfNeeded(request, servletRequest);
        Object response = handleRequest(request, context);
        if (response == null) {
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.ok(response);
    }

    @SuppressWarnings("unchecked")
    private Object handleRequest(Object request, AgentToolContext context) {
        if (request instanceof Map<?, ?> map) {
            return jsonRpcHandler.handle((Map<String, Object>) map, context);
        }
        if (request instanceof List<?> list) {
            List<Map<String, Object>> responses = list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> jsonRpcHandler.handle((Map<String, Object>) item, context))
                    .filter(response -> response != null)
                    .toList();
            return responses.isEmpty() ? null : responses;
        }
        return jsonRpcHandler.parseError();
    }

    @SuppressWarnings("unchecked")
    private AgentToolContext resolveContextIfNeeded(Object request, HttpServletRequest servletRequest) {
        if (request instanceof Map<?, ?> map && isToolCall((Map<String, Object>) map)) {
            return contextResolver.resolve(servletRequest);
        }
        if (request instanceof List<?> list && list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .anyMatch(this::isToolCall)) {
            return contextResolver.resolve(servletRequest);
        }
        return null;
    }

    private boolean isToolCall(Map<String, Object> request) {
        return "tools/call".equals(request.get("method"));
    }
}
