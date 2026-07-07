package com.zerozero.marryit.vendor.service;

import jakarta.validation.constraints.NotBlank;

public record VendorExperienceRequest(
        @NotBlank
        String content
) {
}
