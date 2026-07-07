package com.zerozero.marryit.schedule.repository;

import com.zerozero.marryit.schedule.domain.Schedule;
import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    boolean existsByWorkspaceIdAndTargetTypeAndTargetIdAndStartsAtLessThanAndEndsAtGreaterThan(
            Long workspaceId,
            ScheduleTargetType targetType,
            Long targetId,
            LocalDateTime endsAt,
            LocalDateTime startsAt
    );

    boolean existsByWorkspaceIdAndTargetTypeAndTargetIdAndStartsAtLessThanAndEndsAtGreaterThanAndIdNot(
            Long workspaceId,
            ScheduleTargetType targetType,
            Long targetId,
            LocalDateTime endsAt,
            LocalDateTime startsAt,
            Long id
    );

    List<Schedule> findByWorkspaceIdOrderByStartsAtAsc(Long workspaceId);

    java.util.Optional<Schedule> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
