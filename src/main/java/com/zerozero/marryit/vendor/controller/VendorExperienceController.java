package com.zerozero.marryit.vendor.controller;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import com.zerozero.marryit.vendor.service.VendorExperienceRequest;
import com.zerozero.marryit.vendor.service.VendorExperienceResponse;
import com.zerozero.marryit.vendor.service.VendorExperienceService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/vendors/{vendorId}/experiences")
public class VendorExperienceController {

    private final VendorExperienceService vendorExperienceService;

    public VendorExperienceController(VendorExperienceService vendorExperienceService) {
        this.vendorExperienceService = vendorExperienceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendorExperienceResponse create(
            @PathVariable Long workspaceId,
            @PathVariable Long vendorId,
            @Valid @RequestBody VendorExperienceRequest request,
            HttpSession session
    ) {
        return vendorExperienceService.create(workspaceId, vendorId, currentUserId(session), request);
    }

    @GetMapping
    public List<VendorExperienceResponse> findByVendor(
            @PathVariable Long workspaceId,
            @PathVariable Long vendorId,
            HttpSession session
    ) {
        return vendorExperienceService.findByVendor(workspaceId, vendorId, currentUserId(session));
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Login is required.");
        }
        return id;
    }
}
