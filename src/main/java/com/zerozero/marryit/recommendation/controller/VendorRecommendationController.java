package com.zerozero.marryit.recommendation.controller;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import com.zerozero.marryit.recommendation.service.VendorRecommendationRequest;
import com.zerozero.marryit.recommendation.service.VendorRecommendationResponse;
import com.zerozero.marryit.recommendation.service.VendorRecommendationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/recommendations/vendors")
public class VendorRecommendationController {

    private final VendorRecommendationService vendorRecommendationService;

    public VendorRecommendationController(VendorRecommendationService vendorRecommendationService) {
        this.vendorRecommendationService = vendorRecommendationService;
    }

    @PostMapping
    public VendorRecommendationResponse recommend(
            @PathVariable Long workspaceId,
            @Valid @RequestBody VendorRecommendationRequest request,
            HttpSession session
    ) {
        return vendorRecommendationService.recommend(workspaceId, currentUserId(session), request);
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Login is required.");
        }
        return id;
    }
}
