package com.zerozero.marryit.schedule.domain;

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
import java.time.LocalDateTime;

@Entity
public class Schedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScheduleTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ScheduleType scheduleType;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    @Column(length = 255)
    private String location;

    protected Schedule() {
    }

    private Schedule(
            Workspace workspace,
            ScheduleTargetType targetType,
            Long targetId,
            ScheduleType scheduleType,
            String title,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            String location
    ) {
        this.workspace = workspace;
        this.targetType = targetType;
        this.targetId = targetId;
        this.scheduleType = scheduleType;
        this.title = title;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.location = location;
    }

    public static Schedule create(
            Workspace workspace,
            ScheduleTargetType targetType,
            Long targetId,
            ScheduleType scheduleType,
            String title,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            String location
    ) {
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Schedule start time must be before end time.");
        }
        return new Schedule(workspace, targetType, targetId, scheduleType, title, startsAt, endsAt, location);
    }

    public void update(
            ScheduleTargetType targetType,
            Long targetId,
            ScheduleType scheduleType,
            String title,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            String location
    ) {
        if (!startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Schedule start time must be before end time.");
        }
        this.targetType = targetType;
        this.targetId = targetId;
        this.scheduleType = scheduleType;
        this.title = title;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.location = location;
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public ScheduleTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public ScheduleType getScheduleType() {
        return scheduleType;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public LocalDateTime getEndsAt() {
        return endsAt;
    }

    public String getLocation() {
        return location;
    }
}
