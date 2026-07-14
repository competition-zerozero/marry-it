package com.zerozero.marryit.agent.mcp;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class McpToolCatalog {

    private static final Map<String, ToolMetadata> METADATA = Map.of(
            "get_customer_wedding_context", new ToolMetadata(
                    "Customer Wedding Context",
                    "Retrieves a concise wedding preparation context for a customer from Marry-It(메리잇), including date, area, budget fields, preferences, schedules, incomplete tasks, and warnings for missing structured data."
            ),
            "find_replacement_vendor", new ToolMetadata(
                    "Replacement Vendor Search",
                    "Finds ranked replacement vendor candidates from Marry-It(메리잇) workspace data when an existing wedding vendor is cancelled or has an issue. It does not invent unknown prices or availability."
            ),
            "get_urgent_tasks", new ToolMetadata(
                    "Urgent Planner Tasks",
                    "Lists urgent wedding planner tasks from Marry-It(메리잇), ordered by wedding D-day and issue keywords such as cancellation, delay, confirmation, and unresolved tasks."
            ),
            "search_available_vendors", new ToolMetadata(
                    "Available Vendor Search",
                    "Searches registered Marry-It(메리잇) workspace vendors by category, date, area, style, and availability signals. Unknown price data is reported as unknown instead of guessed."
            ),
            "generate_customer_briefing", new ToolMetadata(
                    "Customer Briefing",
                    "Generates a pre-consultation briefing from Marry-It(메리잇) customer data, covering preferences, budget status, progress, incomplete tasks, recent issues, decisions, and cautions."
            )
    );

    private static final Set<String> ALLOWED_TOOL_NAMES = METADATA.keySet();

    public boolean isAllowed(String toolName) {
        return ALLOWED_TOOL_NAMES.contains(toolName);
    }

    public List<String> allowedNames() {
        return List.copyOf(ALLOWED_TOOL_NAMES);
    }

    public ToolMetadata metadata(String toolName) {
        ToolMetadata metadata = METADATA.get(toolName);
        if (metadata == null) {
            throw new IllegalArgumentException("Unsupported MCP tool: " + toolName);
        }
        return metadata;
    }

    public Map<String, Object> annotations(String toolName) {
        ToolMetadata metadata = metadata(toolName);
        return Map.of(
                "title", metadata.title(),
                "readOnlyHint", true,
                "destructiveHint", false,
                "openWorldHint", false,
                "idempotentHint", true
        );
    }

    public record ToolMetadata(String title, String description) {
    }
}
