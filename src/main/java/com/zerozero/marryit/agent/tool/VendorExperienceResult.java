package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.vendor.domain.VendorExperience;

public record VendorExperienceResult(String plannerName, String content) {

    static VendorExperienceResult from(VendorExperience experience) {
        return new VendorExperienceResult(experience.getPlanner().getName(), experience.getContent());
    }
}
