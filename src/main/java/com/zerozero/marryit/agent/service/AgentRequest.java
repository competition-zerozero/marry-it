package com.zerozero.marryit.agent.service;

import com.zerozero.marryit.vendor.domain.VendorCategory;
import jakarta.validation.constraints.NotBlank;

public record AgentRequest(
        @NotBlank
        String message,
        VendorCategory vendorCategory,
        String areaKeyword,
        boolean includeExternalSearch
) {
}
