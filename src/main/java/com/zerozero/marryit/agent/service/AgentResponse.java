package com.zerozero.marryit.agent.service;

import com.zerozero.marryit.recommendation.service.VendorRecommendationResponse;
import java.util.List;

public record AgentResponse(
        String answer,
        List<AgentToolCallLog> toolCalls,
        VendorRecommendationResponse vendorRecommendation
) {
}
