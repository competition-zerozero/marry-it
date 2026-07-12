package com.zerozero.marryit.vendor.service;

import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VendorService {

    private final VendorRepository vendorRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public VendorService(
            VendorRepository vendorRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.vendorRepository = vendorRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    @Transactional
    public VendorResponse create(Long workspaceId, Long userId, VendorRequest request) {
        workspaceAccessService.validateMember(userId, workspaceId);
        validateDuplicateKakaoPlace(workspaceId, request.kakaoPlaceId());

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found."));

        Vendor vendor = Vendor.create(
                workspace,
                request.kakaoPlaceId(),
                request.name(),
                request.category(),
                request.address(),
                request.roadAddress(),
                request.phone(),
                request.latitude(),
                request.longitude(),
                request.placeUrl(),
                request.partnered(),
                request.contactPerson(),
                request.memo()
        );

        return VendorResponse.from(vendorRepository.save(vendor));
    }

    @Transactional(readOnly = true)
    public List<VendorResponse> findAll(Long workspaceId, Long userId) {
        workspaceAccessService.validateMember(userId, workspaceId);
        return vendorRepository.findByWorkspaceIdOrderByIdDesc(workspaceId)
                .stream()
                .map(VendorResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VendorResponse get(Long workspaceId, Long userId, Long vendorId) {
        workspaceAccessService.validateMember(userId, workspaceId);
        return VendorResponse.from(getVendor(vendorId, workspaceId));
    }

    @Transactional
    public VendorResponse update(Long workspaceId, Long userId, Long vendorId, VendorRequest request) {
        workspaceAccessService.validateMember(userId, workspaceId);
        Vendor vendor = getVendor(vendorId, workspaceId);
        if (!vendor.getKakaoPlaceId().equals(request.kakaoPlaceId())) {
            validateDuplicateKakaoPlace(workspaceId, request.kakaoPlaceId());
        }
        vendor.update(
                request.name(),
                request.category(),
                request.address(),
                request.roadAddress(),
                request.phone(),
                request.latitude(),
                request.longitude(),
                request.placeUrl(),
                request.partnered(),
                request.contactPerson(),
                request.memo()
        );
        return VendorResponse.from(vendor);
    }

    @Transactional
    public void delete(Long workspaceId, Long userId, Long vendorId) {
        workspaceAccessService.validateMember(userId, workspaceId);
        vendorRepository.delete(getVendor(vendorId, workspaceId));
    }

    private void validateDuplicateKakaoPlace(Long workspaceId, String kakaoPlaceId) {
        if (vendorRepository.existsByWorkspaceIdAndKakaoPlaceId(workspaceId, kakaoPlaceId)) {
            throw new IllegalArgumentException("Vendor already exists in this workspace.");
        }
    }

    private Vendor getVendor(Long vendorId, Long workspaceId) {
        return vendorRepository.findByIdAndWorkspaceId(vendorId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Vendor not found."));
    }
}
