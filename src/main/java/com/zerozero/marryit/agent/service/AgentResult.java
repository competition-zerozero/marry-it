package com.zerozero.marryit.agent.service;

import com.zerozero.marryit.recommendation.service.VendorCandidateResponse;
import java.util.List;

public record AgentResult(
        String answer,
        List<AgentToolCallLog> toolCalls,
        List<VendorCandidateResponse> workspaceCandidates,
        List<VendorCandidateResponse> externalCandidates
) {
}
