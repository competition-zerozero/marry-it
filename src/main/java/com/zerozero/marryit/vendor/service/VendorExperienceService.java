package com.zerozero.marryit.vendor.service;

import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.auth.repository.UserRepository;
import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.domain.VendorExperience;
import com.zerozero.marryit.vendor.repository.VendorExperienceRepository;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorExperienceService {

    private final VendorExperienceRepository vendorExperienceRepository;
    private final VendorRepository vendorRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public VendorExperienceService(
            VendorExperienceRepository vendorExperienceRepository,
            VendorRepository vendorRepository,
            WorkspaceRepository workspaceRepository,
            UserRepository userRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.vendorExperienceRepository = vendorExperienceRepository;
        this.vendorRepository = vendorRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    @Transactional
    public VendorExperienceResponse create(Long workspaceId, Long vendorId, Long userId, VendorExperienceRequest request) {
        workspaceAccessService.validateMember(userId, workspaceId);
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found."));
        Vendor vendor = vendorRepository.findByIdAndWorkspaceId(vendorId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found."));
        User planner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        VendorExperience experience = VendorExperience.create(workspace, vendor, planner, request.content());
        return VendorExperienceResponse.from(vendorExperienceRepository.save(experience));
    }

    @Transactional(readOnly = true)
    public List<VendorExperienceResponse> findByVendor(Long workspaceId, Long vendorId, Long userId) {
        workspaceAccessService.validateMember(userId, workspaceId);
        vendorRepository.findByIdAndWorkspaceId(vendorId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found."));
        return vendorExperienceRepository.findByWorkspaceIdAndVendorIdOrderByIdDesc(workspaceId, vendorId)
                .stream()
                .map(VendorExperienceResponse::from)
                .toList();
    }
}
