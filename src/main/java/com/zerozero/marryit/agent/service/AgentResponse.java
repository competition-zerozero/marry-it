package com.zerozero.marryit.agent.service;

import com.zerozero.marryit.recommendation.service.VendorRecommendationResponse;

public record AgentResponse(
        String answer,
        VendorRecommendationResponse vendorRecommendation
) {
}
