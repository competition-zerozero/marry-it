package com.zerozero.marryit.schedule.service;

import com.zerozero.marryit.customer.repository.CustomerRepository;
import com.zerozero.marryit.schedule.domain.Schedule;
import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.repository.ScheduleRepository;
import com.zerozero.marryit.vendor.repository.VendorRepository;
import com.zerozero.marryit.workspace.domain.Workspace;
import com.zerozero.marryit.workspace.repository.WorkspaceMemberRepository;
import com.zerozero.marryit.workspace.repository.WorkspaceRepository;
import com.zerozero.marryit.workspace.service.WorkspaceAccessService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final CustomerRepository customerRepository;
    private final VendorRepository vendorRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public ScheduleService(
            ScheduleRepository scheduleRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceAccessService workspaceAccessService,
            CustomerRepository customerRepository,
            VendorRepository vendorRepository,
            WorkspaceMemberRepository workspaceMemberRepository
    ) {
        this.scheduleRepository = scheduleRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceAccessService = workspaceAccessService;
        this.customerRepository = customerRepository;
        this.vendorRepository = vendorRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public ScheduleResponse create(Long workspaceId, Long userId, ScheduleRequest request) {
        workspaceAccessService.validateMember(userId, workspaceId);
        validateTarget(workspaceId, request);
        validateNoConflict(workspaceId, request);

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found."));

        Schedule schedule = Schedule.create(
                workspace,
                request.targetType(),
                request.targetId(),
                request.scheduleType(),
                request.title(),
                request.startsAt(),
                request.endsAt(),
                request.location()
        );

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> findAll(Long workspaceId, Long userId) {
        workspaceAccessService.validateMember(userId, workspaceId);
        return scheduleRepository.findByWorkspaceIdOrderByStartsAtAsc(workspaceId)
                .stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleResponse get(Long workspaceId, Long userId, Long scheduleId) {
        workspaceAccessService.validateMember(userId, workspaceId);
        return ScheduleResponse.from(getSchedule(scheduleId, workspaceId));
    }

    @Transactional
    public ScheduleResponse update(Long workspaceId, Long userId, Long scheduleId, ScheduleRequest request) {
        workspaceAccessService.validateMember(userId, workspaceId);
        validateTarget(workspaceId, request);
        validateNoConflict(workspaceId, scheduleId, request);
        Schedule schedule = getSchedule(scheduleId, workspaceId);
        schedule.update(
                request.targetType(),
                request.targetId(),
                request.scheduleType(),
                request.title(),
                request.startsAt(),
                request.endsAt(),
                request.location()
        );
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(Long workspaceId, Long userId, Long scheduleId) {
        workspaceAccessService.validateMember(userId, workspaceId);
        scheduleRepository.delete(getSchedule(scheduleId, workspaceId));
    }

    private void validateNoConflict(Long workspaceId, ScheduleRequest request) {
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new IllegalArgumentException("Schedule start time must be before end time.");
        }

        boolean conflicts = scheduleRepository.existsByWorkspaceIdAndTargetTypeAndTargetIdAndStartsAtLessThanAndEndsAtGreaterThan(
                workspaceId,
                request.targetType(),
                request.targetId(),
                request.endsAt(),
                request.startsAt()
        );
        if (conflicts) {
            throw new IllegalArgumentException("Schedule conflicts with an existing schedule.");
        }
    }

    private void validateTarget(Long workspaceId, ScheduleRequest request) {
        if (request.targetType() == ScheduleTargetType.CUSTOMER
                && customerRepository.findByIdAndWorkspaceId(request.targetId(), workspaceId).isEmpty()) {
            throw new IllegalArgumentException("Schedule customer target not found.");
        }
        if (request.targetType() == ScheduleTargetType.VENDOR
                && vendorRepository.findByIdAndWorkspaceId(request.targetId(), workspaceId).isEmpty()) {
            throw new IllegalArgumentException("Schedule vendor target not found.");
        }
        if (request.targetType() == ScheduleTargetType.PLANNER
                && !workspaceMemberRepository.existsByUserIdAndWorkspaceId(request.targetId(), workspaceId)) {
            throw new IllegalArgumentException("Schedule planner target not found.");
        }
    }

    private void validateNoConflict(Long workspaceId, Long scheduleId, ScheduleRequest request) {
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new IllegalArgumentException("Schedule start time must be before end time.");
        }

        boolean conflicts = scheduleRepository.existsByWorkspaceIdAndTargetTypeAndTargetIdAndStartsAtLessThanAndEndsAtGreaterThanAndIdNot(
                workspaceId,
                request.targetType(),
                request.targetId(),
                request.endsAt(),
                request.startsAt(),
                scheduleId
        );
        if (conflicts) {
            throw new IllegalArgumentException("Schedule conflicts with an existing schedule.");
        }
    }

    private Schedule getSchedule(Long scheduleId, Long workspaceId) {
        return scheduleRepository.findByIdAndWorkspaceId(scheduleId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found."));
    }
}
