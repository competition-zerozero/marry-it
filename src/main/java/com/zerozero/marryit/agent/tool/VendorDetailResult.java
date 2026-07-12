package com.zerozero.marryit.agent.tool;

import com.zerozero.marryit.vendor.domain.Vendor;

public record VendorDetailResult(
        Long vendorId,
        String name,
        String category,
        String address,
        String roadAddress,
        String phone,
        boolean partnered,
        String contactPerson,
        String placeUrl
) {

    static VendorDetailResult from(Vendor vendor) {
        return new VendorDetailResult(
                vendor.getId(),
                vendor.getName(),
                vendor.getCategory().name(),
                vendor.getAddress(),
                vendor.getRoadAddress(),
                vendor.getPhone(),
                vendor.isPartnered(),
                vendor.getContactPerson(),
                vendor.getPlaceUrl()
        );
    }
}
