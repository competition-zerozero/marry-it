package com.zerozero.marryit.agent.mcp;

import java.util.List;
import java.util.Map;
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

    public McpHttpController(McpJsonRpcHandler jsonRpcHandler) {
        this.jsonRpcHandler = jsonRpcHandler;
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
    public ResponseEntity<Object> handle(@RequestBody Object request) {
        Object response = handleRequest(request);
        if (response == null) {
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.ok(response);
    }

    @SuppressWarnings("unchecked")
    private Object handleRequest(Object request) {
        if (request instanceof Map<?, ?> map) {
            return jsonRpcHandler.handle((Map<String, Object>) map);
        }
        if (request instanceof List<?> list) {
            List<Map<String, Object>> responses = list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> jsonRpcHandler.handle((Map<String, Object>) item))
                    .filter(response -> response != null)
                    .toList();
            return responses.isEmpty() ? null : responses;
        }
        return jsonRpcHandler.parseError();
    }
}
