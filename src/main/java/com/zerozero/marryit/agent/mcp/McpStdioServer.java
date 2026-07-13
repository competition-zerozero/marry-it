package com.zerozero.marryit.agent.mcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@Profile("mcp-stdio")
public class McpStdioServer implements CommandLineRunner {

    private final ObjectMapper objectMapper;
    private final McpJsonRpcHandler jsonRpcHandler;

    public McpStdioServer(
            ObjectMapper objectMapper,
            McpJsonRpcHandler jsonRpcHandler
    ) {
        this.objectMapper = objectMapper;
        this.jsonRpcHandler = jsonRpcHandler;
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
            write(writer, jsonRpcHandler.parseError());
            return;
        }

        Map<String, Object> response = jsonRpcHandler.handle(request);
        if (response != null) {
            write(writer, response);
        }
    }

    private void write(PrintWriter writer, Map<String, Object> response) {
        writer.println(jsonRpcHandler.writeJson(response));
        writer.flush();
    }
}
