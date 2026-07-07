package com.zerozero.marryit.vendor.controller;

import com.zerozero.marryit.auth.oauth.OAuth2LoginSuccessHandler;
import com.zerozero.marryit.vendor.service.VendorRequest;
import com.zerozero.marryit.vendor.service.VendorResponse;
import com.zerozero.marryit.vendor.service.VendorService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendorResponse create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody VendorRequest request,
            HttpSession session
    ) {
        return vendorService.create(workspaceId, currentUserId(session), request);
    }

    @GetMapping
    public List<VendorResponse> findAll(@PathVariable Long workspaceId, HttpSession session) {
        return vendorService.findAll(workspaceId, currentUserId(session));
    }

    @GetMapping("/{vendorId}")
    public VendorResponse get(
            @PathVariable Long workspaceId,
            @PathVariable Long vendorId,
            HttpSession session
    ) {
        return vendorService.get(workspaceId, currentUserId(session), vendorId);
    }

    @PutMapping("/{vendorId}")
    public VendorResponse update(
            @PathVariable Long workspaceId,
            @PathVariable Long vendorId,
            @Valid @RequestBody VendorRequest request,
            HttpSession session
    ) {
        return vendorService.update(workspaceId, currentUserId(session), vendorId, request);
    }

    @DeleteMapping("/{vendorId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long workspaceId,
            @PathVariable Long vendorId,
            HttpSession session
    ) {
        vendorService.delete(workspaceId, currentUserId(session), vendorId);
    }

    private Long currentUserId(HttpSession session) {
        Object userId = session.getAttribute(OAuth2LoginSuccessHandler.SESSION_USER_ID);
        if (!(userId instanceof Long id)) {
            throw new SecurityException("Login is required.");
        }
        return id;
    }
}
