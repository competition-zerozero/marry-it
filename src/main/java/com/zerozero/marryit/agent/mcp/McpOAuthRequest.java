package com.zerozero.marryit.agent.mcp;

import java.io.Serializable;

public record McpOAuthRequest(
        String redirectUri,
        String state,
        String codeChallenge,
        String codeChallengeMethod
) implements Serializable {
}
