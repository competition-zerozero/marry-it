package com.zerozero.marryit.recommendation.service;

import com.zerozero.marryit.external.kakao.KakaoPlaceResponse;
import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.domain.VendorCategory;

public record VendorCandidateResponse(
        VendorCandidateSource source,
        Long vendorId,
        String kakaoPlaceId,
        String name,
        VendorCategory category,
        String address,
        String reason
) {

    public static VendorCandidateResponse workspaceVendor(Vendor vendor, String reason) {
        return new VendorCandidateResponse(
                VendorCandidateSource.WORKSPACE,
                vendor.getId(),
                vendor.getKakaoPlaceId(),
                vendor.getName(),
                vendor.getCategory(),
                vendor.getRoadAddress() == null ? vendor.getAddress() : vendor.getRoadAddress(),
                reason
        );
    }

    public static VendorCandidateResponse kakaoExternal(KakaoPlaceResponse place, VendorCategory category) {
        return new VendorCandidateResponse(
                VendorCandidateSource.KAKAO_EXTERNAL,
                null,
                place.kakaoPlaceId(),
                place.name(),
                category,
                place.roadAddress() == null ? place.address() : place.roadAddress(),
                "Workspace에 등록되지 않은 카카오맵 외부 후보입니다. 계약 조건과 실제 가능 일정은 확인이 필요합니다."
        );
    }
}
