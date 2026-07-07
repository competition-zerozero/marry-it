package com.zerozero.marryit.customer.domain;

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
import jakarta.persistence.ManyToOne;
import java.time.LocalDate;

@Entity
public class Customer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planner_user_id", nullable = false)
    private User planner;

    @Column(nullable = false, length = 50)
    private String groomName;

    @Column(nullable = false, length = 50)
    private String brideName;

    @Column(length = 30)
    private String phoneNumber;

    @Column(length = 100)
    private String residenceArea;

    private LocalDate weddingDate;

    @Column(length = 100)
    private String preferredWeddingArea;

    private Integer expectedGuestCount;

    private Long totalBudget;

    @Column(length = 500)
    private String preferredAtmosphere;

    @Column(length = 500)
    private String preferredStyle;

    @Column(length = 1000)
    private String importantConditions;

    @Column(length = 1000)
    private String avoidConditions;

    @Column(length = 1000)
    private String itemBudgetMemo;

    @Column(length = 2000)
    private String consultationMemo;

    @Column(length = 1000)
    private String todoMemo;

    @Column(length = 1000)
    private String completedMemo;

    protected Customer() {
    }

    private Customer(
            Workspace workspace,
            User planner,
            String groomName,
            String brideName,
            String phoneNumber,
            String residenceArea,
            LocalDate weddingDate,
            String preferredWeddingArea,
            Integer expectedGuestCount,
            Long totalBudget,
            String preferredAtmosphere,
            String preferredStyle,
            String importantConditions,
            String avoidConditions,
            String itemBudgetMemo,
            String consultationMemo,
            String todoMemo,
            String completedMemo
    ) {
        this.workspace = workspace;
        this.planner = planner;
        this.groomName = groomName;
        this.brideName = brideName;
        this.phoneNumber = phoneNumber;
        this.residenceArea = residenceArea;
        this.weddingDate = weddingDate;
        this.preferredWeddingArea = preferredWeddingArea;
        this.expectedGuestCount = expectedGuestCount;
        this.totalBudget = totalBudget;
        this.preferredAtmosphere = preferredAtmosphere;
        this.preferredStyle = preferredStyle;
        this.importantConditions = importantConditions;
        this.avoidConditions = avoidConditions;
        this.itemBudgetMemo = itemBudgetMemo;
        this.consultationMemo = consultationMemo;
        this.todoMemo = todoMemo;
        this.completedMemo = completedMemo;
    }

    public static Customer create(
            Workspace workspace,
            User planner,
            String groomName,
            String brideName,
            String phoneNumber,
            String residenceArea,
            LocalDate weddingDate,
            String preferredWeddingArea,
            Integer expectedGuestCount,
            Long totalBudget,
            String preferredAtmosphere,
            String preferredStyle,
            String importantConditions,
            String avoidConditions,
            String itemBudgetMemo,
            String consultationMemo,
            String todoMemo,
            String completedMemo
    ) {
        return new Customer(
                workspace,
                planner,
                groomName,
                brideName,
                phoneNumber,
                residenceArea,
                weddingDate,
                preferredWeddingArea,
                expectedGuestCount,
                totalBudget,
                preferredAtmosphere,
                preferredStyle,
                importantConditions,
                avoidConditions,
                itemBudgetMemo,
                consultationMemo,
                todoMemo,
                completedMemo
        );
    }

    public void update(
            String groomName,
            String brideName,
            String phoneNumber,
            String residenceArea,
            LocalDate weddingDate,
            String preferredWeddingArea,
            Integer expectedGuestCount,
            Long totalBudget,
            String preferredAtmosphere,
            String preferredStyle,
            String importantConditions,
            String avoidConditions,
            String itemBudgetMemo,
            String consultationMemo,
            String todoMemo,
            String completedMemo
    ) {
        this.groomName = groomName;
        this.brideName = brideName;
        this.phoneNumber = phoneNumber;
        this.residenceArea = residenceArea;
        this.weddingDate = weddingDate;
        this.preferredWeddingArea = preferredWeddingArea;
        this.expectedGuestCount = expectedGuestCount;
        this.totalBudget = totalBudget;
        this.preferredAtmosphere = preferredAtmosphere;
        this.preferredStyle = preferredStyle;
        this.importantConditions = importantConditions;
        this.avoidConditions = avoidConditions;
        this.itemBudgetMemo = itemBudgetMemo;
        this.consultationMemo = consultationMemo;
        this.todoMemo = todoMemo;
        this.completedMemo = completedMemo;
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public User getPlanner() {
        return planner;
    }

    public String getGroomName() {
        return groomName;
    }

    public String getBrideName() {
        return brideName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getResidenceArea() {
        return residenceArea;
    }

    public LocalDate getWeddingDate() {
        return weddingDate;
    }

    public String getPreferredWeddingArea() {
        return preferredWeddingArea;
    }

    public Integer getExpectedGuestCount() {
        return expectedGuestCount;
    }

    public Long getTotalBudget() {
        return totalBudget;
    }

    public String getPreferredAtmosphere() {
        return preferredAtmosphere;
    }

    public String getPreferredStyle() {
        return preferredStyle;
    }

    public String getImportantConditions() {
        return importantConditions;
    }

    public String getAvoidConditions() {
        return avoidConditions;
    }

    public String getItemBudgetMemo() {
        return itemBudgetMemo;
    }

    public String getConsultationMemo() {
        return consultationMemo;
    }

    public String getTodoMemo() {
        return todoMemo;
    }

    public String getCompletedMemo() {
        return completedMemo;
    }
}
