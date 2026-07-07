package com.zerozero.marryit.schedule.service;

import com.zerozero.marryit.schedule.domain.Schedule;
import com.zerozero.marryit.schedule.domain.ScheduleTargetType;
import com.zerozero.marryit.schedule.domain.ScheduleType;
import java.time.LocalDateTime;

public record ScheduleResponse(
        Long id,
        Long workspaceId,
        ScheduleTargetType targetType,
        Long targetId,
        ScheduleType scheduleType,
        String title,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String location
) {

    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getWorkspace().getId(),
                schedule.getTargetType(),
                schedule.getTargetId(),
                schedule.getScheduleType(),
                schedule.getTitle(),
                schedule.getStartsAt(),
                schedule.getEndsAt(),
                schedule.getLocation()
        );
    }
}
