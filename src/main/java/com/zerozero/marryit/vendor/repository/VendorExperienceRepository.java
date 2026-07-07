package com.zerozero.marryit.vendor.repository;

import com.zerozero.marryit.vendor.domain.VendorExperience;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorExperienceRepository extends JpaRepository<VendorExperience, Long> {

    List<VendorExperience> findByWorkspaceIdAndVendorIdOrderByIdDesc(Long workspaceId, Long vendorId);
}
