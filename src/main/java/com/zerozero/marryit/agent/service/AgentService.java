package com.zerozero.marryit.agent.service;

import com.zerozero.marryit.recommendation.service.VendorRecommendationRequest;
import com.zerozero.marryit.recommendation.service.VendorRecommendationResponse;
import com.zerozero.marryit.recommendation.service.VendorRecommendationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentService {

    private final VendorRecommendationService vendorRecommendationService;

    public AgentService(VendorRecommendationService vendorRecommendationService) {
        this.vendorRecommendationService = vendorRecommendationService;
    }

    @Transactional(readOnly = true)
    public AgentResponse respond(Long workspaceId, Long userId, AgentRequest request) {
        if (request.vendorCategory() == null) {
            return new AgentResponse(
                    "현재 Agent는 업체 카테고리가 지정된 업체 추천/대체 업체 탐색만 지원합니다. 확인되지 않은 가격, 일정, 계약 조건은 생성하지 않습니다.",
                    null
            );
        }

        VendorRecommendationResponse recommendation = vendorRecommendationService.recommend(
                workspaceId,
                userId,
                new VendorRecommendationRequest(
                        request.vendorCategory(),
                        request.areaKeyword(),
                        request.includeExternalSearch()
                )
        );

        String answer = buildAnswer(recommendation);
        return new AgentResponse(answer, recommendation);
    }

    private String buildAnswer(VendorRecommendationResponse recommendation) {
        if (!recommendation.workspaceCandidates().isEmpty()) {
            return "Workspace에 등록된 기존 업체 후보를 우선 반환합니다. 실제 예약 가능 여부와 계약 조건은 저장된 일정/계약 데이터로 추가 확인해야 합니다.";
        }
        if (!recommendation.externalCandidates().isEmpty()) {
            return "기존 업체 후보가 없어 카카오맵 외부 후보를 반환합니다. 외부 후보는 검증되지 않았으며 등록 전 실제 조건 확인이 필요합니다.";
        }
        return "현재 데이터와 사용 가능한 외부 검색 결과에서 후보를 찾지 못했습니다.";
    }
}
