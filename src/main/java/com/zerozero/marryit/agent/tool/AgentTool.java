package com.zerozero.marryit.agent.tool;

import java.util.Map;

/**
 * A single MCP-style capability the AI Agent can call. Implementations must scope every
 * data access by {@link AgentToolContext#workspaceId()} — the workspace/user are injected
 * by the server, never accepted as model-supplied arguments, so a different workspace's
 * data can never be exposed through tool calling.
 */
public interface AgentTool {

    String name();

    String description();

    /**
     * JSON Schema (as a plain Map so it serializes directly into an OpenAI tool definition).
     */
    Map<String, Object> parametersSchema();

    Object execute(Map<String, Object> arguments, AgentToolContext context);
}
