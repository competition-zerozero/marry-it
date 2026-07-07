package com.zerozero.marryit.vendor.domain;

import com.zerozero.marryit.auth.domain.User;
import com.zerozero.marryit.global.entity.BaseTimeEntity;
import com.zerozero.marryit.workspace.domain.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

@Entity
public class VendorExperience extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planner_user_id", nullable = false)
    private User planner;

    @Lob
    @Column(nullable = false)
    private String content;

    protected VendorExperience() {
    }

    private VendorExperience(Workspace workspace, Vendor vendor, User planner, String content) {
        this.workspace = workspace;
        this.vendor = vendor;
        this.planner = planner;
        this.content = content;
    }

    public static VendorExperience create(Workspace workspace, Vendor vendor, User planner, String content) {
        return new VendorExperience(workspace, vendor, planner, content);
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public User getPlanner() {
        return planner;
    }

    public String getContent() {
        return content;
    }
}
