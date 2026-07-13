package com.zerozero.marryit.agent.mcp;

import java.util.Map;
import org.springframework.context.annotation.Profile;
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

    @PostMapping({"/mcp", "/"})
    public ResponseEntity<Map<String, Object>> handle(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = jsonRpcHandler.handle(request);
        if (response == null) {
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.ok(response);
    }
}
