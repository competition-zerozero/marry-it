package com.zerozero.marryit.vendor.domain;

import com.zerozero.marryit.global.entity.BaseTimeEntity;
import com.zerozero.marryit.workspace.domain.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vendor_workspace_kakao_place",
                        columnNames = {"workspace_id", "kakao_place_id"}
                )
        }
)
public class Vendor extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "kakao_place_id", nullable = false, length = 100)
    private String kakaoPlaceId;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private VendorCategory category;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String roadAddress;

    @Column(length = 30)
    private String phone;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 500)
    private String placeUrl;

    @Column(nullable = false)
    private boolean partnered;

    @Column(length = 100)
    private String contactPerson;

    protected Vendor() {
    }

    private Vendor(
            Workspace workspace,
            String kakaoPlaceId,
            String name,
            VendorCategory category,
            String address,
            String roadAddress,
            String phone,
            BigDecimal latitude,
            BigDecimal longitude,
            String placeUrl,
            boolean partnered,
            String contactPerson
    ) {
        this.workspace = workspace;
        this.kakaoPlaceId = kakaoPlaceId;
        this.name = name;
        this.category = category;
        this.address = address;
        this.roadAddress = roadAddress;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeUrl = placeUrl;
        this.partnered = partnered;
        this.contactPerson = contactPerson;
    }

    public static Vendor create(
            Workspace workspace,
            String kakaoPlaceId,
            String name,
            VendorCategory category,
            String address,
            String roadAddress,
            String phone,
            BigDecimal latitude,
            BigDecimal longitude,
            String placeUrl,
            boolean partnered,
            String contactPerson
    ) {
        return new Vendor(
                workspace,
                kakaoPlaceId,
                name,
                category,
                address,
                roadAddress,
                phone,
                latitude,
                longitude,
                placeUrl,
                partnered,
                contactPerson
        );
    }

    public void update(
            String name,
            VendorCategory category,
            String address,
            String roadAddress,
            String phone,
            BigDecimal latitude,
            BigDecimal longitude,
            String placeUrl,
            boolean partnered,
            String contactPerson
    ) {
        this.name = name;
        this.category = category;
        this.address = address;
        this.roadAddress = roadAddress;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.placeUrl = placeUrl;
        this.partnered = partnered;
        this.contactPerson = contactPerson;
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public String getKakaoPlaceId() {
        return kakaoPlaceId;
    }

    public String getName() {
        return name;
    }

    public VendorCategory getCategory() {
        return category;
    }

    public String getAddress() {
        return address;
    }

    public String getRoadAddress() {
        return roadAddress;
    }

    public String getPhone() {
        return phone;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public String getPlaceUrl() {
        return placeUrl;
    }

    public boolean isPartnered() {
        return partnered;
    }

    public String getContactPerson() {
        return contactPerson;
    }
}
