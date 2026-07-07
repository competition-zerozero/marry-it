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
                .map(this::toWorkspaceCandidate)
                .toList();

        List<VendorCandidateResponse> externalCandidates = List.of();
        if (workspaceCandidates.isEmpty() && request.includeExternalSearch()) {
            externalCandidates = searchExternalCandidates(request);
        }

        return new VendorRecommendationResponse(workspaceCandidates, externalCandidates);
    }

    private VendorCandidateResponse toWorkspaceCandidate(Vendor vendor) {
        String reason = "Workspace에 등록된 기존 거래처입니다. 상세 추천 판단에는 업체 경험, 일정, 계약 조건 확인이 필요합니다.";
        return VendorCandidateResponse.workspaceVendor(vendor, reason);
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
