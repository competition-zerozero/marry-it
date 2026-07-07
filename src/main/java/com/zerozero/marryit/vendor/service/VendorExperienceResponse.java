package com.zerozero.marryit.vendor.service;

import com.zerozero.marryit.vendor.domain.VendorExperience;
import java.time.LocalDateTime;

public record VendorExperienceResponse(
        Long id,
        Long workspaceId,
        Long vendorId,
        Long plannerUserId,
        String plannerName,
        String content,
        LocalDateTime createdAt
) {

    public static VendorExperienceResponse from(VendorExperience experience) {
        return new VendorExperienceResponse(
                experience.getId(),
                experience.getWorkspace().getId(),
                experience.getVendor().getId(),
                experience.getPlanner().getId(),
                experience.getPlanner().getName(),
                experience.getContent(),
                experience.getCreatedAt()
        );
    }
}
