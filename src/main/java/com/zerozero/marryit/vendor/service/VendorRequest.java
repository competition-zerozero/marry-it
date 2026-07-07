package com.zerozero.marryit.vendor.service;

import com.zerozero.marryit.vendor.domain.VendorCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VendorRequest(
        @NotBlank
        String kakaoPlaceId,
        @NotBlank
        String name,
        @NotNull
        VendorCategory category,
        String address,
        String roadAddress,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        String placeUrl,
        boolean partnered,
        String contactPerson
) {
}
