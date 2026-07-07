package com.zerozero.marryit.recommendation.service;

import java.util.List;

public record VendorRecommendationResponse(
        List<VendorCandidateResponse> workspaceCandidates,
        List<VendorCandidateResponse> externalCandidates
) {
}
