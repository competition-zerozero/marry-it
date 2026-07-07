package com.zerozero.marryit.recommendation.service;

import com.zerozero.marryit.vendor.domain.VendorCategory;
import jakarta.validation.constraints.NotNull;

public record VendorRecommendationRequest(
        @NotNull
        VendorCategory category,
        String areaKeyword,
        boolean includeExternalSearch
) {
}
