package com.zerozero.marryit.recommendation.service;

import com.zerozero.marryit.external.kakao.KakaoPlaceClient;
import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorRecommendationService {

    private final VendorRepository vendorRepository;
    private final KakaoPlaceClient kakaoPlaceClient;
    private final WorkspaceAccessService workspaceAccessService;

    public VendorRecommendationService(
            VendorRepository vendorRepository,
            KakaoPlaceClient kakaoPlaceClient,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.vendorRepository = vendorRepository;
        this.kakaoPlaceClient = kakaoPlaceClient;
        this.workspaceAccessService = workspaceAccessService;
    }

    @Transactional(readOnly = true)
    public VendorRecommendationResponse recommend(Long workspaceId, Long userId, VendorRecommendationRequest request) {
        workspaceAccessService.validateMember(userId, workspaceId);

        List<VendorCandidateResponse> workspaceCandidates = vendorRepository
                .findByWorkspaceIdAndCategoryOrderByIdDesc(workspaceId, request.category())
                .stream()
                .filter(vendor -> matchesArea(vendor, request.areaKeyword()))
                .map(this::toWorkspaceCandidate)
                .toList();

        List<VendorCandidateResponse> externalCandidates = List.of();
        if (request.includeExternalSearch()) {
            externalCandidates = searchExternalCandidates(request);
        }

        return new VendorRecommendationResponse(workspaceCandidates, externalCandidates);
    }

    private boolean matchesArea(Vendor vendor, String areaKeyword) {
        if (areaKeyword == null || areaKeyword.isBlank()) {
            return true;
        }
        String keyword = areaKeyword.trim().toLowerCase();
        String address = vendor.getRoadAddress() != null ? vendor.getRoadAddress() : vendor.getAddress();
        return address != null && address.toLowerCase().contains(keyword);
    }

    private VendorCandidateResponse toWorkspaceCandidate(Vendor vendor) {
        String reason = buildReason(vendor);
        return VendorCandidateResponse.workspaceVendor(vendor, reason);
    }

    private String buildReason(Vendor vendor) {
        StringBuilder sb = new StringBuilder();
        if (vendor.getMemo() != null && !vendor.getMemo().isBlank()) {
            sb.append(vendor.getMemo());
        }
        if (vendor.getContactPerson() != null && !vendor.getContactPerson().isBlank()) {
            if (!sb.isEmpty()) sb.append(" · ");
            sb.append("담당: ").append(vendor.getContactPerson());
        }
        if (vendor.isPartnered()) {
            if (!sb.isEmpty()) sb.append(" · ");
            sb.append("제휴 업체");
        }
        if (sb.isEmpty()) {
            sb.append("등록된 거래처입니다.");
        }
        return sb.toString();
    }

    private List<VendorCandidateResponse> searchExternalCandidates(VendorRecommendationRequest request) {
        String query = request.areaKeyword() == null || request.areaKeyword().isBlank()
                ? request.category().name()
                : request.areaKeyword() + " " + request.category().name();

        return kakaoPlaceClient.searchPlaces(query).stream()
                .map(place -> VendorCandidateResponse.kakaoExternal(place, request.category()))
                .toList();
    }
}
