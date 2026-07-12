package com.zerozero.marryit.agent.service;

import com.zerozero.marryit.agent.tool.AgentToolContext;
import com.zerozero.marryit.recommendation.service.VendorRecommendationResponse;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentService {

    private static final String NOT_CONFIGURED_ANSWER =
            "AI 연동이 아직 설정되지 않았습니다. 관리자에게 OPENAI_API_KEY 설정을 요청하세요. "
                    + "설정 전까지는 확인되지 않은 추천을 만들어내지 않습니다.";

    private final AgentOrchestrator agentOrchestrator;
    private final WorkspaceAccessService workspaceAccessService;

    public AgentService(AgentOrchestrator agentOrchestrator, WorkspaceAccessService workspaceAccessService) {
        this.agentOrchestrator = agentOrchestrator;
        this.workspaceAccessService = workspaceAccessService;
    }

    @Transactional(readOnly = true)
    public AgentResponse respond(Long workspaceId, Long userId, AgentRequest request) {
        workspaceAccessService.validateMember(userId, workspaceId);

        if (!agentOrchestrator.isAvailable()) {
            return new AgentResponse(NOT_CONFIGURED_ANSWER, List.of(), null);
        }

        AgentResult result = agentOrchestrator.run(request.message(), new AgentToolContext(workspaceId, userId));

        VendorRecommendationResponse vendorRecommendation =
                result.workspaceCandidates().isEmpty() && result.externalCandidates().isEmpty()
                        ? null
                        : new VendorRecommendationResponse(result.workspaceCandidates(), result.externalCandidates());

        return new AgentResponse(result.answer(), result.toolCalls(), vendorRecommendation);
    }
}
