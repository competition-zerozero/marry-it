package com.zerozero.marryit.vendor.repository;

import com.zerozero.marryit.vendor.domain.Vendor;
import com.zerozero.marryit.vendor.domain.VendorCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRepository extends JpaRepository<Vendor, Long> {

    boolean existsByWorkspaceIdAndKakaoPlaceId(Long workspaceId, String kakaoPlaceId);

    List<Vendor> findByWorkspaceIdOrderByIdDesc(Long workspaceId);

    List<Vendor> findByWorkspaceIdAndCategoryOrderByIdDesc(Long workspaceId, VendorCategory category);

    Optional<Vendor> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
