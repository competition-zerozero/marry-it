package com.zerozero.marryit.agent.tool;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> toolsByName;

    public AgentToolRegistry(List<AgentTool> tools) {
        this.toolsByName = tools.stream()
                .collect(Collectors.toMap(AgentTool::name, tool -> tool));
    }

    public List<AgentTool> tools() {
        return List.copyOf(toolsByName.values());
    }

    public Object execute(String toolName, Map<String, Object> arguments, AgentToolContext context) {
        AgentTool tool = toolsByName.get(toolName);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + toolName);
        }
        return tool.execute(arguments == null ? Map.of() : arguments, context);
    }
}
