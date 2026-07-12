package com.zerozero.marryit.vendor.service;

import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import java.math.BigDecimal;

public record VendorResponse(
        Long id,
        Long workspaceId,
        String kakaoPlaceId,
        String name,
        VendorCategory category,
        String address,
        String roadAddress,
        String phone,
        BigDecimal latitude,
        BigDecimal longitude,
        String placeUrl,
        boolean partnered,
        String contactPerson,
        String memo
) {

    public static VendorResponse from(Vendor vendor) {
        return new VendorResponse(
                vendor.getId(),
                vendor.getWorkspace().getId(),
                vendor.getKakaoPlaceId(),
                vendor.getName(),
                vendor.getCategory(),
                vendor.getAddress(),
                vendor.getRoadAddress(),
                vendor.getPhone(),
                vendor.getLatitude(),
                vendor.getLongitude(),
                vendor.getPlaceUrl(),
                vendor.isPartnered(),
                vendor.getContactPerson(),
                vendor.getMemo()
        );
    }
}
