package com.zerozero.marryit.agent.service;

import jakarta.validation.constraints.NotBlank;

public record AgentRequest(
        @NotBlank
        String message
) {
}
